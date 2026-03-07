package com.leteam.locked.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val userRepository: UserRepository = UserRepository(FirebaseProvider.firestore)
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<User>>(emptyList())
    val results: StateFlow<List<User>> = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun setQuery(q: String) {
        _query.value = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            runSearch(_query.value.trim())
        }
    }

    private fun runSearch(prefix: String) {
        if (prefix.isBlank()) {
            _results.value = emptyList()
            _isLoading.value = false
            return
        }
        _isLoading.value = true
        userRepository.searchUsersByDisplayName(prefix, 25) { result ->
            _results.value = result.getOrElse { emptyList() }
            _isLoading.value = false
        }
    }
}
