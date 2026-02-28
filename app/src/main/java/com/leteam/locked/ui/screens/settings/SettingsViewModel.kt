package com.leteam.locked.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class SettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    fun signOut() {
        auth.signOut()
    }
}