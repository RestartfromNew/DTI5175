package com.example.chatpart.screens

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.material.icons.rounded.Refresh
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
import com.example.chatpart.data.ClonedVoice
import com.example.chatpart.data.SlotStatus
import com.example.chatpart.firestore.FirestoreError
import com.example.chatpart.firestore.UserVoiceManager
import com.example.chatpart.i18n.Languages
import com.example.chatpart.i18n.LanguageManager
import com.example.chatpart.i18n.VoiceCloneManager
import com.example.chatpart.voice.AudioRecordManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@Composable
fun VoiceCloneScreen(
    isDarkMode: Boolean = false,
    characterName: String,
    characterId: String,
    avatarPath: String?,
    uid: String, // Firebase user ID for account isolation
    userVoiceManager: UserVoiceManager,
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
    var referenceTranscript by remember { mutableStateOf("") }
    val minimaxManager = remember { MiniMaxVoiceCloneManager(context) }
    val voiceCloneManager = remember { VoiceCloneManager(context) }

    // Network status
    var isOffline by remember { mutableStateOf(false) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRetryAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Slot status for Firestore
    var slotStatus by remember { mutableStateOf<SlotStatus?>(null) }
    var isSlotFull by remember { mutableStateOf(false) }
    var slotStatusError by remember { mutableStateOf<String?>(null) }

    // Check network connectivity
    fun checkNetworkStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        isOffline = capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Load slot status, then cross-check with actual Firestore voices.
    // If slotUsed says full but no real voices exist, the count is stale — allow cloning.
    LaunchedEffect(Unit) {
        checkNetworkStatus()
        userVoiceManager.getSlotStatus().onSuccess { status ->
            slotStatus = status
            slotStatusError = null
            if (status.isFull) {
                userVoiceManager.getClonedVoices().onSuccess { voices ->
                    isSlotFull = voices.isNotEmpty()
                }.onFailure {
                    isSlotFull = true // can't verify, stay conservative
                }
            } else {
                isSlotFull = false
            }
        }.onFailure { e ->
            slotStatusError = userVoiceManager.getUserFriendlyErrorMessage(e)
            Log.e("VoiceClone", "Failed to load slot status: ${e.message}")
        }
    }

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
            // Offline banner
            if (isOffline) {
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MicOff,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline - Recording only, cannot clone",
                            fontSize = 12.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            // Slot status section
            if (slotStatusError != null) {
                // Error state with retry
                Surface(
                    color = surfaceColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠ Slot status unavailable",
                            fontSize = 12.sp,
                            color = Color(0xFFE53935),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                slotStatusError = null
                                scope.launch {
                                    userVoiceManager.getSlotStatus().onSuccess {
                                        slotStatus = it
                                        isSlotFull = it.isFull
                                    }.onFailure { e ->
                                        slotStatusError = userVoiceManager.getUserFriendlyErrorMessage(e)
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry", fontSize = 12.sp)
                        }
                    }
                }
            } else if (slotStatus != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Slots: ${slotStatus!!.used}/${slotStatus!!.limit}",
                        fontSize = 12.sp,
                        color = if (isSlotFull) Color(0xFFE53935) else hintColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    LinearProgressIndicator(
                        progress = { slotStatus!!.percentage },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isSlotFull) Color(0xFFE53935) else peachColor,
                        trackColor = hintColor.copy(alpha = 0.2f),
                    )
                }

                if (isSlotFull) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Slot full. Please delete an existing voice first.",
                        fontSize = 12.sp,
                        color = Color(0xFFE53935)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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

            Spacer(modifier = Modifier.height(16.dp))

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
                                        val file = audioManager.stopRecording()
                                        recordedFile = file

                                        if (file != null) {
                                            scope.launch {
                                                //val transcript = voiceCloneManager.tryGenerateTranscript(file)

                                                //referenceTranscript = transcript
                                                //Log.d("VoiceDebug", "referenceTranscript = $referenceTranscript")
                                                Log.d("VoiceDebug", "recorded file = ${file.absolutePath}")
                                            }
                                        }
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = referenceTranscript,
                onValueChange = { referenceTranscript = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Reference transcript") },
                placeholder = { Text("Enter the exact words spoken in the reference audio") },
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                enabled = !isCloning,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = peachColor,
                    unfocusedBorderColor = hintColor.copy(alpha = 0.4f),
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor
                )
            )

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
                        // Check network first
                        checkNetworkStatus()
                        if (isOffline) {
                            cloneError = "No network connection. Please check your internet."
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "No network - Voice cloning requires internet",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            return@Button
                        }
                        // Check slot status first
                        if (isSlotFull) {
                            cloneError = "Voice slot is full. Please delete an existing voice first."
                            return@Button
                        }
                        // Check minimum duration (MiniMax requires at least 10 seconds)
                        if (recordingDuration < 10) {
                            cloneError = "Recording too short. Please record at least 10 seconds."
                            return@Button
                        }
                        if (referenceTranscript.isBlank()) {
                            cloneError = "Please enter the transcript of the reference audio."
                            return@Button
                        }
                        // Start cloning
                        isCloning = true
                        cloneError = null
                        // Launch cloning in background

                        scope.launch {
                            val file = recordedFile
                            if (file != null) {
                                // 1. SECURITY: Ensure user document exists (recreates it if manually deleted)
                                val userEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: "demo@example.com"
                                userVoiceManager.ensureUserExists(userEmail)
                                
                                // 2. Call MiniMax cloning
                                val result = minimaxManager.cloneVoice(
                                    audioFile = file,
                                    characterId = characterId,
                                    uid = uid
                                )

                                if (result.isSuccess) {
                                    val voiceId = result.getOrThrow()

                                    // 3. Upload reference assets (Audio + Transcript) for future use
                                    try {
                                        val uploadResult = voiceCloneManager.uploadReferenceAssets(
                                            audioFile = file,
                                            characterId = characterId,
                                            avatarPath = avatarPath,
                                            transcript = referenceTranscript
                                        )
                                        Log.d("VoiceDebug", "uploadReferenceAssets result = $uploadResult")
                                    } catch (e: Exception) {
                                        Log.e("VoiceDebug", "uploadReferenceAssets failed: ${e.message}", e)
                                    }

                                    val voice = ClonedVoice(
                                        id = UUID.randomUUID().toString(),
                                        voiceId = voiceId,
                                        characterId = characterId,
                                        characterName = characterName
                                    )

                                    userVoiceManager.addClonedVoice(voice)
                                        .onSuccess {
                                            cloneSuccess = true
                                            cloneError = null
                                            isCloning = false
                                            delay(1000)
                                            onVoiceCloned(voiceId)
                                        }
                                        .onFailure { firestoreError ->
                                            Log.e("VoiceClone", "Firestore save failed: ${firestoreError.message}")
                                            isCloning = false

                                            Log.w("VoiceClone", "Rolling back MiniMax voice due to Firestore failure: $voiceId")
                                            minimaxManager.deleteVoice(voiceId)

                                            val errorMsg = userVoiceManager.getUserFriendlyErrorMessage(firestoreError)
                                            cloneError = "$errorMsg (Voice cloned but not saved)"

                                            scope.launch {
                                                val snackbarResult = snackbarHostState.showSnackbar(
                                                    message = "Failed to save voice: $errorMsg",
                                                    actionLabel = "Retry",
                                                    duration = SnackbarDuration.Long
                                                )

                                                when (snackbarResult) {
                                                    SnackbarResult.ActionPerformed -> {
                                                        isCloning = true
                                                        userVoiceManager.addClonedVoice(voice)
                                                            .onSuccess {
                                                                cloneSuccess = true
                                                                cloneError = null
                                                                isCloning = false
                                                                scope.launch {
                                                                    delay(1000)
                                                                    onVoiceCloned(voiceId)
                                                                }
                                                            }
                                                            .onFailure { retryError ->
                                                                isCloning = false
                                                                cloneSuccess = false
                                                                cloneError = "Failed to save: ${userVoiceManager.getUserFriendlyErrorMessage(retryError)}"
                                                                Log.e("VoiceClone", "Retry failed: ${retryError.message}")
                                                            }
                                                    }
                                                    SnackbarResult.Dismissed -> {
                                                        cloneSuccess = false
                                                        cloneError = "Voice cloned but not saved to cloud"
                                                    }
                                                }
                                            }
                                        }
                                } else {
                                    val error = result.exceptionOrNull()
                                    isCloning = false
                                    Log.e("VoiceClone", "Clone failed: ${error?.message}", error)
                                    val msg = error?.message ?: "Unknown error"
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
                        containerColor = if (isSlotFull) hintColor else peachColor,
                        contentColor = Color.White
                    ),
                    enabled = recordedFile != null && !isCloning && !isSlotFull
                ) {
                    Text(sendCloneText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Snackbar for error messages and retry
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
        ) { data ->
            val actionLabel = data.visuals.actionLabel
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (actionLabel != null) Color(0xFF424242) else Color(0xFFE53935),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.visuals.message,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    if (actionLabel != null) {
                        TextButton(
                            onClick = { data.performAction() }
                        ) {
                            Text(
                                text = actionLabel,
                                color = Peach,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
