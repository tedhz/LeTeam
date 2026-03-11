package com.leteam.locked.posts

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

class PostsRepositoryTest {

    private val mockFirestore: FirebaseFirestore = mockk()
    private val mockBatch: WriteBatch = mockk(relaxed = true)

    private val mockPostsCollection: CollectionReference = mockk()
    private val mockUsersCollection: CollectionReference = mockk()

    private val mockNewPostDoc: DocumentReference = mockk()
    private val mockUserDoc: DocumentReference = mockk()
    private val mockExistingPostDoc: DocumentReference = mockk()

    private val mockLikesCollection: CollectionReference = mockk()
    private val mockLikeDoc: DocumentReference = mockk()

    private val mockCommentsCollection: CollectionReference = mockk()
    private val mockNewCommentDoc: DocumentReference = mockk()

    private val mockTaskVoid: Task<Void> = mockk()
    private val mockTaskDoc: Task<DocumentSnapshot> = mockk()
    private val mockTaskQuery: Task<QuerySnapshot> = mockk()

    private val mockPostDocSnapshot: DocumentSnapshot = mockk()
    private val mockQuery: Query = mockk()
    private val mockQuerySnapshot: QuerySnapshot = mockk()
    private val mockQueryDocSnapshot: QueryDocumentSnapshot = mockk()

    private lateinit var repository: PostsRepository

    private val testOwnerUserId = "user_123"
    private val testLikerUserId = "user_999"
    private val testPostId = "post_abc"
    private val testCommentId = "comment_123"

    @Before
    fun setUp() {
        repository = PostsRepository(mockFirestore)

        every { mockFirestore.collection("posts") } returns mockPostsCollection
        every { mockFirestore.collection("users") } returns mockUsersCollection

        every { mockPostsCollection.document() } returns mockNewPostDoc
        every { mockPostsCollection.document(any()) } returns mockExistingPostDoc

        every { mockNewPostDoc.id } returns testPostId
        every { mockExistingPostDoc.id } returns testPostId

        every { mockUsersCollection.document(any()) } returns mockUserDoc

        every { mockFirestore.batch() } returns mockBatch
        every { mockBatch.commit() } returns mockTaskVoid

        every { mockTaskVoid.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockTaskVoid
        }
        every { mockTaskVoid.addOnFailureListener(any()) } returns mockTaskVoid

        every { mockExistingPostDoc.collection("likes") } returns mockLikesCollection
        every { mockLikesCollection.document(any()) } returns mockLikeDoc

        every { mockExistingPostDoc.collection("comments") } returns mockCommentsCollection
        every { mockCommentsCollection.document() } returns mockNewCommentDoc
        every { mockNewCommentDoc.id } returns testCommentId
    }

    @Test
    fun `createPost writes post and updates dailyPostStatus when enabled`() {
        var resultCaptured: Result<String>? = null

        repository.createPost(
            ownerUserId = testOwnerUserId,
            caption = "hello",
            photoUrl = "https://photo",
            updateDailyPostStatus = true
        ) { result ->
            resultCaptured = result
        }

        assertTrue(resultCaptured!!.isSuccess)
        assertEquals(testPostId, resultCaptured!!.getOrNull())

        verify { mockBatch.set(mockNewPostDoc, any()) }
        verify { mockBatch.set(mockUserDoc, any<Map<String, Any>>(), SetOptions.merge()) }
        verify { mockBatch.commit() }
    }

    @Test
    fun `createPost does not update dailyPostStatus when disabled`() {
        var resultCaptured: Result<String>? = null

        repository.createPost(
            ownerUserId = testOwnerUserId,
            caption = "hello",
            photoUrl = "https://photo",
            updateDailyPostStatus = false
        ) { result ->
            resultCaptured = result
        }

        assertTrue(resultCaptured!!.isSuccess)
        assertEquals(testPostId, resultCaptured!!.getOrNull())

        verify { mockBatch.set(mockNewPostDoc, any()) }
        verify(exactly = 0) { mockBatch.set(mockUserDoc, any<Map<String, Any>>(), SetOptions.merge()) }
        verify { mockBatch.commit() }
    }

    @Test
    fun `getPost returns parsed post with likes`() {
        every { mockExistingPostDoc.get() } returns mockTaskDoc

        every { mockPostDocSnapshot.exists() } returns true
        every { mockPostDocSnapshot.id } returns testPostId
        every { mockPostDocSnapshot.getString("caption") } returns "cap"
        every { mockPostDocSnapshot.getString("ownerUserId") } returns testOwnerUserId
        every { mockPostDocSnapshot.getString("photoUrl") } returns "url"
        every { mockPostDocSnapshot.getTimestamp("createdAt") } returns Timestamp.now()
        every { mockPostDocSnapshot.get("likes") } returns listOf(testLikerUserId, "user_other")

        every { mockTaskDoc.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockPostDocSnapshot)
            mockTaskDoc
        }
        every { mockTaskDoc.addOnFailureListener(any()) } returns mockTaskDoc

        var postCaptured: Post? = null

        repository.getPost(testPostId) { result ->
            postCaptured = result.getOrNull()
        }

        assertNotNull(postCaptured)
        assertEquals(testPostId, postCaptured!!.id)
        assertEquals("cap", postCaptured!!.caption)
        assertEquals(testOwnerUserId, postCaptured!!.ownerUserId)
        assertEquals("url", postCaptured!!.photoUrl)
        assertEquals(2, postCaptured!!.likes.size)
        assertTrue(postCaptured!!.likes.contains(testLikerUserId))
    }

    @Test
    fun `getPost returns empty likes when likes field is null`() {
        every { mockExistingPostDoc.get() } returns mockTaskDoc

        every { mockPostDocSnapshot.exists() } returns true
        every { mockPostDocSnapshot.id } returns testPostId
        every { mockPostDocSnapshot.getString("caption") } returns "cap"
        every { mockPostDocSnapshot.getString("ownerUserId") } returns testOwnerUserId
        every { mockPostDocSnapshot.getString("photoUrl") } returns "url"
        every { mockPostDocSnapshot.getTimestamp("createdAt") } returns Timestamp.now()
        every { mockPostDocSnapshot.get("likes") } returns null

        every { mockTaskDoc.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockPostDocSnapshot)
            mockTaskDoc
        }
        every { mockTaskDoc.addOnFailureListener(any()) } returns mockTaskDoc

        var postCaptured: Post? = null

        repository.getPost(testPostId) { result ->
            postCaptured = result.getOrNull()
        }

        assertNotNull(postCaptured)
        assertTrue(postCaptured!!.likes.isEmpty())
    }

    @Test
    fun `getPostsByUser queries by ownerUserId and parses list`() {
        every { mockPostsCollection.whereEqualTo("ownerUserId", testOwnerUserId) } returns mockQuery
        every { mockQuery.orderBy("createdAt", Query.Direction.DESCENDING) } returns mockQuery
        every { mockQuery.limit(any()) } returns mockQuery
        every { mockQuery.get() } returns mockTaskQuery

        every { mockQueryDocSnapshot.id } returns testPostId
        every { mockQueryDocSnapshot.getString("caption") } returns "cap"
        every { mockQueryDocSnapshot.getString("ownerUserId") } returns testOwnerUserId
        every { mockQueryDocSnapshot.getString("photoUrl") } returns "url"
        every { mockQueryDocSnapshot.getTimestamp("createdAt") } returns Timestamp.now()
        every { mockQueryDocSnapshot.get("likes") } returns listOf(testLikerUserId)

        every { mockQuerySnapshot.documents } returns listOf(mockQueryDocSnapshot)

        every { mockTaskQuery.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mockQuerySnapshot)
            mockTaskQuery
        }
        every { mockTaskQuery.addOnFailureListener(any()) } returns mockTaskQuery

        var postsCaptured: List<Post>? = null

        repository.getPostsByUser(testOwnerUserId, limit = 10) { result ->
            postsCaptured = result.getOrNull()
        }

        assertNotNull(postsCaptured)
        assertEquals(1, postsCaptured!!.size)
        assertEquals(testPostId, postsCaptured!![0].id)
        assertEquals(1, postsCaptured!![0].likes.size)
        assertTrue(postsCaptured!![0].likes.contains(testLikerUserId))

        verify { mockPostsCollection.whereEqualTo("ownerUserId", testOwnerUserId) }
        verify { mockQuery.orderBy("createdAt", Query.Direction.DESCENDING) }
    }

    @Test
    fun `getPostsByUser handles null likes field`() {
        every { mockPostsCollection.whereEqualTo("ownerUserId", testOwnerUserId) } returns mockQuery
        every { mockQuery.orderBy("createdAt", Query.Direction.DESCENDING) } returns mockQuery
        every { mockQuery.limit(any()) } returns mockQuery
        every { mockQuery.get() } returns mockTaskQuery

        every { mockQueryDocSnapshot.id } returns testPostId
        every { mockQueryDocSnapshot.getString("caption") } returns "cap"
        every { mockQueryDocSnapshot.getString("ownerUserId") } returns testOwnerUserId
        every { mockQueryDocSnapshot.getString("photoUrl") } returns "url"
        every { mockQueryDocSnapshot.getTimestamp("createdAt") } returns Timestamp.now()
        every { mockQueryDocSnapshot.get("likes") } returns null

        every { mockQuerySnapshot.documents } returns listOf(mockQueryDocSnapshot)

        every { mockTaskQuery.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mockQuerySnapshot)
            mockTaskQuery
        }
        every { mockTaskQuery.addOnFailureListener(any()) } returns mockTaskQuery

        var postsCaptured: List<Post>? = null

        repository.getPostsByUser(testOwnerUserId, limit = 10) { result ->
            postsCaptured = result.getOrNull()
        }

        assertNotNull(postsCaptured)
        assertEquals(1, postsCaptured!!.size)
        assertTrue(postsCaptured!![0].likes.isEmpty())
    }

    @Test
    fun `likePost adds userId to likes array using arrayUnion`() {
        val mockUpdateTask: Task<Void> = mockk()
        every { mockExistingPostDoc.update("likes", any<FieldValue>()) } returns mockUpdateTask
        every { mockUpdateTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockUpdateTask
        }
        every { mockUpdateTask.addOnFailureListener(any()) } returns mockUpdateTask

        var resultCaptured: Result<Unit>? = null

        repository.likePost(testPostId, testLikerUserId) { resultCaptured = it }

        assertTrue(resultCaptured!!.isSuccess)

        verify { mockPostsCollection.document(testPostId) }
        verify { mockExistingPostDoc.update("likes", any<FieldValue>()) }
    }

    @Test
    fun `unlikePost removes userId from likes array using arrayRemove`() {
        val mockUpdateTask: Task<Void> = mockk()
        every { mockExistingPostDoc.update("likes", any<FieldValue>()) } returns mockUpdateTask
        every { mockUpdateTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<Void>>()
            listener.onSuccess(null)
            mockUpdateTask
        }
        every { mockUpdateTask.addOnFailureListener(any()) } returns mockUpdateTask

        var resultCaptured: Result<Unit>? = null

        repository.unlikePost(testPostId, testLikerUserId) { resultCaptured = it }

        assertTrue(resultCaptured!!.isSuccess)

        verify { mockPostsCollection.document(testPostId) }
        verify { mockExistingPostDoc.update("likes", any<FieldValue>()) }
    }

    @Test
    fun `isPostLikedByUser returns true when userId in likes array`() {
        every { mockExistingPostDoc.get() } returns mockTaskDoc
        every { mockPostDocSnapshot.get("likes") } returns listOf(testLikerUserId, "user_other")

        every { mockTaskDoc.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockPostDocSnapshot)
            mockTaskDoc
        }
        every { mockTaskDoc.addOnFailureListener(any()) } returns mockTaskDoc

        var liked: Boolean? = null

        repository.isPostLikedByUser(testPostId, testLikerUserId) { result ->
            liked = result.getOrNull()
        }

        assertEquals(true, liked)
        verify { mockPostsCollection.document(testPostId) }
        verify { mockExistingPostDoc.get() }
    }

    @Test
    fun `isPostLikedByUser returns false when userId not in likes array`() {
        every { mockExistingPostDoc.get() } returns mockTaskDoc
        every { mockPostDocSnapshot.get("likes") } returns listOf("user_other", "user_another")

        every { mockTaskDoc.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockPostDocSnapshot)
            mockTaskDoc
        }
        every { mockTaskDoc.addOnFailureListener(any()) } returns mockTaskDoc

        var liked: Boolean? = null

        repository.isPostLikedByUser(testPostId, testLikerUserId) { result ->
            liked = result.getOrNull()
        }

        assertEquals(false, liked)
    }

    @Test
    fun `isPostLikedByUser returns false when likes field is null`() {
        every { mockExistingPostDoc.get() } returns mockTaskDoc
        every { mockPostDocSnapshot.get("likes") } returns null

        every { mockTaskDoc.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<DocumentSnapshot>>()
            listener.onSuccess(mockPostDocSnapshot)
            mockTaskDoc
        }
        every { mockTaskDoc.addOnFailureListener(any()) } returns mockTaskDoc

        var liked: Boolean? = null

        repository.isPostLikedByUser(testPostId, testLikerUserId) { result ->
            liked = result.getOrNull()
        }

        assertEquals(false, liked)
    }

    @Test
    fun `addComment creates comment document and returns commentId`() {
        every { mockNewCommentDoc.set(any<Map<String, Any>>()) } returns mockTaskVoid

        var resultCaptured: Result<String>? = null

        repository.addComment(
            postId = testPostId,
            authorUserId = testOwnerUserId,
            text = "nice"
        ) { resultCaptured = it }

        assertTrue(resultCaptured!!.isSuccess)
        assertEquals(testCommentId, resultCaptured!!.getOrNull())

        verify { mockExistingPostDoc.collection("comments") }
        verify { mockCommentsCollection.document() }
        verify { mockNewCommentDoc.set(any<Map<String, Any>>()) }
    }

    @Test
    fun `getComments parses list`() {
        every { mockCommentsCollection.orderBy("createdAt", Query.Direction.ASCENDING) } returns mockQuery
        every { mockQuery.get() } returns mockTaskQuery

        every { mockQueryDocSnapshot.id } returns testCommentId
        every { mockQueryDocSnapshot.getString("authorUserId") } returns testOwnerUserId
        every { mockQueryDocSnapshot.getString("text") } returns "nice"
        every { mockQueryDocSnapshot.getTimestamp("createdAt") } returns Timestamp.now()

        every { mockQuerySnapshot.documents } returns listOf(mockQueryDocSnapshot)

        every { mockTaskQuery.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mockQuerySnapshot)
            mockTaskQuery
        }
        every { mockTaskQuery.addOnFailureListener(any()) } returns mockTaskQuery

        var comments: List<Comment>? = null

        repository.getComments(testPostId) { result ->
            comments = result.getOrNull()
        }

        assertNotNull(comments)
        assertEquals(1, comments!!.size)
        assertEquals(testCommentId, comments!![0].id)
        assertEquals(testPostId, comments!![0].postId)
        assertEquals("nice", comments!![0].text)
    }

    @Test
    fun `getCommentCount returns snapshot size on success`() {
        every { mockCommentsCollection.get() } returns mockTaskQuery
        every { mockQuerySnapshot.size() } returns 2
        every { mockTaskQuery.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mockQuerySnapshot)
            mockTaskQuery
        }
        every { mockTaskQuery.addOnFailureListener(any()) } returns mockTaskQuery

        var count: Int? = null
        repository.getCommentCount(testPostId) { result ->
            count = result.getOrNull()
        }

        assertNotNull(count)
        assertEquals(2, count)
        verify { mockExistingPostDoc.collection("comments") }
        verify { mockCommentsCollection.get() }
    }

    @Test
    fun `getCommentCount returns zero when no comments`() {
        every { mockCommentsCollection.get() } returns mockTaskQuery
        every { mockQuerySnapshot.size() } returns 0
        every { mockQuerySnapshot.documents } returns emptyList()
        every { mockTaskQuery.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mockQuerySnapshot)
            mockTaskQuery
        }
        every { mockTaskQuery.addOnFailureListener(any()) } returns mockTaskQuery

        var count: Int? = null
        repository.getCommentCount(testPostId) { result ->
            count = result.getOrNull()
        }

        assertNotNull(count)
        assertEquals(0, count)
    }

    @Test
    fun `getCommentCount propagates failure`() {
        every { mockCommentsCollection.get() } returns mockTaskQuery
        every { mockTaskQuery.addOnSuccessListener(any()) } returns mockTaskQuery
        every { mockTaskQuery.addOnFailureListener(any()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(Exception("Network error"))
            mockTaskQuery
        }

        var resultCaptured: Result<Int>? = null
        repository.getCommentCount(testPostId) { resultCaptured = it }

        assertTrue(resultCaptured!!.isFailure)
        assertEquals("Network error", resultCaptured!!.exceptionOrNull()?.message)
    }

    @Test
    fun `getFeedPosts excludes current user's own posts`() {
        val currentUserId = "user_current"
        val followedUserId = "user_followed"
        val mockFollowsCollection: CollectionReference = mockk()
        val mockFollowsQuery: QuerySnapshot = mockk()
        val mockFollowDoc: QueryDocumentSnapshot = mockk()
        val mockPostsQuery: Query = mockk()
        val mockPostsQuerySnapshot: QuerySnapshot = mockk()
        val mockPostDoc: QueryDocumentSnapshot = mockk()
        val mockFollowsTask: Task<QuerySnapshot> = mockk()
        val mockPostsTask: Task<QuerySnapshot> = mockk()

        every { mockUsersCollection.document(currentUserId).collection("follows") } returns mockFollowsCollection
        every { mockFollowsCollection.get() } returns mockFollowsTask
        every { mockFollowsQuery.documents } returns listOf(mockFollowDoc)
        every { mockFollowDoc.id } returns followedUserId

        every { mockFollowsTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mockFollowsQuery)
            mockFollowsTask
        }
        every { mockFollowsTask.addOnFailureListener(any()) } returns mockFollowsTask

        every { mockPostsCollection.whereIn("ownerUserId", listOf(followedUserId)) } returns mockPostsQuery
        every { mockPostsQuery.orderBy("createdAt", Query.Direction.DESCENDING) } returns mockPostsQuery
        every { mockPostsQuery.limit(any()) } returns mockPostsQuery
        every { mockPostsQuery.get() } returns mockPostsTask

        every { mockPostDoc.id } returns "post_followed"
        every { mockPostDoc.getString("caption") } returns "Followed post"
        every { mockPostDoc.getString("ownerUserId") } returns followedUserId
        every { mockPostDoc.getString("photoUrl") } returns "url"
        every { mockPostDoc.getTimestamp("createdAt") } returns Timestamp.now()
        every { mockPostDoc.get("likes") } returns emptyList<String>()

        every { mockPostsQuerySnapshot.documents } returns listOf(mockPostDoc)

        every { mockPostsTask.addOnSuccessListener(any()) } answers {
            val listener = firstArg<OnSuccessListener<QuerySnapshot>>()
            listener.onSuccess(mockPostsQuerySnapshot)
            mockPostsTask
        }
        every { mockPostsTask.addOnFailureListener(any()) } returns mockPostsTask

        var postsCaptured: List<Post>? = null

        repository.getFeedPosts(currentUserId, limit = 50) { result ->
            postsCaptured = result.getOrNull()
        }

        assertNotNull(postsCaptured)
        assertEquals(1, postsCaptured!!.size)
        assertEquals(followedUserId, postsCaptured!![0].ownerUserId)
        verify(exactly = 0) { mockPostsCollection.whereIn("ownerUserId", listOf(currentUserId)) }
    }
}
