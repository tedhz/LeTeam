package com.leteam.locked.ui.screens.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.Post
import com.leteam.locked.posts.PostsRepository
import com.leteam.locked.users.FollowRepository
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import com.leteam.locked.workout.Workout
import com.leteam.locked.workout.WorkoutRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Date

class ProfileViewModelTest {

    private val testUserId = "user_123"
    private val otherUserId = "other_456"

    private val mockUserRepo: UserRepository = mockk(relaxed = true)
    private val mockFollowRepo: FollowRepository = mockk(relaxed = true)
    private val mockPostsRepo: PostsRepository = mockk(relaxed = true)
    private val mockWorkoutRepo: WorkoutRepository = mockk(relaxed = true)
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setUp() {
        mockkObject(FirebaseProvider)
        every { FirebaseProvider.auth } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns testUserId

        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = ProfileViewModel(mockUserRepo, mockFollowRepo, mockPostsRepo, mockWorkoutRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(FirebaseProvider)
    }

    @Test
    fun `loadProfile with null loads own profile and sets user counts posts workouts`() = runTest {
        val user = User(userId = testUserId, fullName = "Me", displayName = "me", email = "me@test.com")
        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockFollowRepo.getFollowerIds(testUserId, any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(listOf("f1")))
        }
        every { mockFollowRepo.getFollowingIds(testUserId, any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(listOf("f2")))
        }
        every { mockPostsRepo.getPostsByUser(testUserId, 500, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockPostsRepo.getPostsByUser(testUserId, 20, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockWorkoutRepo.getWorkouts(testUserId, any()) } answers {
            lastArg<(Result<List<Workout>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        viewModel.loadProfile(null)
        advanceUntilIdle()

        assertEquals(user, viewModel.user.value)
        assertEquals(1, viewModel.followerCount.value)
        assertEquals(1, viewModel.followingCount.value)
        assertEquals(0, viewModel.postCount.value)
        assertNull(viewModel.isFollowing.value)
        assertEquals(emptyList<Post>(), viewModel.recentPosts.value)
        assertEquals(emptyList<Workout>(), viewModel.recentWorkouts.value)
    }

    @Test
    fun `loadProfile with other user id loads their profile and isFollowing`() = runTest {
        val other = User(userId = otherUserId, fullName = "Other", displayName = "other", email = "o@test.com")
        every { mockUserRepo.getUser(otherUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(other))
        }
        every { mockFollowRepo.getFollowerIds(otherUserId, any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockFollowRepo.getFollowingIds(otherUserId, any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockPostsRepo.getPostsByUser(otherUserId, 500, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockPostsRepo.getPostsByUser(otherUserId, 20, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockWorkoutRepo.getWorkouts(otherUserId, any()) } answers {
            lastArg<(Result<List<Workout>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockFollowRepo.isFollowing(testUserId, otherUserId, any()) } answers {
            lastArg<(Result<Boolean>) -> Unit>().invoke(Result.success(false))
        }

        viewModel.loadProfile(otherUserId)
        advanceUntilIdle()

        assertEquals(other, viewModel.user.value)
        assertEquals(false, viewModel.isFollowing.value)
    }

    @Test
    fun `loadProfile populates recentPosts and recentWorkouts`() = runTest {
        val user = User(userId = testUserId, fullName = "Me", displayName = "me", email = "me@test.com")
        val post = Post(id = "p1", caption = "Hi", ownerUserId = testUserId, createdAt = Date())
        val workout = Workout("w1", testUserId, Date(), Date())

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockFollowRepo.getFollowerIds(any(), any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockFollowRepo.getFollowingIds(any(), any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockPostsRepo.getPostsByUser(testUserId, 500, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockPostsRepo.getPostsByUser(testUserId, 20, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockWorkoutRepo.getWorkouts(testUserId, any()) } answers {
            lastArg<(Result<List<Workout>>) -> Unit>().invoke(Result.success(listOf(workout)))
        }

        viewModel.loadProfile(null)
        advanceUntilIdle()

        assertEquals(listOf(post), viewModel.recentPosts.value)
        assertEquals(listOf(workout), viewModel.recentWorkouts.value)
    }

    @Test
    fun `toggleFollow calls follow when not following`() = runTest {
        val other = User(userId = otherUserId, fullName = "Other", displayName = "other", email = "o@test.com")
        every { mockUserRepo.getUser(otherUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(other))
        }
        every { mockFollowRepo.getFollowerIds(any(), any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockFollowRepo.getFollowingIds(any(), any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockPostsRepo.getPostsByUser(any(), any(), any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockWorkoutRepo.getWorkouts(any(), any()) } answers {
            lastArg<(Result<List<Workout>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockFollowRepo.isFollowing(testUserId, otherUserId, any()) } answers {
            lastArg<(Result<Boolean>) -> Unit>().invoke(Result.success(false))
        }
        every { mockFollowRepo.follow(testUserId, otherUserId, any()) } answers {
            lastArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }

        viewModel.loadProfile(otherUserId)
        advanceUntilIdle()

        viewModel.toggleFollow()
        advanceUntilIdle()

        verify { mockFollowRepo.follow(testUserId, otherUserId, any()) }
        assertEquals(true, viewModel.isFollowing.value)
    }

    @Test
    fun `toggleFollow calls unfollow when following`() = runTest {
        val other = User(userId = otherUserId, fullName = "Other", displayName = "other", email = "o@test.com")
        every { mockUserRepo.getUser(otherUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(other))
        }
        every { mockFollowRepo.getFollowerIds(any(), any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockFollowRepo.getFollowingIds(any(), any()) } answers {
            lastArg<(Result<List<String>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockPostsRepo.getPostsByUser(any(), any(), any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockWorkoutRepo.getWorkouts(any(), any()) } answers {
            lastArg<(Result<List<Workout>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        every { mockFollowRepo.isFollowing(testUserId, otherUserId, any()) } answers {
            lastArg<(Result<Boolean>) -> Unit>().invoke(Result.success(true))
        }
        every { mockFollowRepo.unfollow(testUserId, otherUserId, any()) } answers {
            lastArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }

        viewModel.loadProfile(otherUserId)
        advanceUntilIdle()

        viewModel.toggleFollow()
        advanceUntilIdle()

        verify { mockFollowRepo.unfollow(testUserId, otherUserId, any()) }
        assertEquals(false, viewModel.isFollowing.value)
    }

    @Test
    fun `clearFollowError sets followError to null`() = runTest {
        viewModel.clearFollowError()
        assertNull(viewModel.followError.value)
    }

    @Test
    fun `loadProfile does nothing when no current user and profileUserId is null`() = runTest {
        every { mockAuth.currentUser } returns null

        viewModel.loadProfile(null)
        advanceUntilIdle()

        verify(exactly = 0) { mockUserRepo.getUser(any(), any()) }
    }
}
