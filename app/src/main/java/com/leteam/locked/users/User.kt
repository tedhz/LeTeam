package com.leteam.locked.users

import com.google.firebase.Timestamp

data class User(
    val userId: String = "",
    val fullName: String = "",
    val displayName: String = "",
    val email: String = "",
    val dailyPostStatus: DailyPostStatus = DailyPostStatus(),
    val notificationPrefs: NotificationPrefs = NotificationPrefs(),
    val createdAt: Timestamp? = null
) {
    data class DailyPostStatus(
        val hasPostedToday: Boolean = false,
        val postId: String? = null
    )

    data class NotificationPrefs(
        val enabled: Boolean = true
    )
}