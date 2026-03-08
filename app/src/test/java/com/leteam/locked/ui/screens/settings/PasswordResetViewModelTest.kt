package com.leteam.locked.ui.screens.settings

import com.leteam.locked.auth.AuthRepo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PasswordResetViewModelTest {

    private val mockAuthRepo: AuthRepo = mockk(relaxed = true)

    private lateinit var viewModel: PasswordResetViewModel

    @Before
    fun setUp() {
        viewModel = PasswordResetViewModel(mockAuthRepo)
    }

    @Test
    fun `onEmailChange updates email state`() {
        viewModel.onEmailChange("cliff@example.com")

        assertEquals("cliff@example.com", viewModel.email.value)
    }

    @Test
    fun `sendPasswordResetEmail sets error when email is blank`() {
        viewModel.onEmailChange("")

        viewModel.sendPasswordResetEmail()

        assertEquals("Please enter your email.", viewModel.errorMessage.value)
        assertNull(viewModel.successMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sendPasswordResetEmail trims email before sending`() {
        every { mockAuthRepo.sendPasswordResetEmail("cliff@example.com", any()) } answers {
            val callback = secondArg<(Result<Unit>) -> Unit>()
            callback(Result.success(Unit))
        }

        viewModel.onEmailChange("  cliff@example.com  ")
        viewModel.sendPasswordResetEmail()

        assertEquals("Password reset email sent.", viewModel.successMessage.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sendPasswordResetEmail sets success message on success`() {
        every { mockAuthRepo.sendPasswordResetEmail("cliff@example.com", any()) } answers {
            val callback = secondArg<(Result<Unit>) -> Unit>()
            callback(Result.success(Unit))
        }

        viewModel.onEmailChange("cliff@example.com")
        viewModel.sendPasswordResetEmail()

        assertEquals("Password reset email sent.", viewModel.successMessage.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sendPasswordResetEmail sets error message on failure`() {
        every { mockAuthRepo.sendPasswordResetEmail("cliff@example.com", any()) } answers {
            val callback = secondArg<(Result<Unit>) -> Unit>()
            callback(Result.failure(Exception("No user found with this email.")))
        }

        viewModel.onEmailChange("cliff@example.com")
        viewModel.sendPasswordResetEmail()

        assertEquals("No user found with this email.", viewModel.errorMessage.value)
        assertNull(viewModel.successMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `sendPasswordResetEmail clears old messages before new request`() {
        every { mockAuthRepo.sendPasswordResetEmail("cliff@example.com", any()) } answers {
            val callback = secondArg<(Result<Unit>) -> Unit>()
            callback(Result.success(Unit))
        }

        viewModel.onEmailChange("")
        viewModel.sendPasswordResetEmail()
        assertEquals("Please enter your email.", viewModel.errorMessage.value)

        viewModel.onEmailChange("cliff@example.com")
        viewModel.sendPasswordResetEmail()

        assertEquals("Password reset email sent.", viewModel.successMessage.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `clearMessages clears success and error messages`() {
        every { mockAuthRepo.sendPasswordResetEmail("cliff@example.com", any()) } answers {
            val callback = secondArg<(Result<Unit>) -> Unit>()
            callback(Result.success(Unit))
        }

        viewModel.onEmailChange("cliff@example.com")
        viewModel.sendPasswordResetEmail()

        assertEquals("Password reset email sent.", viewModel.successMessage.value)

        viewModel.clearMessages()

        assertNull(viewModel.successMessage.value)
        assertNull(viewModel.errorMessage.value)
    }
}