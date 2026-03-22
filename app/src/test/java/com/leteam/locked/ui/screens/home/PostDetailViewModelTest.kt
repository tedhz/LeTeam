package com.leteam.locked.ui.screens.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.Comment
import com.leteam.locked.posts.Post
import com.leteam.locked.posts.PostsRepository
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
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

class PostDetailViewModelTest {

    private val testUserId = "user_123"
    private val ownerUserId = "owner_456"
    private val postId = "post_789"

    private val mockUserRepo: UserRepository = mockk(relaxed = true)
    private val mockPostsRepo: PostsRepository = mockk(relaxed = true)
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    private lateinit var viewModel: PostDetailViewModel

    @Before
    fun setUp() {
        mockkObject(FirebaseProvider)
        every { FirebaseProvider.auth } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns testUserId

        Dispatchers.setMain(UnconfinedTestDispatcher())

        viewModel = PostDetailViewModel(mockUserRepo, mockPostsRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(FirebaseProvider)
    }

    @Test
    fun `loadPost success loads post user and comment count sets postWithUser`() = runTest {
        val post = Post(
            id = postId,
            caption = "Test",
            ownerUserId = ownerUserId,
            photoUrl = "url",
            createdAt = Date(),
            likes = emptyList()
        )
        val owner = User(
            userId = ownerUserId,
            fullName = "Owner Name",
            displayName = "owner",
            email = "owner@test.com"
        )

        every { mockPostsRepo.getPost(postId, any()) } answers {
            lastArg<(Result<Post>) -> Unit>().invoke(Result.success(post))
        }
        every { mockUserRepo.getUser(ownerUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(owner))
        }
        every { mockPostsRepo.getCommentCount(postId, any()) } answers {
            lastArg<(Result<Int>) -> Unit>().invoke(Result.success(5))
        }

        viewModel.loadPost(postId)
        advanceUntilIdle()

        val pw = viewModel.postWithUser.value
        assertTrue(pw != null)
        assertEquals(post, pw!!.post)
        assertEquals("Owner Name", pw.ownerFullName)
        assertEquals("@owner", pw.ownerDisplayName)
        assertEquals(5, pw.commentCount)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadPost when getPost fails sets loading false and postWithUser null`() = runTest {
        every { mockPostsRepo.getPost(postId, any()) } answers {
            lastArg<(Result<Post>) -> Unit>().invoke(Result.failure(Exception("Not found")))
        }

        viewModel.loadPost(postId)
        advanceUntilIdle()

        assertNull(viewModel.postWithUser.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadPost when getUser fails still sets post with default owner name`() = runTest {
        val post = Post(
            id = postId,
            caption = "Test",
            ownerUserId = ownerUserId,
            photoUrl = "url",
            createdAt = Date()
        )

        every { mockPostsRepo.getPost(postId, any()) } answers {
            lastArg<(Result<Post>) -> Unit>().invoke(Result.success(post))
        }
        every { mockUserRepo.getUser(ownerUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.failure(Exception("User not found")))
        }

        viewModel.loadPost(postId)
        advanceUntilIdle()

        val pw = viewModel.postWithUser.value
        assertTrue(pw != null)
        assertEquals(post, pw!!.post)
        assertEquals("User", pw.ownerFullName)
        assertEquals("", pw.ownerDisplayName)
        assertEquals(0, pw.commentCount)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `currentUserId returns auth uid when logged in`() {
        assertEquals(testUserId, viewModel.currentUserId)
    }

    @Test
    fun `currentUserId returns null when no auth user`() {
        every { mockAuth.currentUser } returns null
        assertNull(viewModel.currentUserId)
    }

    private fun stubLoadPostSuccess(likes: List<String> = emptyList()) {
        val post = Post(
            id = postId,
            caption = "Test",
            ownerUserId = ownerUserId,
            photoUrl = "url",
            createdAt = Date(),
            likes = likes
        )
        val owner = User(userId = ownerUserId, fullName = "Owner", displayName = "owner", email = "o@test.com")
        every { mockPostsRepo.getPost(postId, any()) } answers {
            lastArg<(Result<Post>) -> Unit>().invoke(Result.success(post))
        }
        every { mockUserRepo.getUser(ownerUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(owner))
        }
        every { mockPostsRepo.getCommentCount(postId, any()) } answers {
            lastArg<(Result<Int>) -> Unit>().invoke(Result.success(0))
        }
    }

    @Test
    fun `toggleLike calls likePost when post not liked and updates local state`() = runTest {
        stubLoadPostSuccess()
        viewModel.loadPost(postId)
        advanceUntilIdle()

        every { mockPostsRepo.likePost(postId, testUserId, any()) } answers {
            lastArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }

        viewModel.toggleLike(postId)
        advanceUntilIdle()

        verify { mockPostsRepo.likePost(postId, testUserId, any()) }
        verify(exactly = 0) { mockPostsRepo.unlikePost(any(), any(), any()) }
        assertTrue(viewModel.postWithUser.value!!.post.likes.contains(testUserId))
    }

    @Test
    fun `toggleLike calls unlikePost when post already liked and updates local state`() = runTest {
        val post = Post(
            id = postId,
            caption = "Test",
            ownerUserId = ownerUserId,
            photoUrl = "url",
            createdAt = Date(),
            likes = listOf(testUserId)
        )
        val owner = User(userId = ownerUserId, fullName = "Owner", displayName = "owner", email = "o@test.com")
        every { mockPostsRepo.getPost(postId, any()) } answers {
            lastArg<(Result<Post>) -> Unit>().invoke(Result.success(post))
        }
        every { mockUserRepo.getUser(ownerUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(owner))
        }
        every { mockPostsRepo.getCommentCount(postId, any()) } answers {
            lastArg<(Result<Int>) -> Unit>().invoke(Result.success(0))
        }
        viewModel.loadPost(postId)
        advanceUntilIdle()

        every { mockPostsRepo.unlikePost(postId, testUserId, any()) } answers {
            lastArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }

        viewModel.toggleLike(postId)
        advanceUntilIdle()

        verify { mockPostsRepo.unlikePost(postId, testUserId, any()) }
        verify(exactly = 0) { mockPostsRepo.likePost(any(), any(), any()) }
        assertFalse(viewModel.postWithUser.value!!.post.likes.contains(testUserId))
    }

    @Test
    fun `toggleLike does nothing when no current user`() = runTest {
        every { mockAuth.currentUser } returns null
        val testVm = PostDetailViewModel(mockUserRepo, mockPostsRepo)
        stubLoadPostSuccess()
        testVm.loadPost(postId)
        advanceUntilIdle()

        testVm.toggleLike(postId)
        advanceUntilIdle()

        verify(exactly = 0) { mockPostsRepo.likePost(any(), any(), any()) }
        verify(exactly = 0) { mockPostsRepo.unlikePost(any(), any(), any()) }
    }

    @Test
    fun `toggleLike does nothing when no post loaded`() = runTest {
        viewModel.toggleLike(postId)
        advanceUntilIdle()

        verify(exactly = 0) { mockPostsRepo.likePost(any(), any(), any()) }
        verify(exactly = 0) { mockPostsRepo.unlikePost(any(), any(), any()) }
    }

    @Test
    fun `openCommentsDrawer sets drawer open and calls getComments when post loaded`() = runTest {
        stubLoadPostSuccess()
        viewModel.loadPost(postId)
        advanceUntilIdle()

        every { mockPostsRepo.getComments(postId, any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        viewModel.openCommentsDrawer()
        advanceUntilIdle()

        assertTrue(viewModel.commentsDrawerOpen.value)
        assertTrue(viewModel.comments.value.isEmpty())
        assertFalse(viewModel.commentsLoading.value)
        verify { mockPostsRepo.getComments(postId, any()) }
    }

    @Test
    fun `closeCommentsDrawer clears drawer and comments`() = runTest {
        stubLoadPostSuccess()
        viewModel.loadPost(postId)
        advanceUntilIdle()
        every { mockPostsRepo.getComments(postId, any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        viewModel.openCommentsDrawer()
        advanceUntilIdle()

        viewModel.closeCommentsDrawer()

        assertFalse(viewModel.commentsDrawerOpen.value)
        assertTrue(viewModel.comments.value.isEmpty())
    }

    @Test
    fun `loadComments with comments sets CommentWithAuthor and updateCommentCount`() = runTest {
        stubLoadPostSuccess()
        viewModel.loadPost(postId)
        advanceUntilIdle()

        val comment = Comment(
            id = "c1",
            postId = postId,
            authorUserId = "author1",
            text = "Nice!",
            createdAt = Date()
        )
        val authorUser = User(userId = "author1", fullName = "Author Full", displayName = "author", email = "a@test.com")
        every { mockPostsRepo.getComments(postId, any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(listOf(comment)))
        }
        every { mockUserRepo.getUser("author1", any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(authorUser))
        }

        viewModel.openCommentsDrawer()
        advanceUntilIdle()

        val comments = viewModel.comments.value
        assertEquals(1, comments.size)
        assertEquals("Nice!", comments[0].comment.text)
        assertEquals("author", comments[0].authorDisplayName)
        assertEquals("Author Full", comments[0].authorFullName)
        assertEquals(1, viewModel.postWithUser.value!!.commentCount)
    }

    @Test
    fun `addComment does nothing when text is blank`() = runTest {
        stubLoadPostSuccess()
        viewModel.loadPost(postId)
        advanceUntilIdle()

        viewModel.addComment(postId, "")
        advanceUntilIdle()
        viewModel.addComment(postId, "   ")
        advanceUntilIdle()

        verify(exactly = 0) { mockPostsRepo.addComment(any(), any(), any(), any()) }
    }

    @Test
    fun `addComment does nothing when no current user`() = runTest {
        every { mockAuth.currentUser } returns null
        val testVm = PostDetailViewModel(mockUserRepo, mockPostsRepo)
        stubLoadPostSuccess()
        testVm.loadPost(postId)
        advanceUntilIdle()

        testVm.addComment(postId, "hello")
        advanceUntilIdle()

        verify(exactly = 0) { mockPostsRepo.addComment(any(), any(), any(), any()) }
    }

    @Test
    fun `addComment calls repository and loadComments on success`() = runTest {
        stubLoadPostSuccess()
        viewModel.loadPost(postId)
        advanceUntilIdle()

        every { mockPostsRepo.addComment(postId, testUserId, "hello", any()) } answers {
            lastArg<(Result<String>) -> Unit>().invoke(Result.success("comment_1"))
        }
        every { mockPostsRepo.getComments(postId, any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(emptyList()))
        }

        var onDoneCalled = false
        viewModel.addComment(postId, "hello") { onDoneCalled = true }
        advanceUntilIdle()

        verify { mockPostsRepo.addComment(postId, testUserId, "hello", any()) }
        verify(atLeast = 1) { mockPostsRepo.getComments(postId, any()) }
        assertTrue(onDoneCalled)
    }

    @Test
    fun `openLikesDrawer sets drawer open and loads like users`() = runTest {
        val likerId = "liker_1"
        val liker = User(userId = likerId, fullName = "Liker Name", displayName = "liker", email = "l@test.com")
        stubLoadPostSuccess(likes = listOf(likerId))
        viewModel.loadPost(postId)
        advanceUntilIdle()

        every { mockUserRepo.getUser(likerId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(liker))
        }

        viewModel.openLikesDrawer()
        advanceUntilIdle()

        assertTrue(viewModel.likesDrawerOpen.value)
        assertEquals(1, viewModel.likeUsers.value.size)
        assertEquals("Liker Name", viewModel.likeUsers.value[0].fullName)
        assertFalse(viewModel.likesListLoading.value)
        verify { mockUserRepo.getUser(likerId, any()) }
    }

    @Test
    fun `openLikesDrawer with empty likes keeps likeUsers empty`() = runTest {
        stubLoadPostSuccess(likes = emptyList())
        viewModel.loadPost(postId)
        advanceUntilIdle()

        viewModel.openLikesDrawer()
        advanceUntilIdle()

        assertTrue(viewModel.likesDrawerOpen.value)
        assertTrue(viewModel.likeUsers.value.isEmpty())
        assertFalse(viewModel.likesListLoading.value)
    }

    @Test
    fun `openCommentsDrawer closes likes drawer`() = runTest {
        val likerId = "liker_1"
        val liker = User(userId = likerId, fullName = "Liker", displayName = "liker", email = "l@test.com")
        stubLoadPostSuccess(likes = listOf(likerId))
        viewModel.loadPost(postId)
        advanceUntilIdle()
        every { mockUserRepo.getUser(likerId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(liker))
        }
        viewModel.openLikesDrawer()
        advanceUntilIdle()
        assertTrue(viewModel.likesDrawerOpen.value)

        every { mockPostsRepo.getComments(postId, any()) } answers {
            lastArg<(Result<List<Comment>>) -> Unit>().invoke(Result.success(emptyList()))
        }
        viewModel.openCommentsDrawer()
        advanceUntilIdle()

        assertFalse(viewModel.likesDrawerOpen.value)
        assertTrue(viewModel.commentsDrawerOpen.value)
    }

    @Test
    fun `closeLikesDrawer clears likes state`() = runTest {
        val likerId = "liker_1"
        val liker = User(userId = likerId, fullName = "Liker", displayName = "liker", email = "l@test.com")
        stubLoadPostSuccess(likes = listOf(likerId))
        viewModel.loadPost(postId)
        advanceUntilIdle()
        every { mockUserRepo.getUser(likerId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(liker))
        }
        viewModel.openLikesDrawer()
        advanceUntilIdle()

        viewModel.closeLikesDrawer()

        assertFalse(viewModel.likesDrawerOpen.value)
        assertTrue(viewModel.likeUsers.value.isEmpty())
    }
}
