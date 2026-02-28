package com.leteam.locked.ui.screens.settings

import com.google.firebase.auth.FirebaseAuth
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        viewModel = SettingsViewModel(mockAuth)
    }

    @Test
    fun `signOut calls auth signOut`() {
        viewModel.signOut()
        verify { mockAuth.signOut() }
    }
}
