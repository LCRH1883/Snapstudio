package com.snapswipe.app.ui

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.snapswipe.app.data.DeleteResult
import com.snapswipe.app.data.DeleteMode
import com.snapswipe.app.data.PhotoDataSource
import com.snapswipe.app.data.PhotoItem
import com.snapswipe.app.data.SortOrder
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
        assertTrue("Last action should record delete", state.lastAction is LastAction.Deleted)
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
                batchResult = DeleteResult.RequiresUserApproval(pending)
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
        assertEquals("Position should stay aligned with remaining list", 2, state.currentPosition)
    }

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
    private val batchResult: DeleteResult = DeleteResult.Success
) : PhotoDataSource {

    override suspend fun loadPhotos(sortOrder: SortOrder): List<PhotoItem> {
        return photos
    }

    override suspend fun deletePhoto(photo: PhotoItem): DeleteResult {
        return deleteResult
    }

    override suspend fun deletePhotosBatch(photos: List<PhotoItem>): DeleteResult {
        return batchResult
    }
}
