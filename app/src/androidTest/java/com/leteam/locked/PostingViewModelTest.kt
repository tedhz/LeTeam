package com.leteam.locked.ui.screens.camera

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.photos.PhotoRepository
import com.leteam.locked.posts.PostsRepository
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
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
    lateinit var photoRepository: PhotoRepository

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

        viewModel = PostingViewModel(postsRepository, photoRepository)
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
        val photoUrl = "https://firebasestorage.googleapis.com/photo.jpg"

        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "user_123"

        // Mock photo upload
        coEvery {
            photoRepository.uploadPhoto(
                type = PhotoRepository.PhotoType.POST_PHOTO,
                userId = "user_123",
                imageUri = validUri,
                contentType = "image/jpeg"
            )
        } returns photoUrl

        // Capture the callback and invoke it with success
        val callbackSlot = slot<(Result<String>) -> Unit>()
        every {
            postsRepository.createPost(
                ownerUserId = "user_123",
                caption = "Nice photo",
                photoUrl = photoUrl,
                updateDailyPostStatus = any(),
                onResult = capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.invoke(Result.success("post_123"))
        }

        viewModel.createPost(validUri, "Nice photo")

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PostingUiState.Success, state)
    }

    @Test
    fun createPost_repositoryFailure_updatesStateToError() = runTest {
        val validUri = Uri.parse("file:///sdcard/photo.jpg")
        val photoUrl = "https://firebasestorage.googleapis.com/photo.jpg"
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "user_123"

        // Mock photo upload
        coEvery {
            photoRepository.uploadPhoto(
                type = PhotoRepository.PhotoType.POST_PHOTO,
                userId = "user_123",
                imageUri = validUri,
                contentType = "image/jpeg"
            )
        } returns photoUrl

        // Capture the callback and invoke it with failure
        val errorMsg = "Upload failed"
        val callbackSlot = slot<(Result<String>) -> Unit>()
        every {
            postsRepository.createPost(
                ownerUserId = "user_123",
                caption = "Caption",
                photoUrl = photoUrl,
                updateDailyPostStatus = any(),
                onResult = capture(callbackSlot)
            )
        } answers {
            callbackSlot.captured.invoke(Result.failure(RuntimeException(errorMsg)))
        }

        viewModel.createPost(validUri, "Caption")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be Error", state is PostingUiState.Error)
        assertEquals(errorMsg, (state as PostingUiState.Error).message)
    }
}