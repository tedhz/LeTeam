package com.leteam.locked.users

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UserRepositoryTest {

    private val mockDb: FirebaseFirestore = mockk(relaxed = true)
    private val mockUsersCollection: CollectionReference = mockk(relaxed = true)
    private val mockUserDoc: DocumentReference = mockk(relaxed = true)

    private val mockVoidTask: Task<Void> = mockk(relaxed = true)
    private val mockGetTask: Task<DocumentSnapshot> = mockk(relaxed = true)

    private val mockSnap: DocumentSnapshot = mockk(relaxed = true)

    private lateinit var repo: UserRepository

    @Before
    fun setUp() {
        every { mockDb.collection("users") } returns mockUsersCollection
        every { mockUsersCollection.document(any()) } returns mockUserDoc

        repo = UserRepository(mockDb)
    }

    @Test
    fun `createUserProfile calls callback with success when set succeeds`() {
        val userId = "u1"
        val email = "test@example.com"

        val successSlot = slot<OnSuccessListener<Void>>()

        every { mockUserDoc.set(any()) } returns mockVoidTask
        every { mockVoidTask.addOnSuccessListener(capture(successSlot)) } returns mockVoidTask

        var actual: Result<Unit>? = null

        repo.createUserProfile(userId, email) { actual = it }
        successSlot.captured.onSuccess(null)

        assertTrue(actual!!.isSuccess)
    }

    @Test
    fun `createUserProfile calls callback with failure when set fails`() {
        val userId = "u1"
        val email = "test@example.com"
        val expectedException = Exception("write failed")

        val failureSlot = slot<OnFailureListener>()

        every { mockUserDoc.set(any()) } returns mockVoidTask
        every { mockVoidTask.addOnSuccessListener(any()) } returns mockVoidTask
        every { mockVoidTask.addOnFailureListener(capture(failureSlot)) } returns mockVoidTask

        var actual: Result<Unit>? = null

        repo.createUserProfile(userId, email) { actual = it }
        failureSlot.captured.onFailure(expectedException)

        assertTrue(actual!!.isFailure)
        assertEquals(expectedException, actual!!.exceptionOrNull())
    }

    @Test
    fun `getUser returns failure when document does not exist`() {
        val userId = "u1"
        val successSlot = slot<OnSuccessListener<DocumentSnapshot>>()

        every { mockUserDoc.get() } returns mockGetTask
        every { mockGetTask.addOnSuccessListener(capture(successSlot)) } returns mockGetTask
        every { mockSnap.exists() } returns false

        var actual: Result<User>? = null

        repo.getUser(userId) { actual = it }
        successSlot.captured.onSuccess(mockSnap)

        assertTrue(actual!!.isFailure)
        assertEquals("User doc not found for $userId", actual!!.exceptionOrNull()?.message)
    }

    @Test
    fun `getUser returns success and maps fields with defaults`() {
        val userId = "u1"
        val successSlot = slot<OnSuccessListener<DocumentSnapshot>>()

        every { mockUserDoc.get() } returns mockGetTask
        every { mockGetTask.addOnSuccessListener(capture(successSlot)) } returns mockGetTask

        every { mockSnap.exists() } returns true
        every { mockSnap.getString("displayName") } returns null
        every { mockSnap.getString("fullName") } returns null
        every { mockSnap.getString("email") } returns "a@b.com"

        every { mockSnap.get("dailyPostStatus") } returns mapOf(
            "hasPostedToday" to true,
            "postId" to "p1"
        )

        every { mockSnap.get("notificationPrefs") } returns mapOf(
            "enabled" to false
        )

        val ts = Timestamp.now()
        every { mockSnap.getTimestamp("createdAt") } returns ts

        var actual: Result<User>? = null

        repo.getUser(userId) { actual = it }
        successSlot.captured.onSuccess(mockSnap)

        assertTrue(actual!!.isSuccess)
        val user = actual!!.getOrNull()!!

        assertEquals(userId, user.userId)
        assertEquals("", user.fullName)
        assertEquals("a@b.com", user.email)
        assertEquals(true, user.dailyPostStatus.hasPostedToday)
        assertEquals("p1", user.dailyPostStatus.postId)
        assertEquals(false, user.notificationPrefs.enabled)
        assertEquals(ts, user.createdAt)
    }

    @Test
    fun `getUser returns failure when get fails`() {
        val userId = "u1"
        val expectedException = Exception("read failed")

        val failureSlot = slot<OnFailureListener>()

        every { mockUserDoc.get() } returns mockGetTask
        every { mockGetTask.addOnSuccessListener(any()) } returns mockGetTask
        every { mockGetTask.addOnFailureListener(capture(failureSlot)) } returns mockGetTask

        var actual: Result<User>? = null

        repo.getUser(userId) { actual = it }
        failureSlot.captured.onFailure(expectedException)

        assertTrue(actual!!.isFailure)
        assertEquals(expectedException, actual!!.exceptionOrNull())
    }

    @Test
    fun `updateProfile returns failure when fullName is blank`() {
        var actual: Result<Unit>? = null

        repo.updateProfile("u1", "   ") { actual = it }

        assertTrue(actual!!.isFailure)
        assertEquals("fullName is required", actual!!.exceptionOrNull()?.message)
    }

    @Test
    fun `updateProfile calls callback with success when update succeeds`() {
        val userId = "u1"
        val successSlot = slot<OnSuccessListener<Void>>()

        every { mockUserDoc.update("fullName", "John") } returns mockVoidTask
        every { mockVoidTask.addOnSuccessListener(capture(successSlot)) } returns mockVoidTask

        var actual: Result<Unit>? = null

        repo.updateProfile(userId, "John") { actual = it }
        successSlot.captured.onSuccess(null)

        assertTrue(actual!!.isSuccess)
    }

    @Test
    fun `updateNotificationEnabled calls callback with success`() {
        val successSlot = slot<OnSuccessListener<Void>>()

        every { mockUserDoc.update("notificationPrefs", any()) } returns mockVoidTask
        every { mockVoidTask.addOnSuccessListener(capture(successSlot)) } returns mockVoidTask

        var actual: Result<Unit>? = null

        repo.updateNotificationEnabled("u1", false) { actual = it }
        successSlot.captured.onSuccess(null)

        assertTrue(actual!!.isSuccess)
    }

    @Test
    fun `updateDailyPostStatus calls callback with success`() {
        val successSlot = slot<OnSuccessListener<Void>>()

        every { mockUserDoc.update("dailyPostStatus", any()) } returns mockVoidTask
        every { mockVoidTask.addOnSuccessListener(capture(successSlot)) } returns mockVoidTask

        var actual: Result<Unit>? = null

        repo.updateDailyPostStatus("u1", true, "p1") { actual = it }
        successSlot.captured.onSuccess(null)

        assertTrue(actual!!.isSuccess)
    }

    @Test
    fun `updateFullName calls update on document`() {
        every { mockUserDoc.update("fullName", "A") } returns mockVoidTask

        repo.updateFullName("u1", "A") {}

        verify { mockUserDoc.update("fullName", "A") }
    }

    @Test
    fun `updateDisplayName calls update on document`() {
        every { mockUserDoc.update("displayName", "B") } returns mockVoidTask

        repo.updateDisplayName("u1", "B") {}

        verify { mockUserDoc.update("displayName", "B") }
    }
}