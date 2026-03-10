package com.leteam.locked.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.users.UserRepository

class SetupProfileViewModel(
    private val userRepository: UserRepository = UserRepository(FirebaseProvider.firestore)
) : ViewModel() {

    fun saveDisplayName(displayName: String, onResult: (Boolean, String?) -> Unit) {
        val userId = FirebaseProvider.auth.currentUser?.uid
        val trimmedName = displayName.trim()

        if (userId == null || trimmedName.isBlank()) {
            onResult(false, "Display name cannot be blank.")
            return
        }

        userRepository.isDisplayNameTaken(trimmedName) { takenResult ->
            takenResult
                .onSuccess { isTaken ->
                    if (isTaken) {
                        onResult(false, "That display name is already taken.")
                    } else {
                        userRepository.updateDisplayName(userId, trimmedName) { result ->
                            if (result.isSuccess) {
                                onResult(true, null)
                            } else {
                                onResult(false, "Failed to save display name. Try again.")
                            }
                        }
                    }
                }
                .onFailure {
                    onResult(false, "Failed to check display name. Try again.")
                }
        }
    }
}