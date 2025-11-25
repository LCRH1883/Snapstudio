package com.snapswipe.app.ui

import android.content.ContentResolver
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snapswipe.app.data.PhotoItem
import com.snapswipe.app.data.PhotoDataSource
import com.snapswipe.app.data.PhotoRepository
import com.snapswipe.app.data.SortOrder
import com.snapswipe.app.data.DeleteResult
import com.snapswipe.app.data.DeleteMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.content.IntentSender

data class SnapSwipeUiState(
    val photos: List<PhotoItem> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val errorMessage: String? = null,
    val pendingDeleteIntent: IntentSender? = null,
    val lastAction: LastAction? = null,
    val totalCount: Int = 0,
    val keptCount: Int = 0,
    val deleteMode: DeleteMode = DeleteMode.IMMEDIATE,
    val queuedDeletes: List<PhotoItem> = emptyList()
) {
    val currentPhoto: PhotoItem? get() = photos.getOrNull(currentIndex)
    val isAtEnd: Boolean get() = photos.isNotEmpty() && currentIndex >= photos.size
    val currentPosition: Int?
        get() = if (photos.isNotEmpty()) keptCount + 1 else null
}

sealed class LastAction {
    data class Kept(val photo: PhotoItem, val index: Int) : LastAction()
    data class Deleted(val photo: PhotoItem, val index: Int) : LastAction()
}

class SnapSwipeViewModel(
    private val repository: PhotoDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnapSwipeUiState(isLoading = true))
    val uiState: StateFlow<SnapSwipeUiState> = _uiState

    private var pendingApprovalPhotoId: Long? = null

    init {
        loadPhotos(SortOrder.NEWEST_FIRST)
    }

    fun ensurePhotos(sortOrder: SortOrder = _uiState.value.sortOrder) {
        val state = _uiState.value
        if (!state.isLoading && state.photos.isNotEmpty() && state.sortOrder == sortOrder) {
            return
        }
        loadPhotos(sortOrder)
    }

    fun loadPhotos(sortOrder: SortOrder = _uiState.value.sortOrder) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    sortOrder = sortOrder,
                    currentIndex = 0,
                    lastAction = null,
                    pendingDeleteIntent = null,
                    queuedDeletes = emptyList()
                )
            }
            try {
                val photos = repository.loadPhotos(sortOrder)
                _uiState.update {
                    it.copy(
                        photos = photos,
                        currentIndex = 0,
                        isLoading = false,
                        errorMessage = null,
                        lastAction = null,
                        pendingDeleteIntent = null,
                        totalCount = photos.size,
                        keptCount = 0,
                        queuedDeletes = emptyList()
                    )
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing permission to load photos", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Permission required. Please grant photo access."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load photos", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load photos"
                    )
                }
            }
        }
    }

    fun nextPhoto() {
        advanceIndex()
    }

    fun keepCurrent() {
        val photo = _uiState.value.currentPhoto ?: return
        val indexBefore = _uiState.value.currentIndex
        removeCurrentAndAdvance(LastAction.Kept(photo, indexBefore), isDelete = false)
    }

    fun trashCurrent() {
        val photo = _uiState.value.currentPhoto ?: return
        val index = _uiState.value.currentIndex
        val deleteMode = _uiState.value.deleteMode
        if (deleteMode == DeleteMode.QUEUED) {
            removeCurrentAndAdvance(LastAction.Deleted(photo, index), isDelete = true, queueDelete = true)
        } else {
            removeCurrentAndAdvance(LastAction.Deleted(photo, index), isDelete = true, queueDelete = false)
            commitImmediateDelete(photo)
        }
    }

    fun onDeleteCompleted(success: Boolean) {
        if (success) {
            pendingApprovalPhotoId = null
        }
        if (!success) {
            Log.w(TAG, "Delete approval denied for photoId=$pendingApprovalPhotoId")
        }
        _uiState.update { it.copy(pendingDeleteIntent = null) }
        if (!success) {
            loadPhotos(_uiState.value.sortOrder)
        }
    }

    fun restart() {
        _uiState.update { it.copy(currentIndex = 0, lastAction = null, keptCount = 0, totalCount = it.photos.size) }
    }

    fun reload() {
        loadPhotos(_uiState.value.sortOrder)
    }

    fun setDeleteMode(mode: DeleteMode) {
        _uiState.update { state ->
            if (state.deleteMode == mode) state else state.copy(
                deleteMode = mode,
                queuedDeletes = emptyList(),
                lastAction = null
            )
        }
    }

    fun undoLast() {
        when (val action = _uiState.value.lastAction) {
            is LastAction.Kept -> {
                _uiState.update { state ->
                    val list = state.photos.toMutableList()
                    val insertIndex = action.index.coerceAtMost(list.size)
                    list.add(insertIndex, action.photo)
                    state.copy(
                        photos = list,
                        currentIndex = insertIndex,
                        lastAction = null,
                        keptCount = (state.keptCount - 1).coerceAtLeast(0)
                    )
                }
            }

            is LastAction.Deleted -> {
                _uiState.update { state ->
                    val list = state.photos.toMutableList()
                    val insertIndex = action.index.coerceAtMost(list.size)
                    list.add(insertIndex, action.photo)
                    state.copy(
                        photos = list,
                        currentIndex = insertIndex,
                        lastAction = null,
                        totalCount = state.totalCount + 1,
                        queuedDeletes = state.queuedDeletes.filterNot { it.id == action.photo.id },
                        keptCount = (state.keptCount - 1).coerceAtLeast(0)
                    )
                }
            }

            null -> Unit
        }
    }

    private fun advanceIndex() {
        _uiState.update { state ->
            if (state.photos.isEmpty()) {
                state
            } else {
                val nextIndex = (state.currentIndex + 1).coerceAtMost(state.photos.size)
                state.copy(currentIndex = nextIndex)
            }
        }
    }

    private fun removeCurrentAndAdvance(action: LastAction, isDelete: Boolean, queueDelete: Boolean = false) {
        _uiState.update { state ->
            if (state.photos.isEmpty()) return@update state.copy(pendingDeleteIntent = null)
            val currentIdx = state.currentIndex
            val updated = state.photos.toMutableList().also { list ->
                if (currentIdx in list.indices) {
                    list.removeAt(currentIdx)
                }
            }
            val nextIndex = if (updated.isEmpty()) 0 else currentIdx.coerceAtMost(updated.lastIndex)
            val newQueued = if (queueDelete && action is LastAction.Deleted) {
                state.queuedDeletes + action.photo
            } else {
                state.queuedDeletes
            }
            state.copy(
                photos = updated,
                currentIndex = nextIndex,
                pendingDeleteIntent = null,
                errorMessage = null,
                lastAction = action,
                keptCount = state.keptCount + 1,
                totalCount = if (isDelete) (state.totalCount - 1).coerceAtLeast(0) else state.totalCount,
                queuedDeletes = newQueued
            )
        }
    }

    private fun commitImmediateDelete(photo: PhotoItem) {
        viewModelScope.launch {
            when (val result = repository.deletePhoto(photo)) {
                is DeleteResult.Success -> Unit
                is DeleteResult.RequiresUserApproval -> {
                    pendingApprovalPhotoId = photo.id
                    Log.d(TAG, "Launching delete approval for photoId=${photo.id}")
                    _uiState.update { it.copy(pendingDeleteIntent = result.intentSender) }
                }
                is DeleteResult.Error -> {
                    Log.e(TAG, "Immediate delete failed for id=${photo.id}", result.throwable)
                    _uiState.update { it.copy(errorMessage = "Unable to delete photo") }
                    reload()
                }
            }
        }
    }

    fun commitQueuedDeletes() {
        val queued = _uiState.value.queuedDeletes
        if (queued.isEmpty()) return
        viewModelScope.launch {
            when (val result = repository.deletePhotosBatch(queued)) {
                is DeleteResult.Success -> {
                    _uiState.update { it.copy(queuedDeletes = emptyList(), lastAction = null) }
                }
                is DeleteResult.RequiresUserApproval -> {
                    pendingApprovalPhotoId = null
                    _uiState.update { it.copy(pendingDeleteIntent = result.intentSender) }
                    // queuedDeletes remain until approval result
                }
                is DeleteResult.Error -> {
                    Log.e(TAG, "Queued delete failed for ${queued.size} photos", result.throwable)
                    _uiState.update { it.copy(errorMessage = "Unable to delete queued photos") }
                }
            }
        }
    }

    private companion object {
        private const val TAG = "SnapSwipeViewModel"
    }
}

class SnapSwipeViewModelFactory(
    private val contentResolver: ContentResolver
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SnapSwipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SnapSwipeViewModel(PhotoRepository(contentResolver)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
