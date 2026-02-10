package com.leteam.locked.users

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FollowRepositoryTest {

    private val mockDb: FirebaseFirestore = mockk(relaxed = true)

    private val mockUsersCollection: CollectionReference = mockk(relaxed = true)
    private val mockMeDoc: DocumentReference = mockk(relaxed = true)
    private val mockTargetDoc: DocumentReference = mockk(relaxed = true)

    private val mockFollowsCollection: CollectionReference = mockk(relaxed = true)
    private val mockFollowersCollection: CollectionReference = mockk(relaxed = true)

    private val mockFollowsDoc: DocumentReference = mockk(relaxed = true)
    private val mockFollowersDoc: DocumentReference = mockk(relaxed = true)

    private val mockBatch: WriteBatch = mockk(relaxed = true)
    private val mockCommitTask: Task<Void> = mockk(relaxed = true)

    private val mockGetDocTask: Task<DocumentSnapshot> = mockk(relaxed = true)
    private val mockDocSnap: DocumentSnapshot = mockk(relaxed = true)

    private val mockGetQueryTask: Task<QuerySnapshot> = mockk(relaxed = true)
    private val mockQuerySnap: QuerySnapshot = mockk(relaxed = true)

    private lateinit var repo: FollowRepository

    @Before
    fun setUp() {
        every { mockDb.collection("users") } returns mockUsersCollection

        every { mockUsersCollection.document("me") } returns mockMeDoc
        every { mockUsersCollection.document("target") } returns mockTargetDoc

        every { mockMeDoc.collection("follows") } returns mockFollowsCollection
        every { mockTargetDoc.collection("followers") } returns mockFollowersCollection

        every { mockFollowsCollection.document("target") } returns mockFollowsDoc
        every { mockFollowersCollection.document("me") } returns mockFollowersDoc

        every { mockDb.batch() } returns mockBatch
        every { mockBatch.commit() } returns mockCommitTask

        repo = FollowRepository(mockDb)
    }

    @Test
    fun `follow calls callback with success when batch commit succeeds`() {
        val successSlot = slot<OnSuccessListener<Void>>()

        every { mockCommitTask.addOnSuccessListener(capture(successSlot)) } returns mockCommitTask

        var actual: Result<Unit>? = null

        repo.follow("me", "target") { actual = it }
        successSlot.captured.onSuccess(null)

        assertTrue(actual!!.isSuccess)

        verify { mockBatch.set(mockFollowsDoc, any()) }
        verify { mockBatch.set(mockFollowersDoc, any()) }
        verify { mockBatch.commit() }
    }

    @Test
    fun `unfollow calls callback with success when batch commit succeeds`() {
        val successSlot = slot<OnSuccessListener<Void>>()

        every { mockCommitTask.addOnSuccessListener(capture(successSlot)) } returns mockCommitTask

        var actual: Result<Unit>? = null

        repo.unfollow("me", "target") { actual = it }
        successSlot.captured.onSuccess(null)

        assertTrue(actual!!.isSuccess)

        verify { mockBatch.delete(mockFollowsDoc) }
        verify { mockBatch.delete(mockFollowersDoc) }
        verify { mockBatch.commit() }
    }

    @Test
    fun `isFollowing returns true when follows doc exists`() {
        val successSlot = slot<OnSuccessListener<DocumentSnapshot>>()

        every { mockFollowsDoc.get() } returns mockGetDocTask
        every { mockGetDocTask.addOnSuccessListener(capture(successSlot)) } returns mockGetDocTask
        every { mockDocSnap.exists() } returns true

        var actual: Result<Boolean>? = null

        repo.isFollowing("me", "target") { actual = it }
        successSlot.captured.onSuccess(mockDocSnap)

        assertTrue(actual!!.isSuccess)
        assertEquals(true, actual!!.getOrNull())
    }

    @Test
    fun `getFollowingIds returns list of document ids`() {
        val successSlot = slot<OnSuccessListener<QuerySnapshot>>()

        val doc1: DocumentSnapshot = mockk(relaxed = true)
        val doc2: DocumentSnapshot = mockk(relaxed = true)
        every { doc1.id } returns "u2"
        every { doc2.id } returns "u3"

        every { mockFollowsCollection.get() } returns mockGetQueryTask
        every { mockGetQueryTask.addOnSuccessListener(capture(successSlot)) } returns mockGetQueryTask
        every { mockQuerySnap.documents } returns listOf(doc1, doc2)

        var actual: Result<List<String>>? = null

        repo.getFollowingIds("me") { actual = it }
        successSlot.captured.onSuccess(mockQuerySnap)

        assertTrue(actual!!.isSuccess)
        assertEquals(listOf("u2", "u3"), actual!!.getOrNull())
    }

    @Test
    fun `getFollowerIds returns list of document ids`() {
        val successSlot = slot<OnSuccessListener<QuerySnapshot>>()

        val queryTask: Task<QuerySnapshot> = mockk(relaxed = true)   // dedicated task
        val querySnap: QuerySnapshot = mockk(relaxed = true)

        val doc1: DocumentSnapshot = mockk(relaxed = true)
        every { doc1.id } returns "u9"

        every { querySnap.documents } returns listOf(doc1)

        // IMPORTANT: stub the exact call chain for THIS test
        every { mockTargetDoc.collection("followers") } returns mockFollowersCollection
        every { mockFollowersCollection.get() } returns queryTask
        every { queryTask.addOnSuccessListener(capture(successSlot)) } returns queryTask
        every { queryTask.addOnFailureListener(any()) } returns queryTask

        var actual: Result<List<String>>? = null

        repo.getFollowerIds("target") { actual = it }

        // Fire the captured listener
        successSlot.captured.onSuccess(querySnap)

        assertNotNull(actual)
        assertTrue(actual!!.isSuccess)
        assertEquals(listOf("u9"), actual!!.getOrNull())
    }

    @Test
    fun `follow calls callback with failure when batch commit fails`() {
        val expectedException = Exception("commit failed")
        val failureSlot = slot<OnFailureListener>()

        every { mockCommitTask.addOnSuccessListener(any()) } returns mockCommitTask
        every { mockCommitTask.addOnFailureListener(capture(failureSlot)) } returns mockCommitTask

        var actual: Result<Unit>? = null

        repo.follow("me", "target") { actual = it }
        failureSlot.captured.onFailure(expectedException)

        assertTrue(actual!!.isFailure)
        assertEquals(expectedException, actual!!.exceptionOrNull())
    }
}