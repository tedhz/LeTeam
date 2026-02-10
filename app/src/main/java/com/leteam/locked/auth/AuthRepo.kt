package com.leteam.locked.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leteam.locked.firebase.FirebaseProvider
import com.leteam.locked.users.UserRepository

class AuthRepo(
    private val auth: FirebaseAuth = FirebaseProvider.auth,
    private val userRepo: UserRepository = UserRepository(FirebaseProvider.firestore)
) {
    fun signUp(
        email: String,
        password: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(Result.failure(Exception("User creation failed")))
                    return@addOnSuccessListener
                }

                userRepo.createUserProfile(
                    userId = user.uid,
                    email = email
                ) { docResult ->
                    if (docResult.isSuccess) onResult(Result.success(user))
                    else onResult(Result.failure(docResult.exceptionOrNull() ?: Exception("Failed to create user profile doc")))
                }
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    fun signIn(
        email: String,
        password: String,
        onResult: (Result<FirebaseUser>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    onResult(Result.success(user))
                } else {
                    onResult(Result.failure(Exception("Login failed.")))
                }
            }
            .addOnFailureListener { exception ->
                onResult(Result.failure(exception))
            }
    }

    fun signOut() {
        auth.signOut()
    }

    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}
