package com.leteam.locked.users

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FollowRepository(
    private val db: FirebaseFirestore
) {
    private fun followsDoc(me: String, target: String) =
        db.collection("users").document(me).collection("follows").document(target)

    private fun followersDoc(target: String, me: String) =
        db.collection("users").document(target).collection("followers").document(me)

    fun follow(
        myUserId: String,
        targetUserId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val batch = db.batch()
        val payload = mapOf("createdAt" to FieldValue.serverTimestamp()) // optional

        batch.set(followsDoc(myUserId, targetUserId), payload)
        batch.set(followersDoc(targetUserId, myUserId), payload)

        batch.commit()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun unfollow(
        myUserId: String,
        targetUserId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val batch = db.batch()
        batch.delete(followsDoc(myUserId, targetUserId))
        batch.delete(followersDoc(targetUserId, myUserId))

        batch.commit()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun isFollowing(
        myUserId: String,
        targetUserId: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        followsDoc(myUserId, targetUserId).get()
            .addOnSuccessListener { snap -> onResult(Result.success(snap.exists())) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun getFollowingIds(
        myUserId: String,
        onResult: (Result<List<String>>) -> Unit
    ) {
        db.collection("users").document(myUserId).collection("follows").get()
            .addOnSuccessListener { qs -> onResult(Result.success(qs.documents.map { it.id })) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }

    fun getFollowerIds(
        myUserId: String,
        onResult: (Result<List<String>>) -> Unit
    ) {
        db.collection("users").document(myUserId).collection("followers").get()
            .addOnSuccessListener { qs -> onResult(Result.success(qs.documents.map { it.id })) }
            .addOnFailureListener { e -> onResult(Result.failure(e)) }
    }
}