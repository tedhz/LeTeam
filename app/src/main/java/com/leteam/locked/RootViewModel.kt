package com.leteam.locked

import androidx.lifecycle.ViewModel
import com.leteam.locked.auth.AuthRepo
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.users.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AppState {
    data object Loading : AppState()
    data object Unauthenticated : AppState()
    data object NeedsProfileSetup : AppState()
    data object Authenticated : AppState()
}

class RootViewModel(
    private val authRepo: AuthRepo = AuthRepo(),
    private val userRepo: UserRepository = UserRepository(FirebaseProvider.firestore)
) : ViewModel() {

    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        _appState.value = AppState.Loading
        val currentUser = authRepo.getCurrentUser()

        if (currentUser == null) {
            _appState.value = AppState.Unauthenticated
        } else {
            userRepo.getUser(currentUser.uid) { result ->
                result.onSuccess { user ->
                    if (user.displayName.isBlank()) {
                        _appState.value = AppState.NeedsProfileSetup
                    } else {
                        _appState.value = AppState.Authenticated
                    }
                }
                result.onFailure {
                    _appState.value = AppState.Unauthenticated
                }
            }
        }
    }
}