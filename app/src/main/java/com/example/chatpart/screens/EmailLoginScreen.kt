package com.example.chatpart.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.*
import com.example.chatpart.auth.SessionManager
import com.example.chatpart.network.APIService
import org.json.JSONObject

@Composable
fun EmailLoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit
) {

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage: String? by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val context = LocalContext.current

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
                "Sign in with Email",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                visualTransformation = PasswordVisualTransformation(),
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {

                    Log.d("LOGIN", "Button clicked")

                    APIService.login(email, password) { success, response ->

                        if (success) {

                            val json = JSONObject(response)

                            val data = json.getJSONObject("data")

                            val accessToken = data.getString("access_token")
                            val refreshToken = data.getString("refresh_token")

                            val user = data.getJSONObject("user")

                            val userId = user.getString("id")
                            val username = user.getString("username")
                            val userEmail = user.getString("email")

                            val sessionManager = SessionManager(context)

                            sessionManager.saveSession(
                                accessToken,
                                refreshToken,
                                userId,
                                username,
                                userEmail
                            )

                            println("Login success")
                            println(sessionManager.getAccessToken())

                            showSuccessDialog = true

                        } else {

                            println("Login failed: $response")
                            errorMessage = response
                            showErrorDialog = true
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

                Text("Sign In")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { onBack() }) {
                Text("Back")
            }

            Spacer(Modifier.weight(1f))
        }

        if (showSuccessDialog) {

            LoginSuccessDialog(
                onConfirm = {

                    showSuccessDialog = false
                    onLoginSuccess()
                }
            )
        }

        if (showErrorDialog) {

            LoginErrorDialog(
                message = errorMessage ?: "Login failed",
                onDismiss = {
                    showErrorDialog = false
                }
            )
        }
    }
}

@Composable
fun LoginSuccessDialog(
    onConfirm: () -> Unit
) {

    AlertDialog(
        onDismissRequest = { },

        confirmButton = {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Peach),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("OK")
                }
            }
        },

        title = {

            Text(
                text = "Login Successful!",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },

        text = {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(8.dp))

                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(80.dp)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Welcome back!",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 16.sp
                )
            }
        }
    )
}
@Composable
fun LoginErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {

    val cleanMessage = try {
        JSONObject(message).getString("error")
    } catch (e: Exception) {
        message
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },

        confirmButton = {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Peach),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("OK")
                }
            }
        },

        title = {

            Text(
                text = "Login Failed",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },

        text = {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(12.dp))

                Text(
                    text = cleanMessage,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}