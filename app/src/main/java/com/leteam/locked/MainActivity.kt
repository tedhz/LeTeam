package com.leteam.locked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.leteam.locked.auth.LoginScreen
import com.leteam.locked.ui.theme.LockedTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            LockedTheme {

                var loggedIn by remember { mutableStateOf(false) }

                if (loggedIn) {
                    Text("AHAHAHAHAHAHA BANG")

                } else {

                    LoginScreen(
                        onLoggedIn = {
                            loggedIn = true
                        }
                    )
                }
            }
        }
    }
}
