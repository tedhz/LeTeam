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
    fun `getPost returns parsed post`() {
        every { mockExistingPostDoc.get() } returns mockTaskDoc

        every { mockPostDocSnapshot.exists() } returns true
        every { mockPostDocSnapshot.id } returns testPostId
        every { mockPostDocSnapshot.getString("caption") } returns "cap"
        every { mockPostDocSnapshot.getString("ownerUserId") } returns testOwnerUserId
        every { mockPostDocSnapshot.getString("photoUrl") } returns "url"
        every { mockPostDocSnapshot.getTimestamp("createdAt") } returns Timestamp.now()

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

        verify { mockPostsCollection.whereEqualTo("ownerUserId", testOwnerUserId) }
        verify { mockQuery.orderBy("createdAt", Query.Direction.DESCENDING) }
    }

    @Test
    fun `likePost writes liker doc under likes subcollection`() {
        every { mockLikeDoc.set(any<Map<String, Any>>()) } returns mockTaskVoid

        var resultCaptured: Result<Unit>? = null

        repository.likePost(testPostId, testLikerUserId) { resultCaptured = it }

        assertTrue(resultCaptured!!.isSuccess)

        verify { mockPostsCollection.document(testPostId) }
        verify { mockExistingPostDoc.collection("likes") }
        verify { mockLikesCollection.document(testLikerUserId) }
        verify { mockLikeDoc.set(any<Map<String, Any>>()) }
    }

    @Test
    fun `unlikePost deletes liker doc under likes subcollection`() {
        every { mockLikeDoc.delete() } returns mockTaskVoid

        var resultCaptured: Result<Unit>? = null

        repository.unlikePost(testPostId, testLikerUserId) { resultCaptured = it }

        assertTrue(resultCaptured!!.isSuccess)

        verify { mockLikeDoc.delete() }
    }

    @Test
    fun `isPostLikedByUser returns true when like doc exists`() {
        every { mockLikeDoc.get() } returns mockTaskDoc
        every { mockPostDocSnapshot.exists() } returns true

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
}
