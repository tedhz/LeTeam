package com.leteam.locked.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.PostsRepository
import com.leteam.locked.users.FollowRepository
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(FirebaseProvider.firestore),
    private val followRepository: FollowRepository = FollowRepository(FirebaseProvider.firestore),
    private val postsRepository: PostsRepository = PostsRepository(FirebaseProvider.firestore)
) : ViewModel() {

    private val auth = FirebaseProvider.auth

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _followerCount = MutableStateFlow(0)
    val followerCount: StateFlow<Int> = _followerCount.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    private val _postCount = MutableStateFlow(0)
    val postCount: StateFlow<Int> = _postCount.asStateFlow()

    // null => viewing your own profile; true/false => viewing another user's profile
    private val _isFollowing = MutableStateFlow<Boolean?>(null)
    val isFollowing: StateFlow<Boolean?> = _isFollowing.asStateFlow()

    private val _followInProgress = MutableStateFlow(false)
    val followInProgress: StateFlow<Boolean> = _followInProgress.asStateFlow()

    private val _followError = MutableStateFlow<String?>(null)
    val followError: StateFlow<String?> = _followError.asStateFlow()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // If [profileUserId] is null, this loads the current user's profile.
    fun loadProfile(profileUserId: String? = null) {
        val targetId = profileUserId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            loadUser(targetId)
            loadCounts(targetId)
            if (currentUserId != null && currentUserId != targetId) {
                loadIsFollowing(currentUserId!!, targetId)
            } else {
                _isFollowing.value = null
            }
        }
    }

    private suspend fun loadUser(userId: String) {
        userRepository.getUser(userId) { result ->
            result.onSuccess { _user.value = it }
            result.onFailure { _user.value = null }
        }
    }

    private fun loadCounts(userId: String) {
        followRepository.getFollowerIds(userId) { result ->
            result.onSuccess { _followerCount.value = it.size }
        }
        followRepository.getFollowingIds(userId) { result ->
            result.onSuccess { _followingCount.value = it.size }
        }
        postsRepository.getPostsByUser(userId, 500) { result ->
            result.onSuccess { _postCount.value = it.size }
        }
    }

    private fun loadIsFollowing(me: String, target: String) {
        followRepository.isFollowing(me, target) { result ->
            result.onSuccess { _isFollowing.value = it }
        }
    }

    fun toggleFollow() {
        val me = currentUserId ?: return
        val target = _user.value?.userId ?: return
        if (me == target) return
        val currentlyFollowing = _isFollowing.value ?: return
        if (_followInProgress.value) return

        _followInProgress.value = true
        _followError.value = null

        if (currentlyFollowing) {
            followRepository.unfollow(me, target) { result ->
                _followInProgress.value = false
                result
                    .onSuccess {
                        _isFollowing.value = false
                        // Update UI immediately; source-of-truth stays in Firestore.
                        _followerCount.value = (_followerCount.value - 1).coerceAtLeast(0)
                    }
                    .onFailure { _followError.value = it.message }
            }
        } else {
            followRepository.follow(me, target) { result ->
                _followInProgress.value = false
                result
                    .onSuccess {
                        _isFollowing.value = true
                        // Update UI immediately; source-of-truth stays in Firestore.
                        _followerCount.value = _followerCount.value + 1
                    }
                    .onFailure { _followError.value = it.message }
            }
        }
    }

    fun clearFollowError() {
        _followError.value = null
    }
}
