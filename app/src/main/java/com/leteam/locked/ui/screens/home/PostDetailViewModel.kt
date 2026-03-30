package com.leteam.locked.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.Comment
import com.leteam.locked.posts.PostsRepository
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val userRepository: UserRepository = UserRepository(FirebaseProvider.firestore),
    private val postsRepository: PostsRepository = PostsRepository(FirebaseProvider.firestore)
) : ViewModel() {

    private val auth = FirebaseProvider.auth

    private val _postWithUser = MutableStateFlow<PostWithUser?>(null)
    val postWithUser: StateFlow<PostWithUser?> = _postWithUser.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _commentsDrawerOpen = MutableStateFlow(false)
    val commentsDrawerOpen: StateFlow<Boolean> = _commentsDrawerOpen.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentWithAuthor>>(emptyList())
    val comments: StateFlow<List<CommentWithAuthor>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    private val _likesDrawerOpen = MutableStateFlow(false)
    val likesDrawerOpen: StateFlow<Boolean> = _likesDrawerOpen.asStateFlow()

    private val _likeUsers = MutableStateFlow<List<User>>(emptyList())
    val likeUsers: StateFlow<List<User>> = _likeUsers.asStateFlow()

    private val _likesListLoading = MutableStateFlow(false)
    val likesListLoading: StateFlow<Boolean> = _likesListLoading.asStateFlow()

    private val _viewerUser = MutableStateFlow<User?>(null)
    val viewerUser: StateFlow<User?> = _viewerUser.asStateFlow()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun loadViewerUser() {
        val uid = auth.currentUser?.uid ?: run {
            _viewerUser.value = null
            return
        }
        userRepository.getUser(uid) { result ->
            result.onSuccess { _viewerUser.value = it }
            result.onFailure { _viewerUser.value = null }
        }
    }

    fun loadPost(postId: String) {
        _isLoading.value = true
        _postWithUser.value = null
        loadViewerUser()

        viewModelScope.launch {
            postsRepository.getPost(postId) { postResult ->
                postResult.onSuccess { post ->
                    userRepository.getUser(post.ownerUserId) { userResult ->
                        userResult.onSuccess { user ->
                            postsRepository.getCommentCount(postId) { countResult ->
                                val commentCount = countResult.getOrNull() ?: 0
                                _postWithUser.value = PostWithUser(
                                    post = post,
                                    ownerFullName = user.fullName.ifBlank { "User" },
                                    ownerDisplayName = if (user.displayName.isNotBlank()) "@${user.displayName}" else "",
                                    ownerPhotoUrl = user.photoUrl,
                                    commentCount = commentCount
                                )
                                _isLoading.value = false
                            }
                        }
                        userResult.onFailure {
                            _postWithUser.value = PostWithUser(
                                post = post,
                                ownerFullName = "User",
                                ownerDisplayName = "",
                                ownerPhotoUrl = ""
                            )
                            _isLoading.value = false
                        }
                    }
                }
                postResult.onFailure {
                    _isLoading.value = false
                }
            }
        }
    }

    fun toggleLike(postId: String) {
        val userId = currentUserId ?: return
        val current = _postWithUser.value ?: return

        viewModelScope.launch {
            val isLiked = current.post.likes.contains(userId)
            if (isLiked) {
                postsRepository.unlikePost(postId, userId) { result ->
                    result.onSuccess { updateLikes(postId, userId, remove = true) }
                }
            } else {
                postsRepository.likePost(postId, userId) { result ->
                    result.onSuccess { updateLikes(postId, userId, remove = false) }
                }
            }
        }
    }

    private fun updateLikes(postId: String, userId: String, remove: Boolean) {
        val current = _postWithUser.value ?: return
        if (current.post.id != postId) return
        val updatedLikes = if (remove) {
            current.post.likes.filter { it != userId }
        } else {
            current.post.likes + userId
        }
        _postWithUser.value = current.copy(post = current.post.copy(likes = updatedLikes))
    }

    fun openCommentsDrawer() {
        closeLikesDrawerInternal()
        _commentsDrawerOpen.value = true
        _postWithUser.value?.post?.id?.let { loadComments(it) }
    }

    fun closeCommentsDrawer() {
        _commentsDrawerOpen.value = false
        _comments.value = emptyList()
    }

    fun openLikesDrawer() {
        _commentsDrawerOpen.value = false
        _comments.value = emptyList()
        val ids = _postWithUser.value?.post?.likes ?: emptyList()
        _likesDrawerOpen.value = true
        loadLikeUsers(ids)
    }

    fun closeLikesDrawer() {
        closeLikesDrawerInternal()
    }

    private fun closeLikesDrawerInternal() {
        _likesDrawerOpen.value = false
        _likeUsers.value = emptyList()
        _likesListLoading.value = false
    }

    private fun loadLikeUsers(likeUserIds: List<String>) {
        val orderedIds = likeUserIds.distinct()
        if (orderedIds.isEmpty()) {
            _likeUsers.value = emptyList()
            _likesListLoading.value = false
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
                    _likeUsers.value = orderedIds.mapNotNull { userMap[it] }
                    _likesListLoading.value = false
                }
            }
        }
    }

    private fun loadComments(postId: String) {
        _commentsLoading.value = true
        _comments.value = emptyList()

        postsRepository.getComments(postId) { result ->
            result.onSuccess { comments ->
                if (comments.isEmpty()) {
                    _comments.value = emptyList()
                    _commentsLoading.value = false
                    updateCommentCount(comments.size)
                    return@getComments
                }
                val uniqueAuthorIds = comments.map { it.authorUserId }.distinct()
                val authorMap = mutableMapOf<String, Triple<String, String, String>>()
                var completed = 0
                val total = uniqueAuthorIds.size

                uniqueAuthorIds.forEach { authorId ->
                    userRepository.getUser(authorId) { userResult ->
                        userResult.onSuccess { user ->
                            authorMap[authorId] = Triple(
                                if (user.displayName.isNotBlank()) user.displayName else user.fullName.ifBlank { "User" },
                                user.fullName.ifBlank { "User" },
                                user.photoUrl
                            )
                        }
                        userResult.onFailure {
                            authorMap[authorId] = Triple("User", "User", "")
                        }
                        completed++
                        if (completed == total) {
                            _comments.value = comments.map { comment ->
                                val (displayName, fullName, photoUrl) = authorMap[comment.authorUserId] ?: Triple("User", "User", "")
                                CommentWithAuthor(
                                    comment = comment,
                                    authorDisplayName = displayName,
                                    authorFullName = fullName,
                                    authorPhotoUrl = photoUrl
                                )
                            }
                            _commentsLoading.value = false
                            updateCommentCount(comments.size)
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

    private fun updateCommentCount(count: Int) {
        _postWithUser.value?.let { pw ->
            _postWithUser.value = pw.copy(commentCount = count)
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