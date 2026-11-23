package com.snapswipe.app.data

data class SavedReviewAction(val photoUri: String, val queued: Boolean)

data class ReviewProgress(
    val sortOrder: SortOrder,
    val startUri: String?,
    val startIndex: Int?,
    val currentUri: String?,
    val currentIndex: Int,
    val actions: List<SavedReviewAction>
)

interface ReviewProgressStore {
    suspend fun readReviewProgress(): ReviewProgress?
    suspend fun saveReviewProgress(progress: ReviewProgress)
}
