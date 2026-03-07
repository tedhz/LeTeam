package com.leteam.locked.ui.screens.settings

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.leteam.locked.notifications.NotificationScheduler
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val mockApp: Application = mockk(relaxed = true)
    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockPrefs: SharedPreferences = mockk(relaxed = true)
    private val mockEditor: SharedPreferences.Editor = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { mockApp.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor

        every { mockPrefs.getBoolean("notifications_enabled", false) } returns false
        every { mockPrefs.getInt("reminder_hour", 18) } returns 18
        every { mockPrefs.getInt("reminder_minute", 0) } returns 0

        val mockAlarmManager: AlarmManager = mockk(relaxed = true)
        every { mockApp.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmManager

        mockkConstructor(NotificationScheduler::class)
        every { anyConstructed<NotificationScheduler>().scheduleDailyReminder(any(), any()) } just Runs
        every { anyConstructed<NotificationScheduler>().cancelReminder() } just Runs

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns mockAuth

        viewModel = SettingsViewModel(mockApp)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initialization loads values from SharedPreferences`() {
        assertEquals(false, viewModel.notificationsEnabled.value)
        assertEquals(18, viewModel.reminderHour.value)
        assertEquals(0, viewModel.reminderMinute.value)
    }

    @Test
    fun `signOut calls auth signOut`() {
        viewModel.signOut()
        verify { mockAuth.signOut() }
    }

    @Test
    fun `toggleNotifications to true saves preference and schedules reminder`() {
        viewModel.toggleNotifications(true)

        assertEquals(true, viewModel.notificationsEnabled.value)

        verify { mockEditor.putBoolean("notifications_enabled", true) }
        verify { mockEditor.apply() }

        verify { anyConstructed<NotificationScheduler>().scheduleDailyReminder(18, 0) }
    }

    @Test
    fun `toggleNotifications to false saves preference and cancels reminder`() {
        viewModel.toggleNotifications(false)

        assertEquals(false, viewModel.notificationsEnabled.value)
        verify { mockEditor.putBoolean("notifications_enabled", false) }

        verify { anyConstructed<NotificationScheduler>().cancelReminder() }
    }

    @Test
    fun `updateReminderTime saves preferences and reschedules if notifications are enabled`() {
        viewModel.toggleNotifications(true)
        clearMocks(mockEditor, answers = false)

        viewModel.updateReminderTime(20, 30)

        assertEquals(20, viewModel.reminderHour.value)
        assertEquals(30, viewModel.reminderMinute.value)

        verify { mockEditor.putInt("reminder_hour", 20) }
        verify { mockEditor.putInt("reminder_minute", 30) }
        verify { mockEditor.apply() }

        verify { anyConstructed<NotificationScheduler>().scheduleDailyReminder(20, 30) }
    }

    @Test
    fun `updateReminderTime saves preferences but does not reschedule if notifications disabled`() {
        viewModel.updateReminderTime(20, 30)

        verify { mockEditor.putInt("reminder_hour", 20) }
        verify(exactly = 0) { anyConstructed<NotificationScheduler>().scheduleDailyReminder(any(), any()) }
    }
}