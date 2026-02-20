package com.leteam.locked.photos

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.UploadTask.TaskSnapshot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PhotoRepositoryTest {

    private val mockStorage: FirebaseStorage = mockk(relaxed = true)
    private val mockRootRef: StorageReference = mockk(relaxed = true)
    private val mockChildRef: StorageReference = mockk(relaxed = true)
    private val mockUploadTask: UploadTask = mockk(relaxed = true)
    private lateinit var mockDownloadUrlTask: Task<Uri>

    private lateinit var repository: PhotoRepository

    private val testUserId = "user_123"
    private val testImageUri: Uri = mockk()
    private val testDownloadUrl = "https://firebasestorage.googleapis.com/test.jpg"
    private val testDownloadUri: Uri = mockk()

    @Before
    fun setUp() {
        // Mock the await() extension function statically
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        
        // Create a real completed Task for downloadUrl
        mockDownloadUrlTask = Tasks.forResult(testDownloadUri)
        
        // Make UploadTask behave like a completed task for await()
        val mockTaskSnapshot: TaskSnapshot = mockk(relaxed = true)
        every { mockUploadTask.isComplete } returns true
        every { mockUploadTask.isSuccessful } returns true
        every { mockUploadTask.exception } returns null
        every { mockUploadTask.result } returns mockTaskSnapshot
        every { mockUploadTask.isCanceled } returns false
        
        // Mock the await() calls
        coEvery { mockUploadTask.await() } returns mockTaskSnapshot
        coEvery { mockDownloadUrlTask.await() } returns testDownloadUri
        
        every { mockStorage.reference } returns mockRootRef
        every { mockRootRef.child(any<String>()) } returns mockChildRef
        every { mockChildRef.putFile(any<Uri>(), any<StorageMetadata>()) } returns mockUploadTask
        every { mockChildRef.downloadUrl } returns mockDownloadUrlTask
        every { testDownloadUri.toString() } returns testDownloadUrl

        repository = PhotoRepository(mockStorage)
    }
    
    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @Test
    fun `uploadPhoto for POST_PHOTO generates correct path with jpeg extension`() = runTest {

        val result = repository.uploadPhoto(
            type = PhotoRepository.PhotoType.POST_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "image/jpeg"
        )

        assertEquals(testDownloadUrl, result)
        verify { mockRootRef.child(match { it.startsWith("images/$testUserId/") && it.endsWith(".jpg") }) }
        verify { mockChildRef.putFile(testImageUri, any<StorageMetadata>()) }
    }

    @Test
    fun `uploadPhoto for POST_PHOTO generates correct path with png extension`() = runTest {
        val result = repository.uploadPhoto(
            type = PhotoRepository.PhotoType.POST_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "image/png"
        )

        assertEquals(testDownloadUrl, result)
        verify { mockRootRef.child(match { it.startsWith("images/$testUserId/") && it.endsWith(".png") }) }
    }

    @Test
    fun `uploadPhoto for POST_PHOTO generates correct path with webp extension`() = runTest {
        val result = repository.uploadPhoto(
            type = PhotoRepository.PhotoType.POST_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "image/webp"
        )

        assertEquals(testDownloadUrl, result)
        verify { mockRootRef.child(match { it.startsWith("images/$testUserId/") && it.endsWith(".webp") }) }
    }

    @Test
    fun `uploadPhoto for POST_PHOTO defaults to jpg for unknown content type`() = runTest {
        val result = repository.uploadPhoto(
            type = PhotoRepository.PhotoType.POST_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "image/gif"
        )

        assertEquals(testDownloadUrl, result)
        verify { mockRootRef.child(match { it.startsWith("images/$testUserId/") && it.endsWith(".jpg") }) }
    }

    @Test
    fun `uploadPhoto for POST_PHOTO handles jpg content type`() = runTest {
        val result = repository.uploadPhoto(
            type = PhotoRepository.PhotoType.POST_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "image/jpg"
        )

        assertEquals(testDownloadUrl, result)
        verify { mockRootRef.child(match { it.startsWith("images/$testUserId/") && it.endsWith(".jpg") }) }
    }

    @Test
    fun `uploadPhoto for PROFILE_PHOTO generates correct path with jpeg extension`() = runTest {
        val result = repository.uploadPhoto(
            type = PhotoRepository.PhotoType.PROFILE_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "image/jpeg"
        )

        assertEquals(testDownloadUrl, result)
        verify { mockRootRef.child("profilePhotos/$testUserId/profile.jpg") }
        verify { mockChildRef.putFile(testImageUri, any<StorageMetadata>()) }
    }

    @Test
    fun `uploadPhoto for PROFILE_PHOTO generates correct path with png extension`() = runTest {
        val result = repository.uploadPhoto(
            type = PhotoRepository.PhotoType.PROFILE_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "image/png"
        )

        assertEquals(testDownloadUrl, result)
        verify { mockRootRef.child("profilePhotos/$testUserId/profile.png") }
    }

    @Test
    fun `uploadPhoto for PROFILE_PHOTO generates correct path with webp extension`() = runTest {
        val result = repository.uploadPhoto(
            type = PhotoRepository.PhotoType.PROFILE_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "image/webp"
        )

        assertEquals(testDownloadUrl, result)
        verify { mockRootRef.child("profilePhotos/$testUserId/profile.webp") }
    }

    @Test
    fun `uploadPhoto sets correct content type in metadata`() = runTest {
        val metadataSlot = io.mockk.slot<StorageMetadata>()

        repository.uploadPhoto(
            type = PhotoRepository.PhotoType.POST_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "image/png"
        )

        verify { mockChildRef.putFile(testImageUri, capture(metadataSlot)) }
        assertEquals("image/png", metadataSlot.captured.contentType)
    }

    @Test
    fun `uploadPhoto uses default content type when not provided`() = runTest {
        val metadataSlot = io.mockk.slot<StorageMetadata>()

        repository.uploadPhoto(
            type = PhotoRepository.PhotoType.POST_PHOTO,
            userId = testUserId,
            imageUri = testImageUri
        )

        verify { mockChildRef.putFile(testImageUri, capture(metadataSlot)) }
        assertEquals("image/jpeg", metadataSlot.captured.contentType)
    }

    @Test
    fun `uploadPhoto handles case insensitive content type`() = runTest {
        val result = repository.uploadPhoto(
            type = PhotoRepository.PhotoType.POST_PHOTO,
            userId = testUserId,
            imageUri = testImageUri,
            contentType = "IMAGE/PNG"
        )

        assertEquals(testDownloadUrl, result)
        verify { mockRootRef.child(match { it.startsWith("images/$testUserId/") && it.endsWith(".png") }) }
    }
}

