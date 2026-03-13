package com.example.chatpart.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.*
import com.example.chatpart.auth.GoogleAuthManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authManager: GoogleAuthManager,
    onSignInSuccess: () -> Unit,
    onEmailSignInClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Get Activity context - required by Credential Manager
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(SoftWhite, Color(0xFFF3F0FF), Color(0xFFFFE9E3))
                    )
                )
                .padding(padding)
        ) {
            // Background decoration
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
                Spacer(Modifier.weight(0.8f))

                // App Icon
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Peach.copy(alpha = 0.15f), Lavender.copy(alpha = 0.15f))
                                )
                            )
                    ) {
                        Icon(
                            Icons.Rounded.Psychology,
                            contentDescription = null,
                            tint = Peach,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Title
                Text(
                    "Welcome to\nAI Chat",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkText,
                    textAlign = TextAlign.Center,
                    lineHeight = 42.sp
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Your intelligent companion,\npowered by AI magic ✨",
                    fontSize = 17.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(Modifier.weight(1f))

                // Google Sign-In Button
                Button(
                    onClick = {
                        if (activity == null) {
                            errorMessage = "Cannot get Activity context"
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val result = authManager.signIn(activity)
                            isLoading = false
                            result.fold(
                                onSuccess = { onSignInSuccess() },
                                onFailure = { e ->
                                    errorMessage = e.localizedMessage ?: "Sign-in failed"
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Peach,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Signing in...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    } else {
                        // Google "G" icon using Canvas
                        GoogleGIcon(modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Sign in with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        onEmailSignInClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        "Sign in with Email",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkText
                    )
                }
                Spacer(Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account?",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    TextButton(
                        onClick = {
                            onRegisterClick()
                        }
                    ) {
                        Text(
                            text = "Register",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Peach
                        )
                    }
                }
                Text(
                    "By signing in, you agree to our Terms of Service",
                    fontSize = 12.sp,
                    color = Color.Gray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun GoogleGIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.15f

        // Red arc (top-right)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -10f,
            sweepAngle = -80f,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = Offset(strokeW / 2, strokeW / 2),
            size = androidx.compose.ui.geometry.Size(w - strokeW, h - strokeW)
        )
        // Yellow arc (bottom-right)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 170f,
            sweepAngle = -80f,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = Offset(strokeW / 2, strokeW / 2),
            size = androidx.compose.ui.geometry.Size(w - strokeW, h - strokeW)
        )
        // Green arc (bottom-left)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 90f,
            sweepAngle = 80f,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = Offset(strokeW / 2, strokeW / 2),
            size = androidx.compose.ui.geometry.Size(w - strokeW, h - strokeW)
        )
        // Blue arc (left)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 170f,
            sweepAngle = 80f,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = Offset(strokeW / 2, strokeW / 2),
            size = androidx.compose.ui.geometry.Size(w - strokeW, h - strokeW)
        )
        // Blue horizontal bar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(w * 0.5f, h * 0.42f),
            end = Offset(w * 0.92f, h * 0.42f),
            strokeWidth = strokeW
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(w * 0.5f, h * 0.58f),
            end = Offset(w * 0.92f, h * 0.58f),
            strokeWidth = strokeW
        )
    }
}
