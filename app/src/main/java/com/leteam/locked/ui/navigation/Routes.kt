package com.leteam.locked.ui.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val CAMERA = "camera"
    const val POSTING = "posting/{imageUri}"
    fun posting(imageUri: Uri): String {
        val encodedUri = Uri.encode(imageUri.toString())
        return "posting/$encodedUri"
    }
}
