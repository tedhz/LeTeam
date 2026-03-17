package com.leteam.locked.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.photos.PhotoRepository
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val fullName: String = "",
    val handle: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val selectedPhotoUri: Uri? = null,
    val error: String? = null,
    val handleError: String? = null
)

class EditProfileViewModel(
    private val userRepository: UserRepository = UserRepository(FirebaseProvider.firestore),
    private val photoRepository: PhotoRepository = PhotoRepository(FirebaseProvider.storage)
) : ViewModel() {

    private val auth = FirebaseProvider.auth

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val handleRegex = Regex("^[a-z0-9_]{3,20}$")
    private var originalHandle: String? = null

    fun load() {
        val uid = auth.currentUser?.uid ?: run {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Not logged in")
            return
        }
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        userRepository.getUser(uid) { result ->
            result.onSuccess { user ->
                originalHandle = user.displayName.trim().removePrefix("@").lowercase()
                _uiState.value = EditProfileUiState(
                    isLoading = false,
                    fullName = user.fullName,
                    handle = user.displayName,
                    bio = user.bio,
                    photoUrl = user.photoUrl
                )
            }
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load profile")
            }
        }
    }

    fun setFullName(value: String) {
        _uiState.value = _uiState.value.copy(fullName = value, error = null)
    }

    fun setHandle(value: String) {
        val normalized = value.trim().removePrefix("@").lowercase()
        _uiState.value = _uiState.value.copy(handle = normalized, handleError = null, error = null)
    }

    fun setBio(value: String) {
        _uiState.value = _uiState.value.copy(bio = value, error = null)
    }

    fun setSelectedPhoto(uri: Uri?) {
        _uiState.value = _uiState.value.copy(selectedPhotoUri = uri, error = null)
    }

    fun save(contentType: String = "image/jpeg", onSaved: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val current = _uiState.value

        val fullName = current.fullName.trim()
        val handle = current.handle.trim().removePrefix("@").lowercase()

        if (fullName.isBlank()) {
            _uiState.value = current.copy(error = "Name can't be blank")
            return
        }

        if (!handleRegex.matches(handle)) {
            _uiState.value = current.copy(handleError = "Handle must be 3–20 chars (a-z, 0-9, _)")
            return
        }

        _uiState.value = current.copy(isSaving = true, error = null, handleError = null)

        if (originalHandle != null && handle == originalHandle) {
            saveWithAvailabilityConfirmed(uid, fullName, handle, contentType, onSaved)
            return
        }

        userRepository.isDisplayNameAvailable(handle, uid) { availResult ->
            val available = availResult.getOrElse {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Failed to check handle")
                return@isDisplayNameAvailable
            }
            if (!available) {
                _uiState.value = _uiState.value.copy(isSaving = false, handleError = "That handle is taken")
                return@isDisplayNameAvailable
            }
            saveWithAvailabilityConfirmed(uid, fullName, handle, contentType, onSaved)
        }
    }

    private fun saveWithAvailabilityConfirmed(
        uid: String,
        fullName: String,
        handle: String,
        contentType: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val photoUri = _uiState.value.selectedPhotoUri
            val uploadedUrl = if (photoUri != null) {
                runCatching {
                    photoRepository.uploadPhoto(
                        type = PhotoRepository.PhotoType.PROFILE_PHOTO,
                        userId = uid,
                        imageUri = photoUri,
                        contentType = contentType
                    )
                }.getOrElse {
                    _uiState.value = _uiState.value.copy(isSaving = false, error = it.message ?: "Failed to upload photo")
                    return@launch
                }
            } else {
                null
            }

            val updates = buildMap<String, Any> {
                put("fullName", fullName)
                put("displayName", handle)
                put("bio", _uiState.value.bio.trim())
                if (uploadedUrl != null) put("photoUrl", uploadedUrl)
            }

            userRepository.updateProfileFields(uid, updates) { updateResult ->
                updateResult.onSuccess {
                    originalHandle = handle
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        photoUrl = uploadedUrl ?: _uiState.value.photoUrl,
                        selectedPhotoUri = null
                    )
                    onSaved()
                }
                updateResult.onFailure { e ->
                    _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "Failed to save profile")
                }
            }
        }
    }
}

