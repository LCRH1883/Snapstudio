package com.snapswipe.app.ui

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import com.snapswipe.app.data.DeleteResult
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
        assertTrue("Last action should record keep", state.lastAction is LastAction.Kept)
        assertEquals("Current index should point at next photo", 0, state.currentIndex)
    }

    @Test
    fun trashCurrentRemovesPhotoAndUpdatesTotals() = runTest {
        val viewModel = SnapSwipeViewModel(FakePhotoDataSource(samplePhotos()))

        advanceUntilIdle()

        viewModel.trashCurrent()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Processed list should shrink after delete", 2, state.photos.size)
        assertEquals("Total count should decrease after delete", 2, state.totalCount)
        assertTrue("Last action should record delete", state.lastAction is LastAction.Deleted)
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
