package com.example.snapswipe.ui

import android.content.ContentResolver
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.snapswipe.data.PhotoItem
import com.example.snapswipe.data.PhotoRepository
import com.example.snapswipe.data.SortOrder
import com.example.snapswipe.data.DeleteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.IntentSender

data class SnapSwipeUiState(
    val photos: List<PhotoItem> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val errorMessage: String? = null,
    val pendingDeleteIntent: IntentSender? = null
) {
    val currentPhoto: PhotoItem? get() = photos.getOrNull(currentIndex)
    val isAtEnd: Boolean get() = photos.isNotEmpty() && currentIndex >= photos.size
}

class SnapSwipeViewModel(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SnapSwipeUiState(isLoading = true))
    val uiState: StateFlow<SnapSwipeUiState> = _uiState

    init {
        loadPhotos(SortOrder.NEWEST_FIRST)
    }

    fun loadPhotos(sortOrder: SortOrder = _uiState.value.sortOrder) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    sortOrder = sortOrder,
                    currentIndex = 0
                )
            }
            try {
                val photos = repository.loadPhotos(sortOrder)
                _uiState.update {
                    it.copy(
                        photos = photos,
                        currentIndex = 0,
                        isLoading = false,
                        errorMessage = null
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
        advanceIndex()
    }

    fun trashCurrent() {
        val photo = _uiState.value.currentPhoto ?: return
        viewModelScope.launch {
            when (val result = repository.deletePhoto(photo)) {
                is DeleteResult.Success -> removeCurrentAndAdvance()
                is DeleteResult.RequiresUserApproval -> {
                    _uiState.update { it.copy(pendingDeleteIntent = result.intentSender) }
                }
                is DeleteResult.Error -> {
                    _uiState.update { it.copy(errorMessage = "Unable to delete photo") }
                }
            }
        }
    }

    fun onDeleteCompleted(success: Boolean) {
        if (success) {
            removeCurrentAndAdvance()
        }
        _uiState.update { it.copy(pendingDeleteIntent = null) }
    }

    fun restart() {
        _uiState.update { it.copy(currentIndex = 0) }
    }

    fun reload() {
        loadPhotos(_uiState.value.sortOrder)
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

    private fun removeCurrentAndAdvance() {
        _uiState.update { state ->
            if (state.photos.isEmpty()) return@update state.copy(pendingDeleteIntent = null)
            val currentIdx = state.currentIndex
            val updated = state.photos.toMutableList().also { list ->
                if (currentIdx in list.indices) {
                    list.removeAt(currentIdx)
                }
            }
            val nextIndex = if (updated.isEmpty()) 0 else currentIdx.coerceAtMost(updated.lastIndex)
            state.copy(
                photos = updated,
                currentIndex = nextIndex,
                pendingDeleteIntent = null,
                errorMessage = null
            )
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
