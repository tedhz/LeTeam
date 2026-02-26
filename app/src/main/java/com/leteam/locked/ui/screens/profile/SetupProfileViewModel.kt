package com.leteam.locked.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.users.UserRepository

class SetupProfileViewModel(
    private val userRepository: UserRepository = UserRepository(FirebaseProvider.firestore)
) : ViewModel() {

    fun saveDisplayName(displayName: String, onResult: (Boolean) -> Unit) {
        val userId = FirebaseProvider.auth.currentUser?.uid
        if (userId == null || displayName.isBlank()) {
            onResult(false)
            return
        }

        userRepository.updateDisplayName(userId, displayName) { result ->
            onResult(result.isSuccess)
        }
    }
}