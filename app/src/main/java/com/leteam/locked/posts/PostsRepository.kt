package com.leteam.locked.posts

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

data class Post(
    val id: String = "",
    val caption: String = "",
    val ownerUserId: String = "",
    val photoUrl: String = "",
    val createdAt: Date = Date()
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorUserId: String = "",
    val text: String = "",
    val createdAt: Date = Date()
)

class PostsRepository(
    private val db: FirebaseFirestore
) {

    private fun postsCollection() = db.collection("posts")
    private fun postDoc(postId: String) = postsCollection().document(postId)

    private fun userDoc(userId: String) = db.collection("users").document(userId)
    private fun followsCollection(userId: String) = userDoc(userId).collection("follows")

    private fun postLikesCollection(postId: String) = postDoc(postId).collection("likes")
    private fun postCommentsCollection(postId: String) = postDoc(postId).collection("comments")
    private fun commentLikesCollection(postId: String, commentId: String) =
        postCommentsCollection(postId).document(commentId).collection("likes")

    fun createPost(
        ownerUserId: String,
        caption: String,
        photoUrl: String,
        updateDailyPostStatus: Boolean = true,
        onResult: (Result<String>) -> Unit
    ) {
        val batch = db.batch()

        val newPostRef = postsCollection().document()
        val postId = newPostRef.id

        val postPayload = hashMapOf(
            "caption" to caption,
            "ownerUserId" to ownerUserId,
            "photoUrl" to photoUrl,
            "createdAt" to Timestamp.now()
        )
        batch.set(newPostRef, postPayload)

        if (updateDailyPostStatus) {
            val dailyStatusPayload = hashMapOf(
                "hasPostedToday" to true,
                "postId" to postId
            )
            batch.set(
                userDoc(ownerUserId),
                mapOf("dailyPostStatus" to dailyStatusPayload),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }

        batch.commit()
            .addOnSuccessListener { onResult(Result.success(postId)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun getPost(
        postId: String,
        onResult: (Result<Post>) -> Unit
    ) {
        postDoc(postId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(Result.failure(NoSuchElementException("Post not found")))
                    return@addOnSuccessListener
                }

                val createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                val post = Post(
                    id = doc.id,
                    caption = doc.getString("caption") ?: "",
                    ownerUserId = doc.getString("ownerUserId") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    createdAt = createdAt
                )
                onResult(Result.success(post))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun getPostsByUser(
        ownerUserId: String,
        limit: Long = 50, // temp paging val
        onResult: (Result<List<Post>>) -> Unit
    ) {
        postsCollection()
            .whereEqualTo("ownerUserId", ownerUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = snapshot.documents.map { doc ->
                    val createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                    Post(
                        id = doc.id,
                        caption = doc.getString("caption") ?: "",
                        ownerUserId = doc.getString("ownerUserId") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        createdAt = createdAt
                    )
                }
                onResult(Result.success(posts))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun getFeedPosts(
        userId: String,
        limit: Long = 50, // temp val for now?
        onResult: (Result<List<Post>>) -> Unit
    ) {
        followsCollection(userId).get()
            .addOnSuccessListener { followsSnap ->
                val followedIds = followsSnap.documents.map { it.id }.toMutableList()
                followedIds.add(userId)

                val chunks = followedIds.distinct().chunked(10)
                if (chunks.isEmpty()) {
                    onResult(Result.success(emptyList()))
                    return@addOnSuccessListener
                }

                val allPosts = mutableListOf<Post>()
                var completed = 0
                var failed = false

                fun finishIfDone() {
                    if (failed) return
                    if (completed == chunks.size) {
                        val sorted = allPosts
                            .sortedByDescending { it.createdAt.time }
                            .take(limit.toInt())
                        onResult(Result.success(sorted))
                    }
                }

                chunks.forEach { chunk ->
                    postsCollection()
                        .whereIn("ownerUserId", chunk)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                        .addOnSuccessListener { postsSnap ->
                            val posts = postsSnap.documents.map { doc ->
                                val createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                                Post(
                                    id = doc.id,
                                    caption = doc.getString("caption") ?: "",
                                    ownerUserId = doc.getString("ownerUserId") ?: "",
                                    photoUrl = doc.getString("photoUrl") ?: "",
                                    createdAt = createdAt
                                )
                            }
                            allPosts.addAll(posts)
                            completed += 1
                            finishIfDone()
                        }
                        .addOnFailureListener { e ->
                            failed = true
                            onResult(Result.failure(e))
                        }
                }
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun likePost(
        postId: String,
        likerUserId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val likeRef = postLikesCollection(postId).document(likerUserId)
        val payload = hashMapOf(
            "createdAt" to Timestamp.now()
        )
        likeRef.set(payload)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun unlikePost(
        postId: String,
        likerUserId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        postLikesCollection(postId).document(likerUserId)
            .delete()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun isPostLikedByUser(
        postId: String,
        likerUserId: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        postLikesCollection(postId).document(likerUserId)
            .get()
            .addOnSuccessListener { doc -> onResult(Result.success(doc.exists())) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun addComment(
        postId: String,
        authorUserId: String,
        text: String,
        onResult: (Result<String>) -> Unit
    ) {
        val newCommentRef = postCommentsCollection(postId).document()
        val commentId = newCommentRef.id

        val payload = hashMapOf(
            "authorUserId" to authorUserId,
            "text" to text,
            "createdAt" to Timestamp.now()
        )

        newCommentRef.set(payload)
            .addOnSuccessListener { onResult(Result.success(commentId)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun getComments(
        postId: String,
        onResult: (Result<List<Comment>>) -> Unit
    ) {
        postCommentsCollection(postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val comments = snapshot.documents.map { doc ->
                    val createdAt = doc.getTimestamp("createdAt")?.toDate() ?: Date()
                    Comment(
                        id = doc.id,
                        postId = postId,
                        authorUserId = doc.getString("authorUserId") ?: "",
                        text = doc.getString("text") ?: "",
                        createdAt = createdAt
                    )
                }
                onResult(Result.success(comments))
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun likeComment(
        postId: String,
        commentId: String,
        likerUserId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val likeRef = commentLikesCollection(postId, commentId).document(likerUserId)
        val payload = hashMapOf(
            "createdAt" to Timestamp.now()
        )
        likeRef.set(payload)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun unlikeComment(
        postId: String,
        commentId: String,
        likerUserId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        commentLikesCollection(postId, commentId).document(likerUserId)
            .delete()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun isCommentLikedByUser(
        postId: String,
        commentId: String,
        likerUserId: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        commentLikesCollection(postId, commentId).document(likerUserId)
            .get()
            .addOnSuccessListener { doc -> onResult(Result.success(doc.exists())) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }
}
