package com.leteam.locked.ui.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PROFILE_USER = "profile/{userId}"
    const val SETTINGS = "settings"
    const val CAMERA = "camera"
    const val WORKOUTS = "workouts"
    const val SEARCH = "search"
    const val POSTING = "posting/{imageUri}"
    const val MYWORKOUTS = "myworkouts"
    const val INSIGHTS = "insights"
    const val SIGNIN = "signin"
    const val PASSRESET = "passreset"

    fun profileUser(userId: String): String = "profile/$userId"

    fun posting(imageUri: Uri): String {
        val encodedUri = Uri.encode(imageUri.toString())
        return "posting/$encodedUri"
    }
}
