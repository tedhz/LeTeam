package com.leteam.locked.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.sp

@Composable
fun SignInScreen(
    repo: AuthRepo = AuthRepo(),
    onLoggedIn: () -> Unit = {},
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(140.dp))

            Text(
                text = "Locked",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(140.dp))

            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your login details",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF7A7A7A)
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = {
                    Text(
                        text = "username",
                        color = Color(0xFF9A9A9A)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD9D9D9),
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        text = "password",
                        color = Color(0xFF9A9A9A)
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFD9D9D9),
                    unfocusedBorderColor = Color(0xFFD9D9D9),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            )

            if (errorText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onNavigateToForgotPassword,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(0.dp)
            ){
                Text(
                    text = "Forgot password?",
                    color = Color(0xFF8A8A8A),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    errorText = null
                    isLoading = true
                    repo.signIn(email.trim(), password) { result ->
                        isLoading = false
                        result
                            .onSuccess { onLoggedIn() }
                            .onFailure { errorText = it.message ?: "Sign in failed" }
                    }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (isLoading) "Loading..." else "Log In",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0E0E0)
                )

                TextButton(
                    onClick = onNavigateToSignUp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "or Sign Up",
                        color = Color(0xFF8A8A8A),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE0E0E0)
                )
            }
        }
    }
}