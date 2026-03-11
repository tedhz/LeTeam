package com.leteam.locked.ui.screens.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.Comment
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

    private fun stubGetCommentCount(count: Int = 0) {
        every { mockPostsRepo.getCommentCount(any(), any()) } answers {
            lastArg<(Result<Int>) -> Unit>().invoke(Result.success(count))
        }
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
        stubGetCommentCount(0)

        viewModel.loadHomeData()
        advanceUntilIdle()

        val feedPosts = viewModel.feedPosts.value
        assertEquals(1, feedPosts.size)
        assertEquals(post, feedPosts[0].post)
        assertEquals("Followed", feedPosts[0].ownerFullName)
        assertEquals("@followed", feedPosts[0].ownerDisplayName)
        assertEquals(0, feedPosts[0].commentCount)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadHomeData requests getCommentCount and updates commentCount`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val followedUser = User(userId = followedUserId, fullName = "Followed", displayName = "followed", email = "f@test.com")
        val post = Post(id = "post1", caption = "Test", ownerUserId = followedUserId, photoUrl = "url", createdAt = Date())

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(followedUser))
        }
        stubGetCommentCount(3)

        viewModel.loadHomeData()
        advanceUntilIdle()

        val feedPosts = viewModel.feedPosts.value
        assertEquals(1, feedPosts.size)
        assertEquals(3, feedPosts[0].commentCount)
        verify { mockPostsRepo.getCommentCount("post1", any()) }
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
        stubGetCommentCount(0)

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
        stubGetCommentCount(0)

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

    @Test
    fun `toggleLike calls likePost when post is not liked`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val post = Post(
            id = "post1",
            caption = "Test post",
            ownerUserId = followedUserId,
            photoUrl = "url",
            createdAt = Date(),
            likes = emptyList()
        )

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(User(userId = followedUserId, fullName = "Followed", displayName = "followed", email = "f@test.com")))
        }
        stubGetCommentCount(0)

        viewModel.loadHomeData()
        advanceUntilIdle()

        every { mockPostsRepo.likePost("post1", testUserId, any()) } answers {
            val callback = lastArg<(Result<Unit>) -> Unit>()
            callback(Result.success(Unit))
        }

        viewModel.toggleLike("post1")
        advanceUntilIdle()

        verify { mockPostsRepo.likePost("post1", testUserId, any()) }
        verify(exactly = 0) { mockPostsRepo.unlikePost(any(), any(), any()) }

        val updatedPosts = viewModel.feedPosts.value
        assertEquals(1, updatedPosts.size)
        assertTrue(updatedPosts[0].post.likes.contains(testUserId))
    }

    @Test
    fun `toggleLike calls unlikePost when post is already liked`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val post = Post(
            id = "post1",
            caption = "Test post",
            ownerUserId = followedUserId,
            photoUrl = "url",
            createdAt = Date(),
            likes = listOf(testUserId)
        )

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(User(userId = followedUserId, fullName = "Followed", displayName = "followed", email = "f@test.com")))
        }

        viewModel.loadHomeData()
        advanceUntilIdle()

        every { mockPostsRepo.unlikePost("post1", testUserId, any()) } answers {
            val callback = lastArg<(Result<Unit>) -> Unit>()
            callback(Result.success(Unit))
        }

        viewModel.toggleLike("post1")
        advanceUntilIdle()

        verify { mockPostsRepo.unlikePost("post1", testUserId, any()) }
        verify(exactly = 0) { mockPostsRepo.likePost(any(), any(), any()) }

        val updatedPosts = viewModel.feedPosts.value
        assertEquals(1, updatedPosts.size)
        assertFalse(updatedPosts[0].post.likes.contains(testUserId))
    }

    @Test
    fun `toggleLike updates local state optimistically when unliking`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val post = Post(
            id = "post1",
            caption = "Test post",
            ownerUserId = followedUserId,
            photoUrl = "url",
            createdAt = Date(),
            likes = listOf(testUserId, "user_other")
        )

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(User(userId = followedUserId, fullName = "Followed", displayName = "followed", email = "f@test.com")))
        }
        stubGetCommentCount(0)

        viewModel.loadHomeData()
        advanceUntilIdle()

        every { mockPostsRepo.unlikePost("post1", testUserId, any()) } answers {
            val callback = lastArg<(Result<Unit>) -> Unit>()
            callback(Result.success(Unit))
        }

        viewModel.toggleLike("post1")
        advanceUntilIdle()

        val updatedPosts = viewModel.feedPosts.value
        assertEquals(1, updatedPosts.size)
        assertFalse(updatedPosts[0].post.likes.contains(testUserId))
        assertEquals(1, updatedPosts[0].post.likes.size)
        assertTrue(updatedPosts[0].post.likes.contains("user_other"))
    }

    @Test
    fun `toggleLike does nothing when no current user`() = runTest {
        every { mockAuth.currentUser } returns null

        viewModel.toggleLike("post1")
        advanceUntilIdle()

        verify(exactly = 0) { mockPostsRepo.likePost(any(), any(), any()) }
        verify(exactly = 0) { mockPostsRepo.unlikePost(any(), any(), any()) }
    }

    @Test
    fun `toggleLike does nothing when post not found in feed`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")

        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        viewModel.loadHomeData()
        advanceUntilIdle()

        viewModel.toggleLike("post1")
        advanceUntilIdle()

        verify(exactly = 0) { mockPostsRepo.likePost(any(), any(), any()) }
        verify(exactly = 0) { mockPostsRepo.unlikePost(any(), any(), any()) }
    }

    @Test
    fun `openCommentsDrawer sets drawer post id and calls loadComments`() = runTest {
        every { mockPostsRepo.getComments("post1", any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        viewModel.openCommentsDrawer("post1")
        advanceUntilIdle()

        assertEquals("post1", viewModel.commentsDrawerPostId.value)
        assertTrue(viewModel.comments.value.isEmpty())
        assertFalse(viewModel.commentsLoading.value)
        verify { mockPostsRepo.getComments("post1", any()) }
    }

    @Test
    fun `closeCommentsDrawer clears drawer state and comments`() = runTest {
        every { mockPostsRepo.getComments("post1", any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        viewModel.openCommentsDrawer("post1")
        advanceUntilIdle()

        viewModel.closeCommentsDrawer()

        assertNull(viewModel.commentsDrawerPostId.value)
        assertTrue(viewModel.comments.value.isEmpty())
    }

    @Test
    fun `loadComments with empty list sets comments empty and updateCommentCount zero`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val post = Post(id = "post1", ownerUserId = followedUserId, photoUrl = "url", createdAt = Date())
        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(User(userId = followedUserId, fullName = "F", displayName = "f", email = "f@test.com")))
        }
        stubGetCommentCount(0)
        viewModel.loadHomeData()
        advanceUntilIdle()

        every { mockPostsRepo.getComments("post1", any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        viewModel.openCommentsDrawer("post1")
        advanceUntilIdle()

        assertTrue(viewModel.comments.value.isEmpty())
        assertEquals(0, viewModel.feedPosts.value.single { it.post.id == "post1" }.commentCount)
    }

    @Test
    fun `loadComments with comments loads author info and sets CommentWithAuthor and updateCommentCount`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val authorUser = User(userId = "author1", fullName = "Author Name", displayName = "author", email = "a@test.com")
        val post = Post(id = "post1", ownerUserId = followedUserId, photoUrl = "url", createdAt = Date())
        val comment = Comment(id = "c1", postId = "post1", authorUserId = "author1", text = "Nice!", createdAt = Date())
        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(User(userId = followedUserId, fullName = "F", displayName = "f", email = "f@test.com")))
        }
        stubGetCommentCount(0)
        viewModel.loadHomeData()
        advanceUntilIdle()

        every { mockPostsRepo.getComments("post1", any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(listOf(comment)))
        }
        every { mockUserRepo.getUser("author1", any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(authorUser))
        }
        viewModel.openCommentsDrawer("post1")
        advanceUntilIdle()

        val comments = viewModel.comments.value
        assertEquals(1, comments.size)
        assertEquals("Nice!", comments[0].comment.text)
        assertEquals("author", comments[0].authorDisplayName)
        assertEquals("Author Name", comments[0].authorFullName)
        assertEquals(1, viewModel.feedPosts.value.single { it.post.id == "post1" }.commentCount)
    }

    @Test
    fun `updateCommentCount updates correct post commentCount`() = runTest {
        val user = User(userId = testUserId, fullName = "Test", displayName = "test", email = "test@test.com")
        val post = Post(id = "post1", ownerUserId = followedUserId, photoUrl = "url", createdAt = Date())
        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(user))
        }
        every { mockPostsRepo.getFeedPosts(testUserId, 50, any()) } answers {
            lastArg<(Result<List<Post>>) -> Unit>().invoke(Result.success(listOf(post)))
        }
        every { mockUserRepo.getUser(followedUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(User(userId = followedUserId, fullName = "F", displayName = "f", email = "f@test.com")))
        }
        stubGetCommentCount(0)
        viewModel.loadHomeData()
        advanceUntilIdle()

        viewModel.updateCommentCount("post1", 5)

        assertEquals(5, viewModel.feedPosts.value.single { it.post.id == "post1" }.commentCount)
    }

    @Test
    fun `addComment does nothing when text is blank`() = runTest {
        viewModel.addComment("post1", "")
        advanceUntilIdle()
        viewModel.addComment("post1", "   ")
        advanceUntilIdle()

        verify(exactly = 0) { mockPostsRepo.addComment(any(), any(), any(), any()) }
    }

    @Test
    fun `addComment does nothing when no current user`() = runTest {
        every { mockAuth.currentUser } returns null
        val testViewModel = HomeViewModel(mockUserRepo, mockPostsRepo)

        testViewModel.addComment("post1", "hello")
        advanceUntilIdle()

        verify(exactly = 0) { mockPostsRepo.addComment(any(), any(), any(), any()) }
    }

    @Test
    fun `addComment calls repository and then loadComments on success`() = runTest {
        every { mockPostsRepo.addComment("post1", testUserId, "hello", any()) } answers {
            lastArg<(Result<String>) -> Unit>().invoke(Result.success("comment_1"))
        }
        every { mockPostsRepo.getComments("post1", any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        var onDoneCalled = false
        viewModel.addComment("post1", "hello") { onDoneCalled = true }
        advanceUntilIdle()

        verify { mockPostsRepo.addComment("post1", testUserId, "hello", any()) }
        verify { mockPostsRepo.getComments("post1", any()) }
        assertTrue(onDoneCalled)
    }
}

