package com.leteam.locked.users

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository(
    private val db: FirebaseFirestore
) {
    private fun userDoc(userId: String) = db.collection("users").document(userId)

    fun createUserProfile(
        userId: String,
        email: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val payload = hashMapOf(
            "email" to email,
            "dailyPostStatus" to hashMapOf(
                "hasPostedToday" to false,
                "postId" to null
            ),
            "notificationPrefs" to hashMapOf(
                "enabled" to true
            ),
            "createdAt" to FieldValue.serverTimestamp()
        )

        userDoc(userId).set(payload)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun getUser(
        userId: String,
        onResult: (Result<User>) -> Unit
    ) {
        userDoc(userId).get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    onResult(Result.failure(Exception("User doc not found for $userId")))
                    return@addOnSuccessListener
                }

                val displayName = snap.getString("displayName") ?: ""
                val fullName = snap.getString("fullName") ?: ""
                val email = snap.getString("email") ?: ""

                val dpsMap = snap.get("dailyPostStatus") as? Map<*, *>
                val hasPostedToday = dpsMap?.get("hasPostedToday") as? Boolean ?: false
                val postId = dpsMap?.get("postId") as? String

                val prefsMap = snap.get("notificationPrefs") as? Map<*, *>
                val enabled = prefsMap?.get("enabled") as? Boolean ?: true

                val createdAt = snap.getTimestamp("createdAt")

                onResult(
                    Result.success(
                        User(
                            userId = userId,
                            fullName = fullName,
                            displayName = displayName,
                            email = email,
                            dailyPostStatus = User.DailyPostStatus(hasPostedToday, postId),
                            notificationPrefs = User.NotificationPrefs(enabled),
                            createdAt = createdAt
                        )
                    )
                )
            }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun updateProfile(
        userId: String,
        fullName: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (fullName.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("fullName is required")))
            return
        }

        userDoc(userId).update("fullName", fullName)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun updateNotificationEnabled(
        userId: String,
        enabled: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        userDoc(userId).update("notificationPrefs", mapOf("enabled" to enabled))
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun updateDailyPostStatus(
        userId: String,
        hasPostedToday: Boolean,
        postId: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        userDoc(userId).update(
            "dailyPostStatus",
            mapOf(
                "hasPostedToday" to hasPostedToday,
                "postId" to postId
            )
        )
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun updateFullName(userId: String, fullName: String, onResult: (Result<Unit>) -> Unit) {
        userDoc(userId).update("fullName", fullName)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun updateDisplayName(userId: String, displayName: String, onResult: (Result<Unit>) -> Unit) {
        userDoc(userId).update("displayName", displayName)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }
}