package com.leteam.locked.ui.screens.camera

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.posts.PostsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PostingViewModelInstrumentedTest {

    @MockK
    lateinit var postsRepository: PostsRepository

    @MockK
    lateinit var mockAuth: FirebaseAuth

    @MockK
    lateinit var mockUser: FirebaseUser

    private lateinit var viewModel: PostingViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        Dispatchers.setMain(testDispatcher)

        mockkObject(FirebaseProvider)
        every { FirebaseProvider.auth } returns mockAuth

        viewModel = PostingViewModel(postsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun createPost_withEmptyUri_updatesStateToError() = runTest {
        val emptyUri = Uri.EMPTY

        viewModel.createPost(emptyUri, "Some caption")

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Error", state is PostingUiState.Error)
        assertEquals("Image is missing", (state as PostingUiState.Error).message)
    }

    @Test
    fun createPost_withNoUser_updatesStateToError() = runTest {
        val validUri = Uri.parse("content://com.leteam.provider/my_image.jpg")

        every { mockAuth.currentUser } returns null

        viewModel.createPost(validUri, "Caption")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Error", state is PostingUiState.Error)
        assertEquals("User not logged in", (state as PostingUiState.Error).message)
    }

    @Test
    fun createPost_success_updatesStateToSuccess() = runTest {
        val validUri = Uri.parse("content://com.leteam.provider/my_image.jpg")

        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "user_123"

        coEvery {
            postsRepository.createPost(any(), any(), any(), any(), any())
        } returns Unit

        viewModel.createPost(validUri, "Nice photo")

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PostingUiState.Success, state)

        coVerify {
            postsRepository.createPost(
                caption = "Nice photo",
                ownerUserId = "user_123",
                photoUrl = validUri.toString(),
                onResult = any()
            )
        }
    }

    @Test
    fun createPost_repositoryFailure_updatesStateToError() = runTest {
        val validUri = Uri.parse("file:///sdcard/photo.jpg")
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "user_123"

        val errorMsg = "Upload failed"
        coEvery {
            postsRepository.createPost(any(), any(), any(), any(), any())
        } throws RuntimeException(errorMsg)

        viewModel.createPost(validUri, "Caption")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Error", state is PostingUiState.Error)
        assertEquals(errorMsg, (state as PostingUiState.Error).message)
    }
}