package com.leteam.locked.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.Post
import com.leteam.locked.posts.PostsRepository
import com.leteam.locked.users.FollowRepository
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import com.leteam.locked.workout.Workout
import com.leteam.locked.workout.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository = UserRepository(FirebaseProvider.firestore),
    private val followRepository: FollowRepository = FollowRepository(FirebaseProvider.firestore),
    private val postsRepository: PostsRepository = PostsRepository(FirebaseProvider.firestore),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(FirebaseProvider.firestore)
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

    private val _recentPosts = MutableStateFlow<List<Post>>(emptyList())
    val recentPosts: StateFlow<List<Post>> = _recentPosts.asStateFlow()

    private val _recentWorkouts = MutableStateFlow<List<Workout>>(emptyList())
    val recentWorkouts: StateFlow<List<Workout>> = _recentWorkouts.asStateFlow()

    private val _followerUsers = MutableStateFlow<List<User>>(emptyList())
    val followerUsers: StateFlow<List<User>> = _followerUsers.asStateFlow()

    private val _followingUsers = MutableStateFlow<List<User>>(emptyList())
    val followingUsers: StateFlow<List<User>> = _followingUsers.asStateFlow()

    private val _followListLoading = MutableStateFlow(false)
    val followListLoading: StateFlow<Boolean> = _followListLoading.asStateFlow()

    private val _likesDrawerPostId = MutableStateFlow<String?>(null)
    val likesDrawerPostId: StateFlow<String?> = _likesDrawerPostId.asStateFlow()

    private val _likeUsers = MutableStateFlow<List<User>>(emptyList())
    val likeUsers: StateFlow<List<User>> = _likeUsers.asStateFlow()

    private val _likesListLoading = MutableStateFlow(false)
    val likesListLoading: StateFlow<Boolean> = _likesListLoading.asStateFlow()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // If [profileUserId] is null, this loads the current user's profile.
    fun loadProfile(profileUserId: String? = null) {
        val targetId = profileUserId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            loadUser(targetId)
            loadCounts(targetId)
            loadRecentPosts(targetId)
            loadRecentWorkouts(targetId)
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

    private fun loadRecentPosts(userId: String) {
        postsRepository.getPostsByUser(userId, 20) { result ->
            result.onSuccess { _recentPosts.value = it }
            result.onFailure { _recentPosts.value = emptyList() }
        }
    }

    private fun loadRecentWorkouts(userId: String) {
        workoutRepository.getWorkouts(userId) { result ->
            result.onSuccess { _recentWorkouts.value = it.take(1) }
            result.onFailure { _recentWorkouts.value = emptyList() }
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

    fun openLikesDrawer(postId: String) {
        _likesDrawerPostId.value = postId
        val ids = _recentPosts.value.find { it.id == postId }?.likes ?: emptyList()
        loadUsersByIds(ids) { ordered ->
            _likeUsers.value = ordered
            _likesListLoading.value = false
        }
    }

    fun closeLikesDrawer() {
        _likesDrawerPostId.value = null
        _likeUsers.value = emptyList()
        _likesListLoading.value = false
    }

    private fun loadUsersByIds(ids: List<String>, onComplete: (List<User>) -> Unit) {
        val orderedIds = ids.distinct()
        if (orderedIds.isEmpty()) {
            _likesListLoading.value = false
            onComplete(emptyList())
            return
        }
        _likesListLoading.value = true
        _likeUsers.value = emptyList()
        val userMap = mutableMapOf<String, User>()
        var completed = 0
        orderedIds.forEach { id ->
            userRepository.getUser(id) { userResult ->
                userResult.onSuccess { userMap[id] = it }
                userResult.onFailure {
                    userMap[id] = User(userId = id, fullName = "User")
                }
                completed++
                if (completed == orderedIds.size) {
                    val ordered = orderedIds.mapNotNull { userMap[it] }
                    onComplete(ordered)
                }
            }
        }
    }

    fun loadFollowerUsers(profileUserId: String) {
        _followListLoading.value = true
        _followerUsers.value = emptyList()
        followRepository.getFollowerIds(profileUserId) { result ->
            result.onSuccess { ids ->
                if (ids.isEmpty()) {
                    _followerUsers.value = emptyList()
                    _followListLoading.value = false
                    return@onSuccess
                }
                val list = mutableListOf<User>()
                var remaining = ids.size
                ids.forEach { id ->
                    userRepository.getUser(id) { userResult ->
                        userResult.onSuccess { list.add(it) }
                        remaining--
                        if (remaining == 0) {
                            _followerUsers.value = list
                            _followListLoading.value = false
                        }
                    }
                }
            }
            result.onFailure { _followListLoading.value = false }
        }
    }

    fun loadFollowingUsers(profileUserId: String) {
        _followListLoading.value = true
        _followingUsers.value = emptyList()
        followRepository.getFollowingIds(profileUserId) { result ->
            result.onSuccess { ids ->
                if (ids.isEmpty()) {
                    _followingUsers.value = emptyList()
                    _followListLoading.value = false
                    return@onSuccess
                }
                val list = mutableListOf<User>()
                var remaining = ids.size
                ids.forEach { id ->
                    userRepository.getUser(id) { userResult ->
                        userResult.onSuccess { list.add(it) }
                        remaining--
                        if (remaining == 0) {
                            _followingUsers.value = list
                            _followListLoading.value = false
                        }
                    }
                }
            }
            result.onFailure { _followListLoading.value = false }
        }
    }
}
