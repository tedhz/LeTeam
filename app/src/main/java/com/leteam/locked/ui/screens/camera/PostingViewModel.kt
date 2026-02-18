package com.leteam.locked.ui.screens.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.Post
import com.leteam.locked.posts.PostsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class PostingViewModel(
    private val postsRepository: PostsRepository = PostsRepository(FirebaseProvider.firestore)
) : ViewModel() {

    private val _uiState = MutableStateFlow<PostingUiState>(PostingUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun createPost(imageUri: Uri, caption: String) {
        if (imageUri == Uri.EMPTY) {
            _uiState.value = PostingUiState.Error("Image is missing")
            return
        }

        val userId = FirebaseProvider.auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = PostingUiState.Error("User not logged in")
            return
        }

        _uiState.value = PostingUiState.Loading

        viewModelScope.launch {
            try {
                // TODO: Upload imageUri to Blob storage here in the future

                postsRepository.createPost(
                    caption = caption,
                    ownerUserId = userId,
                    photoUrl = imageUri.toString(),
                    onResult = {}
                )
                _uiState.value = PostingUiState.Success
            } catch (e: Exception) {
                _uiState.value = PostingUiState.Error(e.message ?: "Failed to create post")
            }
        }
    }

    fun resetState() {
        _uiState.value = PostingUiState.Idle
    }
}

sealed class PostingUiState {
    data object Idle : PostingUiState()
    data object Loading : PostingUiState()
    data object Success : PostingUiState()
    data class Error(val message: String) : PostingUiState()
}