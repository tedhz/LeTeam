package com.leteam.locked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.leteam.locked.auth.SignInScreen
import com.leteam.locked.auth.SignUpScreen
import com.leteam.locked.ui.navigation.MainScreen
import com.leteam.locked.ui.theme.LockedTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LockedTheme {
                var loggedIn by remember { mutableStateOf(false) }
                var showSignUp by remember { mutableStateOf(false) }

                if (loggedIn) {
                    MainScreen()
                } else if (showSignUp) {
                    SignUpScreen(
                        onLoggedIn = { loggedIn = true },
                        onNavigateToSignIn = { showSignUp = false }
                    )
                } else {
                    SignInScreen(
                        onLoggedIn = { loggedIn = true },
                        onNavigateToSignUp = { showSignUp = true }
                    )
                }
            }
        }
    }
}
