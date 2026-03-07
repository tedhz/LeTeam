package com.leteam.locked.ui.screens.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.users.UserRepository
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SetupProfileViewModelTest {

    private val mockUserRepo: UserRepository = mockk(relaxed = true)
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    private lateinit var viewModel: SetupProfileViewModel

    private val testUserId = "user_123"

    @Before
    fun setUp() {
        mockkObject(FirebaseProvider)
        every { FirebaseProvider.auth } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns testUserId

        viewModel = SetupProfileViewModel(mockUserRepo)
    }

    @After
    fun tearDown() {
        unmockkObject(FirebaseProvider)
    }

    @Test
    fun `saveDisplayName returns false immediately if user is null`() {
        every { mockAuth.currentUser } returns null

        var resultCaptured: Boolean? = null

        viewModel.saveDisplayName("NewName") { success ->
            resultCaptured = success
        }

        assertTrue(resultCaptured == false)
        verify(exactly = 0) { mockUserRepo.updateDisplayName(any(), any(), any()) }
    }

    @Test
    fun `saveDisplayName returns false immediately if display name is blank`() {
        var resultCaptured: Boolean? = null

        viewModel.saveDisplayName("   ") { success ->
            resultCaptured = success
        }

        assertTrue(resultCaptured == false)
        verify(exactly = 0) { mockUserRepo.updateDisplayName(any(), any(), any()) }
    }

    @Test
    fun `saveDisplayName returns true when repository update succeeds`() {
        val testName = "ValidName"

        every { mockUserRepo.updateDisplayName(testUserId, testName, any()) } answers {
            val callback = lastArg<(Result<Unit>) -> Unit>()
            callback(Result.success(Unit))
        }

        var resultCaptured: Boolean? = null

        viewModel.saveDisplayName(testName) { success ->
            resultCaptured = success
        }

        assertTrue(resultCaptured == true)
        verify { mockUserRepo.updateDisplayName(testUserId, testName, any()) }
    }

    @Test
    fun `saveDisplayName returns false when repository update fails`() {
        val testName = "ValidName"

        every { mockUserRepo.updateDisplayName(testUserId, testName, any()) } answers {
            val callback = lastArg<(Result<Unit>) -> Unit>()
            callback(Result.failure(Exception("Firestore error")))
        }

        var resultCaptured: Boolean? = null

        viewModel.saveDisplayName(testName) { success ->
            resultCaptured = success
        }

        assertTrue(resultCaptured == false)
        verify { mockUserRepo.updateDisplayName(testUserId, testName, any()) }
    }
}