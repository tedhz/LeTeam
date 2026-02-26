package com.leteam.locked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leteam.locked.auth.SignInScreen
import com.leteam.locked.auth.SignUpScreen
import com.leteam.locked.ui.navigation.MainScreen
import com.leteam.locked.ui.screens.profile.SetupProfileScreen
import com.leteam.locked.ui.theme.LockedTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LockedTheme {
                val rootViewModel: RootViewModel = viewModel()
                val appState by rootViewModel.appState.collectAsState()

                var showSignUp by remember { mutableStateOf(false) }

                when (appState) {
                    is AppState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is AppState.Unauthenticated -> {
                        if (showSignUp) {
                            SignUpScreen(
                                onLoggedIn = { rootViewModel.checkAuthState() },
                                onNavigateToSignIn = { showSignUp = false }
                            )
                        } else {
                            SignInScreen(
                                onLoggedIn = { rootViewModel.checkAuthState() },
                                onNavigateToSignUp = { showSignUp = true }
                            )
                        }
                    }
                    is AppState.NeedsProfileSetup -> {
                        SetupProfileScreen(
                            onSetupComplete = { rootViewModel.checkAuthState() }
                        )
                    }
                    is AppState.Authenticated -> {
                        MainScreen()
                    }
                }
            }
        }
    }
}