package com.snapswipe.app.data

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.content.IntentSender
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

sealed class DeleteResult {
    object Success : DeleteResult()
    data class RequiresUserApproval(val intentSender: IntentSender) : DeleteResult()
    data class Error(val throwable: Throwable) : DeleteResult()
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

    suspend fun deletePhoto(photo: PhotoItem): DeleteResult = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(photo.uri))
                return@withContext DeleteResult.RequiresUserApproval(pendingIntent.intentSender)
            }

            val rowsDeleted = contentResolver.delete(photo.uri, null, null)
            if (rowsDeleted > 0) {
                DeleteResult.Success
            } else {
                DeleteResult.Error(IllegalStateException("Delete returned 0 rows"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete photo id=${photo.id}", e)
            DeleteResult.Error(e)
        }
    }

    suspend fun deletePhotosBatch(photos: List<PhotoItem>): DeleteResult = withContext(Dispatchers.IO) {
        if (photos.isEmpty()) return@withContext DeleteResult.Success
        val uris = photos.map { it.uri }
        return@withContext try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                DeleteResult.RequiresUserApproval(pendingIntent.intentSender)
            } else {
                var deleted = 0
                uris.forEach { uri ->
                    val rows = contentResolver.delete(uri, null, null)
                    if (rows > 0) deleted++
                }
                if (deleted == uris.size) {
                    DeleteResult.Success
                } else {
                    DeleteResult.Error(IllegalStateException("Deleted $deleted of ${uris.size}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete batch", e)
            DeleteResult.Error(e)
        }
    }

    private fun Cursor.getLongOrNull(index: Int): Long? {
        return if (isNull(index)) null else getLong(index)
    }

    private companion object {
        private const val TAG = "PhotoRepository"
    }
}
