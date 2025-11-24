package com.example.snapswipe.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

class PhotoRepository(
    private val contentResolver: ContentResolver
) {

    suspend fun loadPhotos(sortOrder: SortOrder): List<PhotoItem> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<PhotoItem>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN
        )
        val orderExpression = when (sortOrder) {
            SortOrder.NEWEST_FIRST -> "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media._ID} DESC"
            SortOrder.OLDEST_FIRST -> "${MediaStore.Images.Media.DATE_TAKEN} ASC, ${MediaStore.Images.Media._ID} ASC"
        }

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            orderExpression
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateTaken = cursor.getLongOrNull(dateTakenColumn)?.takeIf { it != 0L }
                val uri = ContentUris.withAppendedId(collection, id)

                photos += PhotoItem(
                    id = id,
                    uri = uri,
                    dateTaken = dateTaken
                )
            }
        }

        Log.d(TAG, "Loaded ${photos.size} photos with order $sortOrder")
        photos
    }

    private fun Cursor.getLongOrNull(index: Int): Long? {
        return if (isNull(index)) null else getLong(index)
    }

    private companion object {
        private const val TAG = "PhotoRepository"
    }
}
