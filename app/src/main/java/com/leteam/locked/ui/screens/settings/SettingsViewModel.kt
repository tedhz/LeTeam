package com.leteam.locked.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.leteam.locked.auth.AuthRepo
import com.leteam.locked.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val authRepo = AuthRepo()
    private val prefs = application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private val notificationScheduler = NotificationScheduler(application)

    private val _notificationsEnabled = MutableStateFlow(
        prefs.getBoolean("notifications_enabled", false)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(prefs.getInt("reminder_hour", 18))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(prefs.getInt("reminder_minute", 0))
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    private val _resetEmailState = MutableStateFlow<Result<Unit>?>(null)
    val resetEmailState: StateFlow<Result<Unit>?> = _resetEmailState.asStateFlow()

    fun signOut() {
        authRepo.signOut()
    }

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()

        if (enabled) {
            notificationScheduler.scheduleDailyReminder(_reminderHour.value, _reminderMinute.value)
        } else {
            notificationScheduler.cancelReminder()
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        prefs.edit()
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()

        if (_notificationsEnabled.value) {
            notificationScheduler.scheduleDailyReminder(hour, minute)
        }
    }

    fun sendPasswordResetEmail(email: String) {
        authRepo.sendPasswordResetEmail(email) { result ->
            _resetEmailState.value = result
        }
    }

    fun clearResetEmailState() {
        _resetEmailState.value = null
    }
}