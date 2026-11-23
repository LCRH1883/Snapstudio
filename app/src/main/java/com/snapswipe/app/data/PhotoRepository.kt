package com.snapswipe.app.data

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.content.IntentSender
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

sealed class DeleteResult {
    object Success : DeleteResult()
    data class RequiresUserApproval(
        val intentSender: IntentSender,
        val retryAfterApproval: Boolean = false
    ) : DeleteResult()
    data class Error(val throwable: Throwable) : DeleteResult()
}

interface PhotoDataSource {
    suspend fun loadPhotos(sortOrder: SortOrder): List<PhotoItem>
    val supportsTrash: Boolean
    suspend fun removePhotos(photos: List<PhotoItem>, afterApproval: Boolean = false): DeleteResult
    suspend fun restorePhotos(photos: List<PhotoItem>): DeleteResult
    suspend fun findVisiblePhotos(photos: List<PhotoItem>): List<PhotoItem>
}

class PhotoRepository(
    private val contentResolver: ContentResolver
) : PhotoDataSource {

    override suspend fun loadPhotos(sortOrder: SortOrder): List<PhotoItem> = withContext(Dispatchers.IO) {
        try {
            val photos = mutableListOf<PhotoItem>()
            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = buildList {
                add(MediaStore.Images.Media._ID)
                add(MediaStore.Images.Media.DATE_TAKEN)
                add(MediaStore.Images.Media.DATE_ADDED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.MediaColumns.VOLUME_NAME)
            }.toTypedArray()
            val cursor = contentResolver.query(collection, projection, null, null, null)

            if (cursor == null) {
                Log.w(TAG, "MediaStore query returned null cursor; possible missing permission")
                return@withContext emptyList()
            }

            cursor.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateTakenColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                val volumeColumn = it.getColumnIndex(MediaStore.MediaColumns.VOLUME_NAME)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val dateTaken = it.getLongOrNull(dateTakenColumn)?.takeIf { value -> value != 0L }
                        ?: it.getLongOrNull(dateAddedColumn)?.takeIf { value -> value > 0L }?.times(1000L)
                    val volume = if (volumeColumn >= 0) it.getString(volumeColumn) else null
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volume != null) {
                        ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri(volume), id)
                    } else {
                        ContentUris.withAppendedId(collection, id)
                    }

                    photos += PhotoItem(
                        id = id,
                        uri = uri,
                        dateTaken = dateTaken
                    )
                }
            }

            Log.d(TAG, "Loaded ${photos.size} photos with order $sortOrder")
            val chronological = compareBy<PhotoItem>({ it.dateTaken ?: Long.MIN_VALUE }, { it.id }, { it.uri.toString() })
            photos.sortedWith(if (sortOrder == SortOrder.OLDEST_FIRST) chronological else chronological.reversed())
        } catch (se: SecurityException) {
            Log.e(TAG, "Permission denied while loading photos", se)
            throw se
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load photos", e)
            throw e
        }
    }

    override val supportsTrash: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    override suspend fun removePhotos(photos: List<PhotoItem>, afterApproval: Boolean): DeleteResult =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val request = MediaStore.createTrashRequest(contentResolver, photos.map { it.uri }, true)
                    DeleteResult.RequiresUserApproval(request.intentSender)
                } else {
                    val photo = photos.single()
                    if (contentResolver.delete(photo.uri, null, null) > 0) {
                        DeleteResult.Success
                    } else {
                        DeleteResult.Error(IllegalStateException("Photo was not deleted"))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                    !afterApproval && e is RecoverableSecurityException
                ) {
                    DeleteResult.RequiresUserApproval(e.userAction.actionIntent.intentSender, retryAfterApproval = true)
                } else {
                    DeleteResult.Error(e)
                }
            } catch (e: Exception) {
                DeleteResult.Error(e)
            }
        }

    override suspend fun restorePhotos(photos: List<PhotoItem>): DeleteResult = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                return@withContext DeleteResult.Error(UnsupportedOperationException("Trash is unavailable"))
            }
            val request = MediaStore.createTrashRequest(contentResolver, photos.map { it.uri }, false)
            DeleteResult.RequiresUserApproval(request.intentSender)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DeleteResult.Error(e)
        }
    }

    override suspend fun findVisiblePhotos(photos: List<PhotoItem>): List<PhotoItem> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()
        val projection = arrayOf(MediaStore.MediaColumns.IS_TRASHED)
        photos.filter { photo ->
            contentResolver.query(photo.uri, projection, null, null, null)
                ?.use { it.moveToFirst() && it.getInt(0) == 0 } == true
        }
    }

    private fun Cursor.getLongOrNull(index: Int): Long? {
        return if (isNull(index)) null else getLong(index)
    }

    private companion object {
        private const val TAG = "PhotoRepository"
    }
}
