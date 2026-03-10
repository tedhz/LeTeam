package com.leteam.locked.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PasswordResetScreen(
    viewModel: PasswordResetViewModel = viewModel(),
    onBack: () -> Unit
) {
    val email by viewModel.email.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

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

            Spacer(modifier = Modifier.height(170.dp))

            Text(
                text = "Reset password",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your email to receive a password reset link.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF222222)
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    viewModel.onEmailChange(it)
                    viewModel.clearMessages()
                },
                placeholder = {
                    Text(
                        text = "email@domain.com",
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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.sendPasswordResetEmail() },
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
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Send Reset Email",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (successMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = successMessage!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
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
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Back",
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