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

interface PhotoDataSource {
    suspend fun loadPhotos(sortOrder: SortOrder): List<PhotoItem>
    suspend fun deletePhoto(photo: PhotoItem): DeleteResult
    suspend fun deletePhotosBatch(photos: List<PhotoItem>): DeleteResult
}

class PhotoRepository(
    private val contentResolver: ContentResolver
) : PhotoDataSource {

    override suspend fun loadPhotos(sortOrder: SortOrder): List<PhotoItem> = withContext(Dispatchers.IO) {
        try {
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

            val cursor = contentResolver.query(
                collection,
                projection,
                null,
                null,
                orderExpression
            )

            if (cursor == null) {
                Log.w(TAG, "MediaStore query returned null cursor; possible missing permission")
                return@withContext emptyList()
            }

            cursor.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val dateTaken = it.getLongOrNull(dateTakenColumn)?.takeIf { value -> value != 0L }
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
        } catch (se: SecurityException) {
            Log.e(TAG, "Permission denied while loading photos", se)
            throw se
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load photos", e)
            throw e
        }
    }

    override suspend fun deletePhoto(photo: PhotoItem): DeleteResult = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(photo.uri))
                Log.d(TAG, "Delete requires user approval for id=${photo.id}")
                return@withContext DeleteResult.RequiresUserApproval(pendingIntent.intentSender)
            }

            val rowsDeleted = contentResolver.delete(photo.uri, null, null)
            if (rowsDeleted > 0) {
                DeleteResult.Success
            } else {
                Log.e(TAG, "Delete returned 0 rows for id=${photo.id}")
                DeleteResult.Error(IllegalStateException("Delete returned 0 rows"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete photo id=${photo.id}", e)
            DeleteResult.Error(e)
        }
    }

    override suspend fun deletePhotosBatch(photos: List<PhotoItem>): DeleteResult = withContext(Dispatchers.IO) {
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
                    Log.e(TAG, "Deleted $deleted of ${uris.size} queued photos")
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
