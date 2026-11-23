package com.snapswipe.app.ui

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snapswipe.app.R
import com.snapswipe.app.data.DeleteMode
import com.snapswipe.app.data.DeleteResult
import com.snapswipe.app.data.PhotoDataSource
import com.snapswipe.app.data.PhotoItem
import com.snapswipe.app.data.PhotoRepository
import com.snapswipe.app.data.ReviewProgress
import com.snapswipe.app.data.ReviewProgressStore
import com.snapswipe.app.data.SavedReviewAction
import com.snapswipe.app.data.SortOrderPreferences
import com.snapswipe.app.data.SortOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SnapSwipeUiState(
    val photos: List<PhotoItem> = emptyList(),
    val timelinePhotos: List<PhotoItem> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    @StringRes val loadError: Int? = null,
    @StringRes val operationError: Int? = null,
    val pendingDeleteIntent: IntentSender? = null,
    val mediaBusy: Boolean = false,
    val offerResume: Boolean = false,
    val lastAction: LastAction? = null,
    val totalCount: Int = 0,
    val keptCount: Int = 0,
    val deleteMode: DeleteMode = DeleteMode.IMMEDIATE,
    val queuedDeletes: List<PhotoItem> = emptyList(),
    val confirmedDeleteCount: Int = 0
) {
    val currentPhoto: PhotoItem? get() = photos.getOrNull(currentIndex)
    val displayTotal: Int get() = totalCount - confirmedDeleteCount
    val currentPosition: Int?
        get() = if (currentPhoto == null) null else when (sortOrder) {
            SortOrder.NEWEST_FIRST -> displayTotal - photos.size + currentIndex + 1
            SortOrder.OLDEST_FIRST -> photos.size - currentIndex
        }
}

sealed class LastAction {
    data class Kept(val photo: PhotoItem) : LastAction()
    data class Queued(val photo: PhotoItem) : LastAction()
    data class Trashed(val photos: List<PhotoItem>) : LastAction()
}

class SnapSwipeViewModel(
    private val repository: PhotoDataSource,
    private val progressStore: ReviewProgressStore? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(SnapSwipeUiState(isLoading = true))
    val uiState: StateFlow<SnapSwipeUiState> = _uiState
    val supportsTrash: Boolean get() = repository.supportsTrash

    private val history = ArrayDeque<LastAction>()
    private var libraryOrder: Map<Uri, Int> = emptyMap()
    private var visibleLibrary: Set<Uri> = emptySet()
    private var reviewStartIndex: Int? = null
    private val progressWrites = Channel<ReviewProgress>(Channel.CONFLATED)
    private var lastReturnRevision = 0L
    private var restoringProgress = true
    private var libraryLoaded = false
    private var preferredSortOrder: SortOrder? = null
    private var loadJob: Job? = null
    private var pendingChange: MediaChange? = null
    private var retryAfterApproval = false
    private var deferredPermissionRefresh = false

    private data class MediaChange(
        val photos: List<PhotoItem>,
        val source: Source,
        val remaining: List<PhotoItem> = emptyList()
    )

    private enum class Source { IMMEDIATE, QUEUE, RESTORE }

    init {
        progressStore?.let { store ->
            viewModelScope.launch {
                for (progress in progressWrites) {
                    try {
                        store.saveReviewProgress(progress)
                    } catch (e: IOException) {
                        Log.w(TAG, "Could not save review progress", e)
                    }
                }
            }
        }
        queryLibrary(SortOrder.NEWEST_FIRST, reset = true, resume = true)
    }

    fun ensurePhotos(sortOrder: SortOrder) {
        preferredSortOrder = sortOrder
        if (libraryLoaded && !restoringProgress && _uiState.value.sortOrder != sortOrder) loadPhotos(sortOrder)
    }

    fun loadPhotos(sortOrder: SortOrder = _uiState.value.sortOrder) =
        queryLibrary(sortOrder, reset = true)

    fun refreshPhotoAccess() {
        if (_uiState.value.mediaBusy) {
            deferredPermissionRefresh = true
        } else if (loadJob?.isActive != true) {
            reload()
        }
    }

    private fun queryLibrary(sortOrder: SortOrder, reset: Boolean, resume: Boolean = false) {
        if (_uiState.value.mediaBusy) return
        loadJob?.cancel()
        if (reset) _uiState.update { it.copy(isLoading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            try {
                val storedProgress = if (resume) {
                    try {
                        progressStore?.readReviewProgress()
                    } catch (e: IOException) {
                        Log.w(TAG, "Could not load saved review progress", e)
                        null
                    }
                } else {
                    null
                }
                val order = if (resume) preferredSortOrder ?: storedProgress?.sortOrder ?: sortOrder else sortOrder
                val photos = repository.loadPhotos(order)
                if (!reset && _uiState.value.mediaBusy) {
                    deferredPermissionRefresh = true
                    return@launch
                }
                val visible = photos.mapTo(mutableSetOf()) { it.uri }
                if (!reset && visible == visibleLibrary && _uiState.value.loadError == null) return@launch
                // The user can keep reviewing while a background query is in flight.
                val saved = if (reset) storedProgress else captureProgress()
                val progress = saved?.takeIf { it.sortOrder == order }
                val lastIndex = photos.lastIndex.coerceAtLeast(0)
                reviewStartIndex = progress?.startIndex?.let { index ->
                    photos.indexOfFirst { it.uri.toString() == progress.startUri }
                        .takeIf { it >= 0 } ?: index.coerceIn(0, lastIndex)
                }
                val range = photos.drop(reviewStartIndex ?: 0)
                val photosByUri = range.associateBy { it.uri.toString() }
                history.clear()
                val savedActions = saved?.actions.orEmpty()
                val restored = if (progress != null) savedActions else savedActions.filter { it.queued }
                val restoredUris = mutableSetOf<String>()
                restored.forEach { action ->
                    val photo = photosByUri[action.photoUri]
                    if (photo != null && restoredUris.add(action.photoUri)) {
                        history.addLast(if (action.queued) LastAction.Queued(photo) else LastAction.Kept(photo))
                    }
                }
                // Sort and Home reloads retain pending choices, even when no snapshot was used.
                if (saved == null) {
                    _uiState.value.queuedDeletes.forEach { photo ->
                        photosByUri[photo.uri.toString()]?.let { available ->
                            if (restoredUris.add(available.uri.toString())) history.addLast(LastAction.Queued(available))
                        }
                    }
                }
                val queue = history.filterIsInstance<LastAction.Queued>().map { it.photo }
                val active = range.filterNot { it.uri.toString() in restoredUris }
                val currentIndex = if (progress == null) 0 else {
                    active.indexOfFirst { it.uri.toString() == progress.currentUri }.takeIf { it >= 0 }
                        ?: progress.currentIndex.coerceIn(0, active.lastIndex.coerceAtLeast(0))
                }
                val offerResume = resume && progress != null &&
                    (active.isNotEmpty() || queue.isNotEmpty()) &&
                    (restoredUris.isNotEmpty() || currentIndex > 0 || reviewStartIndex != null)
                libraryOrder = photos.mapIndexed { index, photo -> photo.uri to index }.toMap()
                visibleLibrary = visible
                libraryLoaded = true
                _uiState.update {
                    it.copy(
                        photos = active,
                        timelinePhotos = photos,
                        currentIndex = currentIndex,
                        isLoading = false,
                        sortOrder = order,
                        offerResume = offerResume || (!reset && it.offerResume),
                        loadError = null,
                        lastAction = history.lastOrNull(),
                        totalCount = range.size,
                        keptCount = history.count { action -> action is LastAction.Kept },
                        queuedDeletes = queue,
                        confirmedDeleteCount = 0
                    )
                }
                recordProgress()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unable to load photos", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = if (e is SecurityException) R.string.permission_denied_message
                                    else R.string.photo_load_failed
                    )
                }
            } finally {
                if (resume) {
                    restoringProgress = false
                    val preferred = preferredSortOrder
                    if (preferred != null && preferred != _uiState.value.sortOrder &&
                        !_uiState.value.isLoading && _uiState.value.loadError == null
                    ) {
                        loadPhotos(preferred)
                    }
                }
            }
        }
    }

    fun offerResumeAfterReturn(revision: Long) {
        if (revision <= lastReturnRevision) return
        lastReturnRevision = revision
        if (!canEdit()) return
        val state = _uiState.value
        if ((state.currentPhoto != null || state.queuedDeletes.isNotEmpty()) &&
            (state.lastAction != null || state.currentIndex > 0 || reviewStartIndex != null)
        ) {
            _uiState.update { it.copy(offerResume = true) }
        }
    }

    fun resumeReview() {
        _uiState.update { it.copy(offerResume = false) }
    }

    fun startFromBeginning() {
        resumeReview()
        restart()
    }

    fun startAt(photo: PhotoItem) {
        if (!canEdit()) return
        val state = _uiState.value
        val position = state.timelinePhotos.indexOfFirst { it.uri == photo.uri }
        if (position == -1) return
        val range = state.timelinePhotos.drop(position)
        reviewStartIndex = position
        history.clear()
        _uiState.update {
            it.copy(
                photos = range,
                currentIndex = 0,
                lastAction = null,
                totalCount = range.size,
                keptCount = 0,
                queuedDeletes = emptyList(),
                confirmedDeleteCount = 0
            )
        }
        recordProgress()
    }

    private fun captureProgress(): ReviewProgress {
        val state = _uiState.value
        val actions = history.mapNotNull { action ->
            when (action) {
                is LastAction.Kept -> SavedReviewAction(action.photo.uri.toString(), queued = false)
                is LastAction.Queued -> SavedReviewAction(action.photo.uri.toString(), queued = true)
                is LastAction.Trashed -> null
            }
        }
        return ReviewProgress(
            sortOrder = state.sortOrder,
            startUri = reviewStartIndex?.let { state.timelinePhotos.getOrNull(it)?.uri?.toString() },
            startIndex = reviewStartIndex,
            currentUri = state.currentPhoto?.uri?.toString(),
            currentIndex = state.currentIndex,
            actions = actions
        )
    }

    private fun recordProgress() {
        if (progressStore != null) progressWrites.trySend(captureProgress())
    }

    fun stepForward() = move(1)
    fun stepBack() = move(-1)

    private fun move(delta: Int) {
        if (!canEdit()) return
        val state = _uiState.value
        val next = (state.currentIndex + delta).coerceIn(0, state.photos.lastIndex.coerceAtLeast(0))
        if (next == state.currentIndex) return
        _uiState.update { it.copy(currentIndex = next) }
        recordProgress()
    }

    fun keepCurrent() {
        if (!canEdit()) return
        val photo = _uiState.value.currentPhoto ?: return
        val action = LastAction.Kept(photo)
        history.addLast(action)
        _uiState.update {
            it.withoutCurrent().copy(keptCount = it.keptCount + 1, lastAction = action)
        }
        recordProgress()
    }

    fun trashCurrent() {
        if (!canEdit()) return
        val state = _uiState.value
        val photo = state.currentPhoto ?: return
        if (state.deleteMode == DeleteMode.QUEUED) {
            val action = LastAction.Queued(photo)
            history.addLast(action)
            _uiState.update {
                it.withoutCurrent().copy(queuedDeletes = it.queuedDeletes + photo, lastAction = action)
            }
            recordProgress()
        } else {
            beginChange(listOf(photo), Source.IMMEDIATE)
        }
    }

    fun commitQueuedDeletes() {
        if (!canEdit()) return
        val queue = _uiState.value.queuedDeletes
        if (queue.isNotEmpty()) beginChange(queue, Source.QUEUE)
    }

    fun removeFromQueue(photo: PhotoItem) {
        if (!canEdit() || photo !in _uiState.value.queuedDeletes) return
        history.removeAll { it is LastAction.Queued && it.photo.uri == photo.uri }
        _uiState.update {
            it.reinsert(listOf(photo)).copy(
                queuedDeletes = it.queuedDeletes - photo,
                lastAction = history.lastOrNull()
            )
        }
        recordProgress()
    }

    fun discardQueue() {
        if (!canEdit()) return
        history.removeAll { it is LastAction.Queued }
        _uiState.update {
            it.reinsert(it.queuedDeletes).copy(queuedDeletes = emptyList(), lastAction = history.lastOrNull())
        }
        recordProgress()
    }

    fun restart() {
        if (!canEdit()) return
        _uiState.update { it.copy(queuedDeletes = emptyList()) }
        loadPhotos()
    }
    fun reload() = queryLibrary(_uiState.value.sortOrder, reset = !libraryLoaded, resume = !libraryLoaded)
    fun goHome() = loadPhotos()

    fun setDeleteMode(mode: DeleteMode) {
        _uiState.update { it.copy(deleteMode = mode) }
    }

    fun undoLast() {
        if (!canEdit()) return
        when (val action = history.lastOrNull()) {
            is LastAction.Kept -> {
                history.removeLast()
                _uiState.update {
                    it.reinsert(listOf(action.photo)).copy(keptCount = it.keptCount - 1, lastAction = history.lastOrNull())
                }
                recordProgress()
            }
            is LastAction.Queued -> removeFromQueue(action.photo)
            is LastAction.Trashed -> beginChange(action.photos, Source.RESTORE)
            null -> Unit
        }
    }

    fun consumeApprovalRequest(): IntentSender? {
        val intent = _uiState.value.pendingDeleteIntent
        _uiState.update { it.copy(pendingDeleteIntent = null) }
        return intent
    }

    fun onDeleteCompleted(success: Boolean) {
        val change = pendingChange ?: return
        if (!success) {
            finishChange()
        } else if (retryAfterApproval) {
            retryAfterApproval = false
            performChange(change, afterApproval = true)
        } else if (change.source == Source.RESTORE) {
            viewModelScope.launch {
                try {
                    val restored = repository.findVisiblePhotos(change.photos)
                    if (restored.isNotEmpty()) {
                        applyRestore(restored)
                        recordProgress()
                    }
                    finishChange(if (restored.size == change.photos.size) null else R.string.photo_restore_failed)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Could not verify restored photos", e)
                    finishChange(R.string.photo_restore_failed)
                }
            }
        } else {
            onChangeApplied(change)
        }
    }

    fun clearOperationError() = _uiState.update { it.copy(operationError = null) }

    private fun beginChange(photos: List<PhotoItem>, source: Source) {
        val limit = if (supportsTrash) MAX_TRASH_REQUEST else 1
        val change = MediaChange(photos.take(limit), source, photos.drop(limit))
        _uiState.update { it.copy(mediaBusy = true, operationError = null) }
        performChange(change)
    }

    private fun performChange(change: MediaChange, afterApproval: Boolean = false) {
        pendingChange = change
        viewModelScope.launch {
            val result = if (change.source == Source.RESTORE) {
                repository.restorePhotos(change.photos)
            } else {
                repository.removePhotos(change.photos, afterApproval)
            }
            when (result) {
                DeleteResult.Success -> onChangeApplied(change)
                is DeleteResult.RequiresUserApproval -> {
                    retryAfterApproval = result.retryAfterApproval
                    _uiState.update { it.copy(pendingDeleteIntent = result.intentSender) }
                }
                is DeleteResult.Error -> {
                    Log.e(TAG, "Photo operation failed", result.throwable)
                    val message = if (change.source == Source.RESTORE) R.string.photo_restore_failed else R.string.photo_delete_failed
                    finishChange(message)
                }
            }
        }
    }

    private fun onChangeApplied(change: MediaChange) {
        val uris = change.photos.mapTo(mutableSetOf()) { it.uri }
        if (change.source == Source.RESTORE) {
            applyRestore(change.photos)
        } else {
            history.removeAll { it is LastAction.Queued && it.photo.uri in uris }
            if (supportsTrash) history.addLast(LastAction.Trashed(change.photos))
            visibleLibrary = visibleLibrary - uris
            _uiState.update {
                val next = if (change.source == Source.IMMEDIATE) it.withoutCurrent() else it
                next.copy(
                    queuedDeletes = next.queuedDeletes.filterNot { item -> item.uri in uris },
                    timelinePhotos = next.timelinePhotos.filterNot { item -> item.uri in uris },
                    confirmedDeleteCount = next.confirmedDeleteCount + change.photos.size,
                    lastAction = history.lastOrNull()
                )
            }
        }
        recordProgress()
        // Each completed chunk is committed before starting the next. Canceling leaves
        // only unprocessed items queued. Android 10 may prompt separately for each item.
        if (change.remaining.isNotEmpty() && change.source == Source.QUEUE) {
            val limit = if (supportsTrash) MAX_TRASH_REQUEST else 1
            performChange(change.copy(photos = change.remaining.take(limit), remaining = change.remaining.drop(limit)))
        } else {
            finishChange()
        }
    }

    private fun applyRestore(restored: List<PhotoItem>) {
        val action = history.removeLast() as LastAction.Trashed
        val uris = restored.mapTo(mutableSetOf()) { it.uri }
        val remaining = action.photos.filterNot { it.uri in uris }
        if (remaining.isNotEmpty()) history.addLast(LastAction.Trashed(remaining))
        visibleLibrary = visibleLibrary + uris
        _uiState.update {
            it.reinsert(restored).copy(
                confirmedDeleteCount = it.confirmedDeleteCount - restored.size,
                timelinePhotos = orderedPhotos(it.timelinePhotos + restored),
                lastAction = history.lastOrNull()
            )
        }
    }

    private fun finishChange(@StringRes error: Int? = null) {
        pendingChange = null
        retryAfterApproval = false
        _uiState.update { it.copy(mediaBusy = false, pendingDeleteIntent = null, operationError = error) }
        if (deferredPermissionRefresh) {
            deferredPermissionRefresh = false
            refreshPhotoAccess()
        }
    }

    private fun canEdit(): Boolean = _uiState.value.let { !it.isLoading && !it.mediaBusy && !it.offerResume && it.loadError == null }

    private fun SnapSwipeUiState.withoutCurrent(): SnapSwipeUiState {
        val remaining = photos.toMutableList().apply { removeAt(currentIndex) }
        return copy(photos = remaining, currentIndex = currentIndex.coerceAtMost(remaining.lastIndex.coerceAtLeast(0)))
    }

    private fun SnapSwipeUiState.reinsert(items: List<PhotoItem>): SnapSwipeUiState {
        if (items.isEmpty()) return this
        val updated = orderedPhotos(photos + items)
        return copy(photos = updated, currentIndex = updated.indexOfFirst { it.uri == items.first().uri }.coerceAtLeast(0))
    }

    private fun orderedPhotos(items: List<PhotoItem>): List<PhotoItem> =
        items.distinctBy { it.uri }.sortedBy { libraryOrder[it.uri] ?: Int.MAX_VALUE }

    private companion object {
        const val TAG = "SnapSwipeViewModel"
        const val MAX_TRASH_REQUEST = 2000
    }
}

class SnapSwipeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SnapSwipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SnapSwipeViewModel(
                PhotoRepository(context.contentResolver),
                SortOrderPreferences(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
