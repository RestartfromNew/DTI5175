package com.example.chatpart.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import com.example.chatpart.api.MiniMaxVoiceCloneManager
import com.example.chatpart.i18n.Languages
import com.example.chatpart.i18n.LanguageManager
import com.example.chatpart.voice.AudioRecordManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun VoiceCloneScreen(
    isDarkMode: Boolean = false,
    characterName: String,
    onVoiceCloned: (voiceId: String) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else SoftWhite
    val gradientEnd = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3F0FF)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val hintColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray
    val peachColor = Peach

    // Get localized strings
    val context = LocalContext.current
    val languageManager = remember { LanguageManager(context) }
    val currentLang = languageManager.getCurrentLanguage()

    val titleText = Languages.getString(currentLang, "CLONE_VOICE_FOR").replace("{name}", characterName)
    val descriptionText = Languages.getString(currentLang, "CLONE_VOICE_DESC")
    val holdToRecordText = Languages.getString(currentLang, "HOLD_TO_RECORD")
    val releaseToStopText = Languages.getString(currentLang, "RELEASE_TO_STOP")
    val sendCloneText = Languages.getString(currentLang, "SEND_CLONE")
    val cloningText = Languages.getString(currentLang, "CLONING")
    val successText = Languages.getString(currentLang, "CLONE_SUCCESS")
    val skipText = Languages.getString(currentLang, "SKIP")

    // State
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isCloning by remember { mutableStateOf(false) }
    var cloneSuccess by remember { mutableStateOf(false) }
    var cloneError by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(false) }

    // Coroutine scope
    val scope = rememberCoroutineScope()

    // Audio recorder
    val audioManager = remember { AudioRecordManager(context) }

    // Vibrator for haptic feedback
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Function to trigger haptic feedback
    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            Log.e("VoiceCloneScreen", "Haptic feedback failed: ${e.message}")
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    // Recording timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    // Pulse animation for recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundColor, gradientEnd)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = titleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Description
            Text(
                text = descriptionText,
                fontSize = 14.sp,
                color = hintColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Recording button
            Box(
                modifier = Modifier
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulse ring when recording
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(peachColor.copy(alpha = 0.3f))
                    )
                }

                // Main button
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) {
                                Brush.radialGradient(
                                    colors = listOf(peachColor, peachColor)
                                )
                            } else {
                                Brush.radialGradient(
                                    colors = listOf(peachColor, peachColor.copy(alpha = 0.7f))
                                )
                            }
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    // Request permission if needed
                                    if (!hasPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        return@detectTapGestures
                                    }

                                    // Start recording
                                    hasPermission = audioManager.hasPermission()
                                    if (hasPermission) {
                                        isRecording = true
                                        recordingDuration = 0
                                        triggerHapticFeedback() // Vibration feedback
                                        audioManager.startRecording()
                                    }

                                    tryAwaitRelease()

                                    // Stop recording
                                    triggerHapticFeedback() // Vibration feedback
                                    if (isRecording) {
                                        isRecording = false
                                        recordedFile = audioManager.stopRecording()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        contentDescription = "Record",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recording status
            if (isRecording) {
                Text(
                    text = releaseToStopText,
                    fontSize = 16.sp,
                    color = peachColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatDuration(recordingDuration),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            } else if (recordedFile != null) {
                Text(
                    text = "✓ Recording complete (${formatDuration(recordingDuration)})",
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50)
                )
            } else {
                Text(
                    text = holdToRecordText,
                    fontSize = 14.sp,
                    color = hintColor
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Cloning status
            if (isCloning) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        color = peachColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = cloningText,
                        fontSize = 16.sp,
                        color = textColor
                    )
                }
            } else if (cloneSuccess) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ $successText",
                        fontSize = 16.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (cloneError != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = cloneError!!,
                        fontSize = 14.sp,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = hintColor
                    ),
                    enabled = !isCloning
                ) {
                    Text(skipText, fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        // Check minimum duration (MiniMax requires at least 10 seconds)
                        if (recordingDuration < 10) {
                            cloneError = "Recording too short. Please record at least 10 seconds."
                            return@Button
                        }
                        // Start cloning
                        isCloning = true
                        cloneError = null
                        // Launch cloning in background
                        val manager = MiniMaxVoiceCloneManager(context)
                        scope.launch {
                            if (recordedFile != null) {
                                val result = manager.cloneVoice(recordedFile!!, characterName)
                                isCloning = false
                                result.onSuccess { voiceId ->
                                    cloneSuccess = true
                                    cloneError = null
                                    delay(1000)
                                    onVoiceCloned(voiceId)
                                }.onFailure { error ->
                                    Log.e("VoiceClone", "Clone failed: ${error.message}")
                                    val msg = error.message ?: "Unknown error"
                                    cloneError = when {
                                        msg.contains("2013") || msg.contains("sensitive") ->
                                            "Audio rejected: Please record in Chinese or English only"
                                        msg.contains("too short") ->
                                            "Recording too short (min 10 seconds)"
                                        else -> "Clone failed: $msg"
                                    }
                                }
                            } else {
                                isCloning = false
                                cloneError = "No recorded file available"
                                Log.e("VoiceClone", "No recorded file available")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = peachColor,
                        contentColor = Color.White
                    ),
                    enabled = recordedFile != null && !isCloning
                ) {
                    Text(sendCloneText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
