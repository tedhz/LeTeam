package com.leteam.locked.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.Comment
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
    val ownerDisplayName: String,
    val commentCount: Int = 0
)

data class CommentWithAuthor(
    val comment: Comment,
    val authorDisplayName: String,
    val authorFullName: String
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

    private val _commentsDrawerPostId = MutableStateFlow<String?>(null)
    val commentsDrawerPostId: StateFlow<String?> = _commentsDrawerPostId.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentWithAuthor>>(emptyList())
    val comments: StateFlow<List<CommentWithAuthor>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

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
                                postsWithUsers.forEach { postWithUser ->
                                    postsRepository.getCommentCount(postWithUser.post.id) { countResult ->
                                        countResult.onSuccess { count ->
                                            updateCommentCount(postWithUser.post.id, count)
                                        }
                                    }
                                }
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

    fun updateCommentCount(postId: String, count: Int) {
        _feedPosts.value = _feedPosts.value.map { postWithUser ->
            if (postWithUser.post.id == postId) postWithUser.copy(commentCount = count) else postWithUser
        }
    }

    fun openCommentsDrawer(postId: String) {
        _commentsDrawerPostId.value = postId
        loadComments(postId)
    }

    fun closeCommentsDrawer() {
        _commentsDrawerPostId.value = null
        _comments.value = emptyList()
    }

    fun loadComments(postId: String) {
        _commentsLoading.value = true
        _comments.value = emptyList()

        postsRepository.getComments(postId) { result ->
            result.onSuccess { comments ->
                if (comments.isEmpty()) {
                    _comments.value = emptyList()
                    _commentsLoading.value = false
                    updateCommentCount(postId, 0)
                    return@getComments
                }
                val uniqueAuthorIds = comments.map { it.authorUserId }.distinct()
                val authorMap = mutableMapOf<String, Pair<String, String>>()
                var completed = 0
                val total = uniqueAuthorIds.size

                uniqueAuthorIds.forEach { authorId ->
                    userRepository.getUser(authorId) { userResult ->
                        userResult.onSuccess { user ->
                            authorMap[authorId] = Pair(
                                if (user.displayName.isNotBlank()) user.displayName else user.fullName.ifBlank { "User" },
                                user.fullName.ifBlank { "User" }
                            )
                        }
                        userResult.onFailure {
                            authorMap[authorId] = Pair("User", "User")
                        }
                        completed++
                        if (completed == total) {
                            _comments.value = comments.map { comment ->
                                val (displayName, fullName) = authorMap[comment.authorUserId] ?: Pair("User", "User")
                                CommentWithAuthor(
                                    comment = comment,
                                    authorDisplayName = displayName,
                                    authorFullName = fullName
                                )
                            }
                            _commentsLoading.value = false
                            updateCommentCount(postId, comments.size)
                        }
                    }
                }
            }
            result.onFailure {
                _comments.value = emptyList()
                _commentsLoading.value = false
            }
        }
    }

    fun addComment(postId: String, text: String, onDone: () -> Unit = {}) {
        val userId = currentUserId ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            postsRepository.addComment(postId, userId, text.trim()) { result ->
                result.onSuccess {
                    loadComments(postId)
                    onDone()
                }
            }
        }
    }
}
