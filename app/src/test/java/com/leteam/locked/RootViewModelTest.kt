package com.leteam.locked

import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.auth.AuthRepo
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RootViewModelTest {

    private val mockAuthRepo: AuthRepo = mockk(relaxed = true)
    private val mockUserRepo: UserRepository = mockk(relaxed = true)
    private val mockFirebaseUser: FirebaseUser = mockk(relaxed = true)

    private val testUserId = "user_123"

    @Before
    fun setUp() {
        every { mockFirebaseUser.uid } returns testUserId
    }

    @Test
    fun `checkAuthState sets state to Unauthenticated when user is null`() {
        every { mockAuthRepo.getCurrentUser() } returns null

        val viewModel = RootViewModel(mockAuthRepo, mockUserRepo)

        assertEquals(AppState.Unauthenticated, viewModel.appState.value)
    }

    @Test
    fun `checkAuthState sets state to NeedsProfileSetup when displayName is blank`() {
        every { mockAuthRepo.getCurrentUser() } returns mockFirebaseUser

        val mockUser = User(userId = testUserId, displayName = "")
        every { mockUserRepo.getUser(testUserId, any()) } answers {
            val callback = lastArg<(Result<User>) -> Unit>()
            callback(Result.success(mockUser))
        }

        val viewModel = RootViewModel(mockAuthRepo, mockUserRepo)

        assertEquals(AppState.NeedsProfileSetup, viewModel.appState.value)
    }

    @Test
    fun `checkAuthState sets state to Authenticated when displayName is present`() {
        every { mockAuthRepo.getCurrentUser() } returns mockFirebaseUser

        val mockUser = User(userId = testUserId, displayName = "GymBro99")
        every { mockUserRepo.getUser(testUserId, any()) } answers {
            val callback = lastArg<(Result<User>) -> Unit>()
            callback(Result.success(mockUser))
        }

        val viewModel = RootViewModel(mockAuthRepo, mockUserRepo)

        assertEquals(AppState.Authenticated, viewModel.appState.value)
    }

    @Test
    fun `checkAuthState sets state to Unauthenticated when fetching user profile fails`() {
        every { mockAuthRepo.getCurrentUser() } returns mockFirebaseUser

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            val callback = lastArg<(Result<User>) -> Unit>()
            callback(Result.failure(Exception("Network error")))
        }

        val viewModel = RootViewModel(mockAuthRepo, mockUserRepo)

        assertEquals(AppState.Unauthenticated, viewModel.appState.value)
    }
}