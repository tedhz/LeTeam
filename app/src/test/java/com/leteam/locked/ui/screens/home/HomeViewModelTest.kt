package com.leteam.locked.ui.screens.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.Post
import com.leteam.locked.posts.PostsRepository
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class HomeViewModelTest {

    private val testUserId = "user_123"
    private val followedUserId = "user_followed"
    private val otherUserId = "user_other"

    private val mockUserRepo: UserRepository = mockk(relaxed = true)
    private val mockPostsRepo: PostsRepository = mockk(relaxed = true)
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        mockkObject(FirebaseProvider)
        every { FirebaseProvider.auth } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns testUserId

        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = HomeViewModel(mockUserRepo, mockPostsRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(FirebaseProvider)
    }

    @Test
    fun `loadHomeData loads current user and sets state`() = runTest {
        val user = User(
            userId = testUserId,
            fullName = "Test User",
            displayName = "testuser",
            email = "test@test.com"
        )

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        viewModel.loadHomeData()
        advanceUntilIdle()

        assertEquals(user, viewModel.currentUser.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadHomeData loads feed posts with user info`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val followedUser = User(userId = followedUserId, fullName = "Followed", displayName = "followed", email = "f@test.com")
        val post = Post(
            id = "post1",
            caption = "Test post",
            ownerUserId = followedUserId,
            photoUrl = "url",
            createdAt = Date()
        )

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(followedUser))
        }

        viewModel.loadHomeData()
        advanceUntilIdle()

        val feedPosts = viewModel.feedPosts.value
        assertEquals(1, feedPosts.size)
        assertEquals(post, feedPosts[0].post)
        assertEquals("Followed", feedPosts[0].ownerFullName)
        assertEquals("@followed", feedPosts[0].ownerDisplayName)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadHomeData handles empty feed posts`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        viewModel.loadHomeData()
        advanceUntilIdle()

        assertTrue(viewModel.feedPosts.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadHomeData handles multiple posts from different users`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val followedUser1 = User(userId = followedUserId, fullName = "User1", displayName = "user1", email = "u1@test.com")
        val followedUser2 = User(userId = otherUserId, fullName = "User2", displayName = "", email = "u2@test.com")

        val post1 = Post(id = "post1", ownerUserId = followedUserId, createdAt = Date())
        val post2 = Post(id = "post2", ownerUserId = otherUserId, createdAt = Date())

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post1, post2)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(followedUser1))
        }
        every { mockUserRepo.getUser(otherUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(followedUser2))
        }

        viewModel.loadHomeData()
        advanceUntilIdle()

        val feedPosts = viewModel.feedPosts.value
        assertEquals(2, feedPosts.size)
        assertEquals("@user1", feedPosts[0].ownerDisplayName)
        assertEquals("u2@test.com", feedPosts[1].ownerDisplayName)
    }

    @Test
    fun `loadHomeData handles user fetch failure gracefully`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val post = Post(id = "post1", ownerUserId = followedUserId, createdAt = Date())

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.failure(Exception("User not found")))
        }

        viewModel.loadHomeData()
        advanceUntilIdle()

        val feedPosts = viewModel.feedPosts.value
        assertEquals(1, feedPosts.size)
        assertEquals("User", feedPosts[0].ownerFullName)
        assertEquals("", feedPosts[0].ownerDisplayName)
    }

    @Test
    fun `loadHomeData handles feed posts fetch failure`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.failure(Exception("Network error")))
        }

        viewModel.loadHomeData()
        advanceUntilIdle()

        assertTrue(viewModel.feedPosts.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadHomeData does nothing when no current user`() = runTest {
        clearMocks(mockUserRepo, mockPostsRepo)
        every { mockAuth.currentUser } returns null

        val testViewModel = HomeViewModel(mockUserRepo, mockPostsRepo)
        advanceUntilIdle()

        verify(exactly = 0) { 
            mockUserRepo.getUser(any(), any())
        }
        verify(exactly = 0) { 
            mockPostsRepo.getFeedPosts(any(), any(), any())
        }
    }

    @Test
    fun `refresh calls loadHomeData`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(user, viewModel.currentUser.value)
    }

    @Test
    fun `currentUserId returns auth current user uid`() {
        assertEquals(testUserId, viewModel.currentUserId)
    }

    @Test
    fun `currentUserId returns null when no auth user`() {
        every { mockAuth.currentUser } returns null
        assertNull(viewModel.currentUserId)
    }
}

