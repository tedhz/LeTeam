package com.leteam.locked.ui.screens.profile

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.photos.PhotoRepository
import com.leteam.locked.users.User
import com.leteam.locked.users.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditProfileViewModelTest {

    private val testUserId = "user_123"

    private val mockUserRepo: UserRepository = mockk(relaxed = true)
    private val mockPhotoRepo: PhotoRepository = mockk(relaxed = true)
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockUser: FirebaseUser = mockk(relaxed = true)

    private lateinit var viewModel: EditProfileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkObject(FirebaseProvider)
        every { FirebaseProvider.auth } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns testUserId

        viewModel = EditProfileViewModel(mockUserRepo, mockPhotoRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(FirebaseProvider)
    }

    @Test
    fun `load populates state from repository`() = runTest {
        val u = User(
            userId = testUserId,
            fullName = "Anirudh",
            displayName = "anirudh",
            bio = "Hello",
            photoUrl = "https://x/y.jpg",
            email = "a@b.com"
        )
        every { mockUserRepo.getUser(testUserId, any()) } answers {
            lastArg<(Result<User>) -> Unit>().invoke(Result.success(u))
        }

        viewModel.load()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Anirudh", state.fullName)
        assertEquals("anirudh", state.handle)
        assertEquals("Hello", state.bio)
        assertEquals("https://x/y.jpg", state.photoUrl)
    }

    @Test
    fun `save rejects invalid handle`() = runTest {
        viewModel.setFullName("Name")
        viewModel.setHandle("!!bad!!")

        viewModel.save(onSaved = {})

        assertTrue(viewModel.uiState.value.handleError != null)
        verify(exactly = 0) { mockUserRepo.isDisplayNameAvailable(any(), any(), any()) }
    }

    @Test
    fun `save checks availability then updates profile fields`() = runTest {
        viewModel.setFullName("Name")
        viewModel.setHandle("@unique_name")
        viewModel.setBio("Bio")

        every { mockUserRepo.isDisplayNameAvailable("unique_name", testUserId, any()) } answers {
            lastArg<(Result<Boolean>) -> Unit>().invoke(Result.success(true))
        }

        val fieldsSlot = slot<Map<String, Any>>()
        every { mockUserRepo.updateProfileFields(testUserId, capture(fieldsSlot), any()) } answers {
            lastArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }

        var saved = false
        viewModel.save(onSaved = { saved = true })

        assertTrue(saved)
        val fields = fieldsSlot.captured
        assertEquals("Name", fields["fullName"])
        assertEquals("unique_name", fields["displayName"])
        assertEquals("Bio", fields["bio"])
    }

    @Test
    fun `save uploads photo when selected and stores photoUrl`() = runTest {
        viewModel.setFullName("Name")
        viewModel.setHandle("unique_name")
        viewModel.setSelectedPhoto(Uri.parse("content://photo"))

        every { mockUserRepo.isDisplayNameAvailable("unique_name", testUserId, any()) } answers {
            lastArg<(Result<Boolean>) -> Unit>().invoke(Result.success(true))
        }

        every {
            mockPhotoRepo.uploadPhoto(
                type = PhotoRepository.PhotoType.PROFILE_PHOTO,
                userId = testUserId,
                imageUri = any(),
                contentType = any()
            )
        } returns "https://cdn/profile.jpg"

        val fieldsSlot = slot<Map<String, Any>>()
        every { mockUserRepo.updateProfileFields(testUserId, capture(fieldsSlot), any()) } answers {
            lastArg<(Result<Unit>) -> Unit>().invoke(Result.success(Unit))
        }

        viewModel.save(contentType = "image/png", onSaved = {})

        assertEquals("https://cdn/profile.jpg", fieldsSlot.captured["photoUrl"])
    }
}

