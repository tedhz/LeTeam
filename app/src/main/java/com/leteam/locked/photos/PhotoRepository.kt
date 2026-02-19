package com.leteam.locked.photos

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.leteam.locked.firebase.FirebaseProvider
import kotlinx.coroutines.tasks.await

class PhotoRepository(
    private val storage: FirebaseStorage = FirebaseProvider.storage
) {
    private val rootRef = storage.reference

    enum class PhotoType {
        POST_PHOTO,
        PROFILE_PHOTO
    }

    private fun extensionFromContentType(contentType: String): String =
        when (contentType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

    /**
     * Post photos: images/{userId}/{timestamp}.jpg
     * Profile photos: profilePhotos/{userId}/profile.jpg
     */
    suspend fun uploadPhoto(
        type: PhotoType,
        userId: String,
        imageUri: Uri,
        contentType: String = "image/jpeg"
    ): String {
        val ext = extensionFromContentType(contentType)

        val path = when (type) {
            PhotoType.POST_PHOTO -> "images/$userId/${System.currentTimeMillis()}.$ext"
            PhotoType.PROFILE_PHOTO -> "profilePhotos/$userId/profile.$ext"
        }

        val ref = rootRef.child(path)

        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .build()

        ref.putFile(imageUri, metadata).await()
        return ref.downloadUrl.await().toString()
    }
}
