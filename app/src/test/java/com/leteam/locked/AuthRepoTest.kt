package com.leteam.locked.auth

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthRepoTest {

    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockTask: Task<AuthResult> = mockk(relaxed = true)
    private val mockAuthResult: AuthResult = mockk(relaxed = true)
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    private lateinit var authRepo: AuthRepo

    @Before
    fun setUp() {
        authRepo = AuthRepo(mockAuth)
    }

    @Test
    fun `signUp calls callback with success when Firebase returns user`() {
        val email = "test@example.com"
        val password = "password123"

        val successSlot = slot<OnSuccessListener<AuthResult>>()

        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockTask
        every { mockTask.addOnSuccessListener(capture(successSlot)) } returns mockTask
        every { mockAuthResult.user } returns mockUser

        var actualResult: Result<FirebaseUser>? = null

        authRepo.signUp(email, password) { result ->
            actualResult = result
        }

        successSlot.captured.onSuccess(mockAuthResult)

        assertTrue(actualResult!!.isSuccess)
        assertEquals(mockUser, actualResult!!.getOrNull())
    }

    @Test
    fun `signUp calls callback with failure when Firebase fails`() {
        val email = "test@example.com"
        val password = "password123"
        val expectedException = Exception("Network error")

        val failureSlot = slot<OnFailureListener>()

        every { mockAuth.createUserWithEmailAndPassword(email, password) } returns mockTask
        every { mockTask.addOnSuccessListener(any()) } returns mockTask
        every { mockTask.addOnFailureListener(capture(failureSlot)) } returns mockTask

        var actualResult: Result<FirebaseUser>? = null

        authRepo.signUp(email, password) { result ->
            actualResult = result
        }

        failureSlot.captured.onFailure(expectedException)

        assertTrue(actualResult!!.isFailure)
        assertEquals(expectedException, actualResult!!.exceptionOrNull())
    }

    @Test
    fun `signUp calls callback with failure when user is null despite success`() {
        val successSlot = slot<OnSuccessListener<AuthResult>>()

        every { mockAuth.createUserWithEmailAndPassword(any(), any()) } returns mockTask
        every { mockTask.addOnSuccessListener(capture(successSlot)) } returns mockTask
        every { mockAuthResult.user } returns null

        var actualResult: Result<FirebaseUser>? = null

        authRepo.signUp("a", "b") { actualResult = it }
        successSlot.captured.onSuccess(mockAuthResult)

        assertTrue(actualResult!!.isFailure)
        assertEquals("User creation failed", actualResult!!.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn calls callback with success`() {
        val email = "test@example.com"
        val password = "password123"
        val successSlot = slot<OnSuccessListener<AuthResult>>()

        every { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask
        every { mockTask.addOnSuccessListener(capture(successSlot)) } returns mockTask
        every { mockAuthResult.user } returns mockUser

        var actualResult: Result<FirebaseUser>? = null

        authRepo.signIn(email, password) { actualResult = it }
        successSlot.captured.onSuccess(mockAuthResult)

        assertTrue(actualResult!!.isSuccess)
        assertEquals(mockUser, actualResult!!.getOrNull())
    }

    @Test
    fun `signOut calls firebase signOut`() {
        authRepo.signOut()

        verify { mockAuth.signOut() }
    }

    @Test
    fun `isLoggedIn returns true when currentUser is not null`() {
        every { mockAuth.currentUser } returns mockUser

        val result = authRepo.isLoggedIn()

        assertTrue(result)
    }

    @Test
    fun `isLoggedIn returns false when currentUser is null`() {
        every { mockAuth.currentUser } returns null

        val result = authRepo.isLoggedIn()

        assertFalse(result)
    }

    @Test
    fun `getCurrentUser returns correct user`() {
        every { mockAuth.currentUser } returns mockUser

        val result = authRepo.getCurrentUser()

        assertEquals(mockUser, result)
    }
}