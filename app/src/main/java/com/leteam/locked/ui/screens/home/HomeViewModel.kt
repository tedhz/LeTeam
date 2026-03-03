package com.leteam.locked.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.Post
import com.leteam.locked.posts.PostsRepository
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PostWithUser(
    val post: Post,
    val ownerFullName: String,
    val ownerDisplayName: String
)

class HomeViewModel(
    private val userRepository: UserRepository = UserRepository(FirebaseProvider.firestore),
    private val postsRepository: PostsRepository = PostsRepository(FirebaseProvider.firestore)
) : ViewModel() {

    private val auth = FirebaseProvider.auth

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _feedPosts = MutableStateFlow<List<PostWithUser>>(emptyList())
    val feedPosts: StateFlow<List<PostWithUser>> = _feedPosts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        val userId = currentUserId ?: return
        _isLoading.value = true

        viewModelScope.launch {
            // Load current user
            userRepository.getUser(userId) { result ->
                result.onSuccess { user ->
                    _currentUser.value = user
                }
                result.onFailure {
                    _currentUser.value = null
                }
            }

            // Load feed posts
            postsRepository.getFeedPosts(userId, limit = 50) { postsResult ->
                postsResult.onSuccess { posts ->
                    if (posts.isEmpty()) {
                        _feedPosts.value = emptyList()
                        _isLoading.value = false
                        return@getFeedPosts
                    }

                    val postsWithUsers = mutableListOf<PostWithUser>()
                    var remaining = posts.size
                    val uniqueUserIds = posts.map { it.ownerUserId }.distinct()

                    if (uniqueUserIds.isEmpty()) {
                        _feedPosts.value = emptyList()
                        _isLoading.value = false
                        return@getFeedPosts
                    }

                    val userInfoMap = mutableMapOf<String, Pair<String, String>>()
                    var completedCount = 0
                    val totalUsers = uniqueUserIds.size

                    uniqueUserIds.forEach { ownerUserId ->
                        userRepository.getUser(ownerUserId) { userResult ->
                            userResult.onSuccess { ownerUser ->
                                userInfoMap[ownerUserId] = Pair(
                                    ownerUser.fullName.ifBlank { "User" },
                                    if (ownerUser.displayName.isNotBlank()) "@${ownerUser.displayName}" else ownerUser.email
                                )
                            }
                            userResult.onFailure {
                                userInfoMap[ownerUserId] = Pair("User", "")
                            }

                            completedCount++
                            if (completedCount == totalUsers) {
                                posts.forEach { post ->
                                    val (fullName, displayName) = userInfoMap[post.ownerUserId] ?: Pair("User", "")
                                    postsWithUsers.add(
                                        PostWithUser(
                                            post = post,
                                            ownerFullName = fullName,
                                            ownerDisplayName = displayName
                                        )
                                    )
                                }

                                _feedPosts.value = postsWithUsers
                                _isLoading.value = false
                            }
                        }
                    }
                }
                postsResult.onFailure {
                    _feedPosts.value = emptyList()
                    _isLoading.value = false
                }
            }
        }
    }

    fun refresh() {
        loadHomeData()
    }

    fun toggleLike(postId: String) {
        val userId = currentUserId ?: return

        viewModelScope.launch {
            val currentPosts = _feedPosts.value
            val postWithUser = currentPosts.find { it.post.id == postId } ?: return@launch
            val isLiked = postWithUser.post.likes.contains(userId)

            if (isLiked) {
                postsRepository.unlikePost(postId, userId) { result ->
                    result.onSuccess {
                        updatePostLikes(postId, userId, remove = true)
                    }
                }
            } else {
                postsRepository.likePost(postId, userId) { result ->
                    result.onSuccess {
                        updatePostLikes(postId, userId, remove = false)
                    }
                }
            }
        }
    }

    private fun updatePostLikes(postId: String, userId: String, remove: Boolean) {
        val currentPosts = _feedPosts.value.toMutableList()
        val index = currentPosts.indexOfFirst { it.post.id == postId }
        if (index >= 0) {
            val postWithUser = currentPosts[index]
            val updatedLikes = if (remove) {
                postWithUser.post.likes.filter { it != userId }
            } else {
                postWithUser.post.likes + userId
            }
            val updatedPost = postWithUser.post.copy(likes = updatedLikes)
            currentPosts[index] = postWithUser.copy(post = updatedPost)
            _feedPosts.value = currentPosts
        }
    }
}
