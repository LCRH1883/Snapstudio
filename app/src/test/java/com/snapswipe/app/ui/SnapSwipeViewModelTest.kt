package com.snapswipe.app.ui

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.snapswipe.app.data.DeleteResult
import com.snapswipe.app.data.DeleteMode
import com.snapswipe.app.data.PhotoDataSource
import com.snapswipe.app.data.PhotoItem
import com.snapswipe.app.data.ReviewProgress
import com.snapswipe.app.data.ReviewProgressStore
import com.snapswipe.app.data.SortOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SnapSwipeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun keepCurrentAdvancesAndTracksCounts() = runTest {
        val viewModel = SnapSwipeViewModel(FakePhotoDataSource(samplePhotos()))

        advanceUntilIdle() // allow initial load

        viewModel.keepCurrent()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Processed list should shrink after keep", 2, state.photos.size)
        assertEquals("Kept count should increment", 1, state.keptCount)
        assertEquals("Total count should remain original total", 3, state.totalCount)
        assertEquals("Display total should remain original total until deletes confirm", 3, state.displayTotal)
        assertTrue("Last action should record keep", state.lastAction is LastAction.Kept)
        assertEquals("Current index should point at next photo", 0, state.currentIndex)
        assertEquals("Current position should advance to next photo", 2, state.currentPosition)
    }

    @Test
    fun trashCurrentRemovesPhotoAndUpdatesTotals() = runTest {
        val viewModel = SnapSwipeViewModel(FakePhotoDataSource(samplePhotos()))

        advanceUntilIdle()

        viewModel.trashCurrent()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Processed list should shrink after delete", 2, state.photos.size)
        assertEquals("Display total should decrease after delete", 2, state.displayTotal)
        assertEquals("Base total should remain original until confirmed deletes accumulate", 3, state.totalCount)
        assertTrue("Last action should record delete", state.lastAction is LastAction.Trashed)
        assertEquals("Current position should stay at the same slot after delete", 1, state.currentPosition)
    }

    @Test
    fun trashCurrentRequestsApprovalWhenRequired() = runTest {
        val pending = PendingIntent.getActivity(
            RuntimeEnvironment.getApplication(),
            0,
            Intent(Intent.ACTION_VIEW),
            PendingIntent.FLAG_IMMUTABLE
        ).intentSender
        val viewModel = SnapSwipeViewModel(
            FakePhotoDataSource(
                photos = samplePhotos(),
                deleteResult = DeleteResult.RequiresUserApproval(pending)
            )
        )

        advanceUntilIdle()

        viewModel.trashCurrent()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Pending delete intent should be set when approval is required", state.pendingDeleteIntent)
        assertEquals(samplePhotos().first(), state.currentPhoto)
        viewModel.onDeleteCompleted(false)
        assertEquals(samplePhotos(), viewModel.uiState.value.photos)
        assertNull(viewModel.uiState.value.lastAction)
    }

    @Test
    fun queuedDeletesClearAfterApprovalSuccess() = runTest {
        val pending = PendingIntent.getActivity(
            RuntimeEnvironment.getApplication(),
            0,
            Intent(Intent.ACTION_VIEW),
            PendingIntent.FLAG_IMMUTABLE
        ).intentSender
        val viewModel = SnapSwipeViewModel(
            FakePhotoDataSource(
                photos = samplePhotos(),
                deleteResult = DeleteResult.RequiresUserApproval(pending)
            )
        )

        advanceUntilIdle()

        viewModel.setDeleteMode(DeleteMode.QUEUED)
        viewModel.trashCurrent()
        advanceUntilIdle()

        assertEquals("Queue should track items to delete", 1, viewModel.uiState.value.queuedDeletes.size)

        viewModel.commitQueuedDeletes()
        advanceUntilIdle()

        assertNotNull("Queued delete should request approval", viewModel.uiState.value.pendingDeleteIntent)

        viewModel.onDeleteCompleted(success = true)

        val state = viewModel.uiState.value
        assertEquals("Queued delete count should clear after approval success", 0, state.queuedDeletes.size)
        assertEquals("Pending intent should be cleared", null, state.pendingDeleteIntent)
        assertEquals("Display total should drop after approved queued delete", 2, state.displayTotal)
        assertEquals("Current position should shift down by approved deletes", 1, state.currentPosition)
    }

    @Test
    fun queuedDeleteModeUpdatesPositionAndTotals() = runTest {
        val viewModel = SnapSwipeViewModel(FakePhotoDataSource(samplePhotos()))

        advanceUntilIdle()
        viewModel.setDeleteMode(DeleteMode.QUEUED)
        viewModel.trashCurrent()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals("Display total should stay unchanged until queued deletes are confirmed", 3, state.displayTotal)
        assertEquals("Current position should advance as photos are processed", 2, state.currentPosition)
        assertEquals("Queued count should track deleted item", 1, state.queuedDeletes.size)

        viewModel.trashCurrent()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Display total should stay unchanged after additional queued delete", 3, state.displayTotal)
        assertEquals("Current position should keep advancing", 3, state.currentPosition)
        assertEquals("Queued count should include both queued deletions", 2, state.queuedDeletes.size)

        viewModel.commitQueuedDeletes()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Queued deletes should clear after commit success", 0, state.queuedDeletes.size)
        assertEquals("Display total should drop by number of confirmed deletes", 1, state.displayTotal)
        assertEquals("Current position should decrease by number of confirmed deletes", 1, state.currentPosition)
    }

    @Test
    fun oldestFirstNumbersDescend() = runTest {
        val viewModel = SnapSwipeViewModel(FakePhotoDataSource(samplePhotos()))

        advanceUntilIdle()

        viewModel.loadPhotos(SortOrder.OLDEST_FIRST)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals("Oldest-first should start at highest number", 3, state.currentPosition)
        assertEquals("Total should reflect all items", 3, state.displayTotal)

        viewModel.keepCurrent()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Position should descend after keeping oldest", 2, state.currentPosition)
        assertEquals("Total should remain until deletes are confirmed", 3, state.displayTotal)

        viewModel.trashCurrent()
        advanceUntilIdle()

        // Immediate delete should confirm and reduce total
        state = viewModel.uiState.value
        assertEquals("Total should drop after delete", 2, state.displayTotal)
        assertEquals("Position should stay aligned with remaining list", 1, state.currentPosition)
    }

    @Test
    fun trashedPhotoReturnsOnlyAfterRestoreApproval() = runTest {
        val sender = approvalSender()
        val viewModel = SnapSwipeViewModel(FakePhotoDataSource(
            samplePhotos(),
            deleteResult = DeleteResult.RequiresUserApproval(sender),
            restoreResult = DeleteResult.RequiresUserApproval(sender)
        ))
        advanceUntilIdle()

        viewModel.trashCurrent()
        advanceUntilIdle()
        assertEquals(sender, viewModel.consumeApprovalRequest())
        assertNull(viewModel.consumeApprovalRequest())
        viewModel.onDeleteCompleted(true)
        assertEquals(samplePhotos().drop(1), viewModel.uiState.value.photos)

        viewModel.undoLast()
        advanceUntilIdle()
        assertEquals(samplePhotos().drop(1), viewModel.uiState.value.photos)
        viewModel.onDeleteCompleted(false)
        assertEquals(samplePhotos().drop(1), viewModel.uiState.value.photos)

        viewModel.undoLast()
        advanceUntilIdle()
        viewModel.onDeleteCompleted(true)
        advanceUntilIdle()
        assertEquals(samplePhotos(), viewModel.uiState.value.photos)
        assertEquals(1, viewModel.uiState.value.currentPosition)
        assertEquals(0, viewModel.uiState.value.confirmedDeleteCount)
    }

    @Test
    fun failedRestoreStaysInTrashAndCanBeRetried() = runTest {
        val viewModel = SnapSwipeViewModel(FakePhotoDataSource(
            samplePhotos(), restoreResult = DeleteResult.Error(IllegalStateException())
        ))
        advanceUntilIdle()
        viewModel.trashCurrent()
        advanceUntilIdle()
        viewModel.undoLast()
        advanceUntilIdle()

        assertEquals(samplePhotos().drop(1), viewModel.uiState.value.photos)
        assertEquals(1, viewModel.uiState.value.confirmedDeleteCount)
        assertTrue(viewModel.uiState.value.lastAction is LastAction.Trashed)
        assertNotNull(viewModel.uiState.value.operationError)
    }

    @Test
    fun failedImmediateRemovalDoesNotLoseCurrentPhoto() = runTest {
        val viewModel = SnapSwipeViewModel(FakePhotoDataSource(
            samplePhotos(), deleteResult = DeleteResult.Error(IllegalStateException())
        ))
        advanceUntilIdle()
        viewModel.trashCurrent()
        advanceUntilIdle()

        assertEquals(samplePhotos(), viewModel.uiState.value.photos)
        assertNotNull(viewModel.uiState.value.operationError)
        assertNull(viewModel.uiState.value.lastAction)
    }

    @Test
    fun canceledBatchRemainsActionableAndRestartQueriesGallery() = runTest {
        val repository = FakePhotoDataSource(
            samplePhotos(), batchResult = DeleteResult.RequiresUserApproval(approvalSender())
        )
        val viewModel = SnapSwipeViewModel(repository)
        advanceUntilIdle()
        viewModel.setDeleteMode(DeleteMode.QUEUED)
        repeat(3) { viewModel.trashCurrent() }

        viewModel.commitQueuedDeletes()
        advanceUntilIdle()
        viewModel.onDeleteCompleted(false)
        assertTrue(viewModel.uiState.value.photos.isEmpty())
        assertEquals(samplePhotos(), viewModel.uiState.value.queuedDeletes)

        viewModel.removeFromQueue(samplePhotos()[1])
        assertEquals(listOf(samplePhotos()[1]), viewModel.uiState.value.photos)
        viewModel.discardQueue()
        assertEquals(samplePhotos(), viewModel.uiState.value.photos)

        viewModel.keepCurrent()
        viewModel.restart()
        advanceUntilIdle()
        assertEquals(2, repository.loadCount)
        assertEquals(samplePhotos(), viewModel.uiState.value.photos)
    }

    @Test
    fun androidTenApprovalRetriesRemovalAndPermanentDeleteCannotBeUndone() = runTest {
        val repository = FakePhotoDataSource(
            samplePhotos(),
            deleteResult = DeleteResult.RequiresUserApproval(approvalSender(), retryAfterApproval = true),
            supportsTrash = false
        )
        val viewModel = SnapSwipeViewModel(repository)
        advanceUntilIdle()
        viewModel.trashCurrent()
        advanceUntilIdle()
        assertEquals(samplePhotos(), viewModel.uiState.value.photos)

        viewModel.onDeleteCompleted(true)
        advanceUntilIdle()
        assertEquals(listOf(false, true), repository.retryFlags)
        viewModel.undoLast()
        assertEquals(samplePhotos().drop(1), viewModel.uiState.value.photos)
        assertNull(viewModel.uiState.value.lastAction)
    }

    @Test
    fun partialBatchCancellationKeepsOnlyUnprocessedPhotosQueued() = runTest {
        val repository = FakePhotoDataSource(
            samplePhotos(),
            deleteResult = DeleteResult.RequiresUserApproval(approvalSender(), retryAfterApproval = true),
            supportsTrash = false
        )
        val viewModel = SnapSwipeViewModel(repository)
        advanceUntilIdle()
        viewModel.setDeleteMode(DeleteMode.QUEUED)
        repeat(3) { viewModel.trashCurrent() }
        viewModel.commitQueuedDeletes()
        advanceUntilIdle()

        viewModel.onDeleteCompleted(true)
        advanceUntilIdle()
        viewModel.onDeleteCompleted(false)

        assertEquals(samplePhotos().drop(1), viewModel.uiState.value.queuedDeletes)
        assertEquals(1, viewModel.uiState.value.confirmedDeleteCount)
    }

    @Test
    fun approvedRestoreDoesNotDisplayPhotoStillMissingFromMediaStore() = runTest {
        val repository = FakePhotoDataSource(
            samplePhotos(),
            restoreResult = DeleteResult.RequiresUserApproval(approvalSender()),
            visibleAfterRestore = emptyList()
        )
        val viewModel = SnapSwipeViewModel(repository)
        advanceUntilIdle()
        viewModel.trashCurrent()
        advanceUntilIdle()
        viewModel.undoLast()
        advanceUntilIdle()
        viewModel.onDeleteCompleted(true)
        advanceUntilIdle()

        assertEquals(samplePhotos().drop(1), viewModel.uiState.value.photos)
        assertNotNull(viewModel.uiState.value.operationError)
        assertTrue(viewModel.uiState.value.lastAction is LastAction.Trashed)
    }

    @Test
    fun confirmedRestartClearsCanceledBatchAndLoadsExistingPhotos() = runTest {
        val repository = FakePhotoDataSource(
            samplePhotos(), batchResult = DeleteResult.RequiresUserApproval(approvalSender())
        )
        val viewModel = SnapSwipeViewModel(repository)
        advanceUntilIdle()
        viewModel.setDeleteMode(DeleteMode.QUEUED)
        repeat(3) { viewModel.trashCurrent() }
        viewModel.commitQueuedDeletes()
        advanceUntilIdle()
        viewModel.onDeleteCompleted(false)

        viewModel.restart()
        advanceUntilIdle()

        assertEquals(2, repository.loadCount)
        assertTrue(viewModel.uiState.value.queuedDeletes.isEmpty())
        assertEquals(samplePhotos(), viewModel.uiState.value.photos)
    }

    @Test
    fun resumeRestoresCurrentPhotoKeepsAndPendingQueue() = runTest {
        val photos = libraryPhotos()
        val store = FakeProgressStore()
        val repository = FakePhotoDataSource(photos)
        val first = SnapSwipeViewModel(repository, store)
        advanceUntilIdle()
        first.keepCurrent()
        first.setDeleteMode(DeleteMode.QUEUED)
        first.trashCurrent()
        first.stepForward()
        advanceUntilIdle()
        val saved = first.uiState.value

        val resumed = SnapSwipeViewModel(repository, store)
        advanceUntilIdle()

        assertTrue(resumed.uiState.value.offerResume)
        resumed.resumeReview()
        assertEquals(false, resumed.uiState.value.offerResume)
        assertEquals(saved.currentPhoto, resumed.uiState.value.currentPhoto)
        assertEquals(saved.photos, resumed.uiState.value.photos)
        assertEquals(saved.queuedDeletes, resumed.uiState.value.queuedDeletes)
        assertEquals(1, resumed.uiState.value.keptCount)
    }

    @Test
    fun startingFromTimelineUsesSelectedRangeAfterRelaunch() = runTest {
        val photos = libraryPhotos()
        val store = FakeProgressStore()
        val first = SnapSwipeViewModel(FakePhotoDataSource(photos), store)
        advanceUntilIdle()
        first.startAt(photos[2])
        first.keepCurrent()
        advanceUntilIdle()

        val resumed = SnapSwipeViewModel(FakePhotoDataSource(photos), store)
        advanceUntilIdle()

        assertEquals(photos, resumed.uiState.value.timelinePhotos)
        assertEquals(photos.drop(3), resumed.uiState.value.photos)
        assertEquals(photos[3], resumed.uiState.value.currentPhoto)
    }

    @Test
    fun resumeUsesNearestPositionWhenSavedCurrentPhotoIsMissing() = runTest {
        val photos = libraryPhotos()
        val store = FakeProgressStore()
        val first = SnapSwipeViewModel(FakePhotoDataSource(photos), store)
        advanceUntilIdle()
        first.stepForward()
        advanceUntilIdle()

        val resumed = SnapSwipeViewModel(FakePhotoDataSource(photos - photos[1]), store)
        advanceUntilIdle()

        assertEquals(photos[2], resumed.uiState.value.currentPhoto)
    }

    @Test
    fun startFromBeginningDismissesResumeAndClearsReviewWithoutDeleting() = runTest {
        val photos = libraryPhotos()
        val store = FakeProgressStore()
        val repository = FakePhotoDataSource(photos)
        val first = SnapSwipeViewModel(repository, store)
        advanceUntilIdle()
        first.startAt(photos[2])
        first.setDeleteMode(DeleteMode.QUEUED)
        first.trashCurrent()
        advanceUntilIdle()

        val reopened = SnapSwipeViewModel(repository, store)
        advanceUntilIdle()
        assertTrue(reopened.uiState.value.offerResume)
        reopened.startFromBeginning()
        advanceUntilIdle()

        assertEquals(false, reopened.uiState.value.offerResume)
        assertEquals(photos, reopened.uiState.value.photos)
        assertEquals(photos.first(), reopened.uiState.value.currentPhoto)
        assertTrue(reopened.uiState.value.queuedDeletes.isEmpty())
        assertTrue(repository.retryFlags.isEmpty())
    }

    @Test
    fun returningToAnInProgressReviewOffersResumeOncePerReturn() = runTest {
        val photos = libraryPhotos()
        val viewModel = SnapSwipeViewModel(FakePhotoDataSource(photos))
        advanceUntilIdle()
        viewModel.offerResumeAfterReturn(1L)
        assertEquals(false, viewModel.uiState.value.offerResume)

        viewModel.stepForward()
        viewModel.offerResumeAfterReturn(2L)
        assertTrue(viewModel.uiState.value.offerResume)
        viewModel.resumeReview()
        assertEquals(photos[1], viewModel.uiState.value.currentPhoto)

        viewModel.offerResumeAfterReturn(2L)
        assertEquals(false, viewModel.uiState.value.offerResume)
    }

    @Test
    fun retryAfterInitialLoadFailurePreservesSavedCursor() = runTest {
        val photos = libraryPhotos()
        val store = FakeProgressStore()
        store.saveReviewProgress(
            ReviewProgress(SortOrder.NEWEST_FIRST, null, null, photos[2].uri.toString(), 2, emptyList())
        )
        val repository = FakePhotoDataSource(photos)
        repository.queryPhotos = { throw IllegalStateException("Temporary query failure") }
        val viewModel = SnapSwipeViewModel(repository, store)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.loadError)

        repository.queryPhotos = { photos }
        viewModel.reload()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadError)
        assertTrue(viewModel.uiState.value.offerResume)
        assertEquals(photos[2], viewModel.uiState.value.currentPhoto)
    }

    @Test
    fun backgroundRefreshPreservesDecisionsMadeWhileItLoads() = runTest {
        val photos = libraryPhotos()
        val repository = FakePhotoDataSource(photos)
        val viewModel = SnapSwipeViewModel(repository)
        advanceUntilIdle()
        val query = CompletableDeferred<List<PhotoItem>>()
        repository.queryPhotos = { query.await() }
        viewModel.refreshPhotoAccess()
        advanceUntilIdle()

        viewModel.keepCurrent()
        viewModel.setDeleteMode(DeleteMode.QUEUED)
        viewModel.trashCurrent()
        query.complete(photos.dropLast(1))
        advanceUntilIdle()

        assertEquals(listOf(photos[1]), viewModel.uiState.value.queuedDeletes)
        assertEquals(1, viewModel.uiState.value.keptCount)
        assertEquals(photos[2], viewModel.uiState.value.currentPhoto)
    }

    private fun libraryPhotos(): List<PhotoItem> = (1..5).map { index ->
        PhotoItem(index.toLong(), Uri.parse("content://media/external/images/media/$index"), index * 1000L)
    }

    private fun approvalSender() = PendingIntent.getActivity(
        RuntimeEnvironment.getApplication(), 0, Intent(Intent.ACTION_VIEW), PendingIntent.FLAG_IMMUTABLE
    ).intentSender

    private fun samplePhotos(): List<PhotoItem> {
        return (1..3).map { idx ->
            PhotoItem(
                id = idx.toLong(),
                uri = Uri.parse("content://media/external/images/media/$idx"),
                dateTaken = 1000L * idx
            )
        }
    }
}

private class FakePhotoDataSource(
    private val photos: List<PhotoItem>,
    private val deleteResult: DeleteResult = DeleteResult.Success,
    private val batchResult: DeleteResult = deleteResult,
    private val restoreResult: DeleteResult = DeleteResult.Success,
    override val supportsTrash: Boolean = true,
    private val visibleAfterRestore: List<PhotoItem> = photos
) : PhotoDataSource {
    var loadCount = 0
        private set
    val retryFlags = mutableListOf<Boolean>()
    var queryPhotos: suspend () -> List<PhotoItem> = { photos }

    override suspend fun loadPhotos(sortOrder: SortOrder): List<PhotoItem> {
        loadCount++
        return queryPhotos()
    }

    override suspend fun removePhotos(photos: List<PhotoItem>, afterApproval: Boolean): DeleteResult {
        retryFlags.add(afterApproval)
        return when {
            afterApproval -> DeleteResult.Success
            photos.size > 1 -> batchResult
            else -> deleteResult
        }
    }

    override suspend fun restorePhotos(photos: List<PhotoItem>): DeleteResult = restoreResult

    override suspend fun findVisiblePhotos(photos: List<PhotoItem>): List<PhotoItem> =
        photos.filter { it in visibleAfterRestore }
}

private class FakeProgressStore : ReviewProgressStore {
    private var progress: ReviewProgress? = null

    override suspend fun readReviewProgress(): ReviewProgress? = progress

    override suspend fun saveReviewProgress(progress: ReviewProgress) {
        this.progress = progress
    }
}
