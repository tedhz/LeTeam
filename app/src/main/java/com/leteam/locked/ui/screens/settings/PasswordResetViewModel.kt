package com.leteam.locked.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.leteam.locked.auth.AuthRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PasswordResetViewModel(
    private val authRepo: AuthRepo = AuthRepo()
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun sendPasswordResetEmail() {
        val trimmedEmail = _email.value.trim()

        if (trimmedEmail.isBlank()) {
            _errorMessage.value = "Please enter your email."
            return
        }

        _isLoading.value = true
        _successMessage.value = null
        _errorMessage.value = null

        authRepo.sendPasswordResetEmail(trimmedEmail) { result ->
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Password reset email sent."
            }.onFailure { exception ->
                _errorMessage.value = exception.message ?: "Failed to send password reset email."
            }
        }
    }

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
}