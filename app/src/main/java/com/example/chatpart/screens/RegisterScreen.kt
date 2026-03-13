package com.example.chatpart.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.*
import com.example.chatpart.network.APIService

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {


    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SoftWhite, Color(0xFFF3F0FF), Color(0xFFFFE9E3))
                )
            )
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                Peach.copy(alpha = 0.08f),
                radius = 200.dp.toPx(),
                center = Offset(size.width * 0.8f, size.height * 0.15f)
            )
            drawCircle(
                Lavender.copy(alpha = 0.08f),
                radius = 150.dp.toPx(),
                center = Offset(size.width * 0.1f, size.height * 0.7f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(Modifier.weight(0.6f))

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = Peach,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Create Account",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {

                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match"
                        return@Button
                    }

                    if (username.isBlank() || email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill all fields"
                        return@Button
                    }

                    APIService.register(
                        username,
                        email,
                        password
                    ) { success, response ->

                        if (success) {
                            showSuccessDialog = true
                        } else {
                            errorMessage = response ?: "Register failed"
                        }
                    }

                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Peach
                )
            ) {
                Text("Register")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { onBack() }) {
                Text("Back to Login")
            }

            Spacer(Modifier.weight(1f))
        }

        if (showSuccessDialog) {
            SuccessDialog {
                showSuccessDialog = false
                onRegisterSuccess()
            }
        }

        errorMessage?.let {
            ErrorDialog(
                message = it
            ) {
                errorMessage = null
            }
        }
    }
}

@Composable
fun SuccessDialog(
    onConfirm: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Peach)
            ) {
                Text("OK")
            }
        },
        title = {
            Text(
                text = "Successful!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(64.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Your account has been created successfully!",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    )
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { onDismiss() },

        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Peach)
            ) {
                Text("OK")
            }
        },

        title = {
            Text(
                text = "Error",
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Text(message)
        }
    )
}