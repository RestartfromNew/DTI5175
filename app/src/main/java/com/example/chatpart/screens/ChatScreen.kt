@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.chatpart.screens

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.example.chatpart.api.DeepgramAsrClient
import com.example.chatpart.api.MiniMaxAudioClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.chatpart.DarkText
import com.example.chatpart.Lavender
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import com.example.chatpart.voice.AudioRecordManager
import com.google.firebase.auth.FirebaseUser
import com.example.chatpart.data.PersonChat
import com.example.chatpart.data.ChatHistoryManager
import com.example.chatpart.data.ChatbotPreferences
import com.example.chatpart.data.ChatMessageData
import com.example.chatpart.domain.Profile
import com.example.chatpart.domain.Result
import com.example.chatpart.i18n.LocalizedString
import com.example.chatpart.i18n.Languages
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Role
import kotlinx.coroutines.launch
import java.io.File

// ─── Data Models ────────────────────────────────────────────────────────────

data class ChatbotAvatar(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Color,
    val greeting: String,
    val isCustomCharacter: Boolean = false,
    val profileId: String? = null,
    val voiceId: String? = null    // MiniMax voice_id, null = use default preset
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val isVoice: Boolean = false,
    val voiceDurationSec: Int = 0,
    val voiceFilePath: String? = null,   // absolute path to WAV file for STT replay
    val subtitle: String? = null,        // transcribed text (always stored)
    val isSubtitleExpanded: Boolean = false  // per-bubble: true = show subtitle
)

// Default AI Chatbots - only 3 (use getDefaultChatbots() composable function)
object DefaultChatbots {
    @Composable
    fun get(): List<ChatbotAvatar> = listOf(
        ChatbotAvatar(
            id = "assistant",
            name = LocalizedString("DEFAULT_ASSISTANT"),
            emoji = "🤖",
            color = Color(0xFF7C5CFC),
            greeting = "Hi! I'm your AI assistant. How can I help? ✨",
            isCustomCharacter = false
        ),
        ChatbotAvatar(
            id = "teacher",
            name = LocalizedString("DEFAULT_TEACHER"),
            emoji = "👩‍🏫",
            color = Color(0xFF4CAF50),
            greeting = "Hello! I'm your teacher. Ask me anything! 📚",
            isCustomCharacter = false
        ),
        ChatbotAvatar(
            id = "coding",
            name = LocalizedString("DEFAULT_CODER"),
            emoji = "💻",
            color = Color(0xFF2196F3),
            greeting = "Hey! Need help with code? Let's build something! 🚀",
            isCustomCharacter = false
        )
    )
}

// ─── Main ChatScreen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUser: FirebaseUser? = null,
    personChat: PersonChat? = null,
    currentProfile: Profile? = null,
    isDarkMode: Boolean = false,
    customCharacters: List<Profile> = emptyList(),
    targetBotId: String? = null,
    currentLanguage: String = "en",
    voiceAssignmentPrefs: com.example.chatpart.data.VoiceAssignmentPreferences? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Theme-aware colors
    val backgroundColor = if (isDarkMode) {
        androidx.compose.material3.MaterialTheme.colorScheme.background
    } else {
        com.example.chatpart.SoftWhite
    }
    val gradientEnd = if (isDarkMode) {
        androidx.compose.material3.MaterialTheme.colorScheme.surface
    } else {
        Color(0xFFF3F0FF)
    }

    // Combine default chatbots with custom characters
    val defaultChatbotsList = DefaultChatbots.get()

    val allChatbots = remember(customCharacters, defaultChatbotsList) {
        val customBots = customCharacters.map { profile ->
            ChatbotAvatar(
                id = "custom_${profile.id}",
                name = profile.name,
                emoji = profile.customAvatarPath ?: profile.gender.first().toString(),
                color = com.example.chatpart.Lavender,
                greeting = "Hi! I'm ${profile.name}, ${profile.relationship}! 👋",
                isCustomCharacter = true,
                profileId = profile.id,
                voiceId = profile.voiceId  // 携带克隆的声音 ID
            )
        }
        defaultChatbotsList + customBots
    }

    // Chatbot preferences - remember last selected bot
    val chatbotPreferences = remember { ChatbotPreferences(context) }
    val savedBotId = remember { chatbotPreferences.getSelectedBotId() }

    // Chatbot selection - restore from saved preference or default to first
    var selectedBot by remember {
        mutableStateOf(
            allChatbots.find { it.id == savedBotId }
                ?: allChatbots.firstOrNull()
                ?: defaultChatbotsList[0]
        )
    }
    var showAvatarPicker by remember { mutableStateOf(false) }

    // Helper function to resolve the correct Profile for the selected bot
    fun resolveProfile(): Profile? {
        return if (selectedBot.isCustomCharacter && selectedBot.profileId != null) {
            // From customCharacters list find the matching Profile
            customCharacters.find { it.id == selectedBot.profileId }
        } else {
            // Default bots (assistant/teacher/coding) use the passed currentProfile
            currentProfile
        }
    }

    // Navigate to specific bot when coming from History screen
    LaunchedEffect(targetBotId) {
        if (targetBotId != null) {
            val targetBot = allChatbots.find { it.id == targetBotId }
            if (targetBot != null) {
                selectedBot = targetBot
                showAvatarPicker = false
            }
        }
    }

    // Chat history manager for persistence
    val chatHistoryManager = remember { ChatHistoryManager(context) }

    // Load saved messages for the selected bot, or use default greeting
    var messages by remember(selectedBot) {
        mutableStateOf(run {
            val saved = chatHistoryManager.loadMessages(selectedBot.id)
            if (saved.isNotEmpty()) {
                saved.map {
                    ChatMessage(
                        id = it.id,
                        text = it.text,
                        isFromUser = it.isFromUser,
                        isVoice = it.isVoice,
                        voiceDurationSec = it.voiceDurationSec,
                        voiceFilePath = it.voiceFilePath,
                        subtitle = it.subtitle
                    )
                }
            } else {
                listOf(ChatMessage(text = selectedBot.greeting, isFromUser = false))
            }
        })
    }

    // Save messages whenever they change (per character)
    LaunchedEffect(messages, selectedBot) {
        val dataMessages = messages.map {
            ChatMessageData(
                id = it.id,
                text = it.text,
                isFromUser = it.isFromUser,
                isVoice = it.isVoice,
                voiceDurationSec = it.voiceDurationSec,
                voiceFilePath = it.voiceFilePath,
                subtitle = it.subtitle
            )
        }
        chatHistoryManager.saveMessages(selectedBot.id, dataMessages)
    }

    val listState = rememberLazyListState()

    // Input mode
    var inputText by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Chat history for AI
    var chatHistory by remember { mutableStateOf(listOf<Message>()) }

    // Voice recording
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    // Deepgram for STT, MiniMax for TTS
    val deepgramClient = remember { DeepgramAsrClient() }
    val audioClient = remember { MiniMaxAudioClient(context) }
    val mediaPlayer = remember { MediaPlayer() }
    var isTtsPlaying by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { mediaPlayer.release() }
    }

    // Permission
    var hasAudioPermission by remember { mutableStateOf(audioManager.hasPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundColor, gradientEnd)))
    ) {
        // ── Top App Bar with chatbot info ──
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showAvatarPicker = !showAvatarPicker }
                ) {
                    // Chatbot avatar
                    Surface(
                        shape = CircleShape,
                        color = selectedBot.color.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(selectedBot.emoji, fontSize = 22.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            selectedBot.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkText
                        )
                        Text(
                            "Online · Tap to change",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        // ── Avatar Picker ──
        AnimatedVisibility(visible = showAvatarPicker, enter = fadeIn(), exit = fadeOut()) {
            AvatarPickerBar(
                chatbots = allChatbots,
                selectedBot = selectedBot,
                onSelect = { bot ->
                    selectedBot = bot
                    chatbotPreferences.setSelectedBotId(bot.id)  // Save selection
                    showAvatarPicker = false
                    // Reset chat with new bot greeting
                    messages = listOf(ChatMessage(text = bot.greeting, isFromUser = false))
                }
            )
        }

        // ── Messages List ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    currentUser = currentUser,
                    botAvatar = selectedBot,
                    isDarkMode = isDarkMode,
                    showSubtitle = message.isSubtitleExpanded,
                    onToggleSubtitle = {
                        if (message.subtitle.isNullOrBlank()) {
                            // No subtitle yet — transcribe on demand
                            scope.launch {
                                val audioFile = message.voiceFilePath?.let { java.io.File(it) }
                                if (audioFile != null && audioFile.exists()) {
                                    val transcribed = withContext(Dispatchers.IO) {
                                        deepgramClient.transcribe(audioFile)
                                    }
                                    if (transcribed.isNotBlank()) {
                                        messages = messages.map { msg ->
                                            if (msg.id == message.id) msg.copy(subtitle = transcribed, isSubtitleExpanded = true)
                                            else msg
                                        }
                                    }
                                }
                            }
                        } else {
                            // Already have subtitle — just toggle visibility
                            messages = messages.map { msg ->
                                if (msg.id == message.id) msg.copy(isSubtitleExpanded = !msg.isSubtitleExpanded)
                                else msg
                            }
                        }
                    },
                    onTts = { text ->
                        // Check if TTS is already playing
                        if (isTtsPlaying) {
                            Toast.makeText(context, Languages.getString(currentLanguage, "TTS_PLAYING"), Toast.LENGTH_SHORT).show()
                            return@ChatBubble
                        }
                        scope.launch {
                            var tempFile: File? = null
                            try {
                                isTtsPlaying = true
                                val boost = when (currentLanguage) {
                                    "zh" -> "Chinese"
                                    "fr" -> "French"
                                    else -> ""
                                }
                                val filePath = audioClient.textToVoice(
                                    text = text,
                                    voiceId = voiceAssignmentPrefs?.getVoiceId(selectedBot.profileId ?: selectedBot.id)
                                        ?: selectedBot.voiceId
                                        ?: resolveProfile()?.voiceId
                                        ?: "English_Graceful_Lady",
                                    emotion = "calm",
                                    languageBoost = boost
                                )
                                tempFile = File(filePath)
                                mediaPlayer.reset()
                                mediaPlayer.setDataSource(filePath)
                                mediaPlayer.prepare()
                                mediaPlayer.setOnCompletionListener {
                                    isTtsPlaying = false
                                    mediaPlayer.reset()
                                    tempFile?.delete()  // Clean up cache after playback
                                }
                                mediaPlayer.start()
                            } catch (e: Exception) {
                                Log.e("MiniMaxTTS", "TTS failed: ${e.message}")
                                isTtsPlaying = false
                                tempFile?.delete()  // Clean up on error too
                            }
                        }
                    },
                    onStt = { voiceMessage ->
                        val filePath = voiceMessage.voiceFilePath
                        val file = filePath?.let { java.io.File(it) }
                        
                        if (file != null && file.exists()) {
                            // File exists — just play it
                            scope.launch {
                                try {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(filePath)
                                    withContext(Dispatchers.IO) { mediaPlayer.prepare() }
                                    mediaPlayer.setOnCompletionListener { mediaPlayer.reset() }
                                    mediaPlayer.start()
                                } catch (e: Exception) {
                                    Log.e("VoicePlayback", "Playback failed: ${e.message}")
                                }
                            }
                        } else {
                            // File is missing (maybe cleared because of voice change)! 
                            // AUTO-REGENERATE on demand with THE LATEST voice_id
                            scope.launch {
                                try {
                                    // 1. Indicate loading on this bubble (handled by refreshing the state)
                                    // 2. Generate with current voice settings
                                    val newFilePath = audioClient.textToVoice(
                                        text = voiceMessage.subtitle ?: voiceMessage.text,
                                        voiceId = voiceAssignmentPrefs?.getVoiceId(selectedBot.profileId ?: selectedBot.id)
                                            ?: selectedBot.voiceId
                                            ?: resolveProfile()?.voiceId
                                            ?: "English_Graceful_Lady",
                                        emotion = "happy",
                                        languageBoost = if (currentLanguage == "zh") "Chinese" else ""
                                    )
                                    
                                    // 3. Update the messages list so the new path is persisted
                                    messages = messages.map { msg ->
                                        if (msg.id == voiceMessage.id) msg.copy(voiceFilePath = newFilePath)
                                        else msg
                                    }
                                    
                                    // 4. Play the new file
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(newFilePath)
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()
                                } catch (e: Exception) {
                                    Log.e("VoiceRegeneration", "Failed to regenerate voice: ${e.message}")
                                    Toast.makeText(context, "Voice missing — reconstruction failed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }

        // ── Input Bar ──
        val inputBarColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = inputBarColor,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice/Keyboard toggle button
                    Surface(
                        onClick = {
                            isVoiceMode = !isVoiceMode
                            if (isVoiceMode && !hasAudioPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        shape = CircleShape,
                        color = if (isVoiceMode) Peach else Lavender,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                if (isVoiceMode) "⌨️" else "🎤",
                                fontSize = 20.sp
                            )
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    if (isVoiceMode) {
                        // ── WeChat-style hold-to-record button ──
                        VoiceRecordButton(
                            isRecording = isRecording,
                            modifier = Modifier.weight(1f),
                            currentLanguage = currentLanguage,
                            onStartRecording = {
                                triggerHapticFeedback()
                                if (hasAudioPermission) {
                                    val started = audioManager.startRecording()
                                    isRecording = started
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onCancelRecording = {
                                triggerHapticFeedback()
                                isRecording = false
                                val discarded = audioManager.stopRecording()
                                discarded?.delete() // 丢弃录音，不发送
                            },
                            onStopRecording = {
                                triggerHapticFeedback()
                                isRecording = false
                                val audioFile = audioManager.stopRecording()
                                if (audioFile != null && audioFile.exists()) {
                                    // Step 1: Add voice message bubble
                                    val voiceMsg = ChatMessage(
                                        text = "🎤 Voice message",
                                        isFromUser = true,
                                        isVoice = true,
                                        voiceDurationSec = (audioFile.length() / (16000 * 2)).toInt().coerceAtLeast(1),
                                        voiceFilePath = audioFile.absolutePath
                                    )
                                    messages = messages + voiceMsg

                                    // Step 2: Auto STT with Deepgram (like WeChat subtitle)
                                    scope.launch {
                                        isLoading = true
                                        val transcribedText = withContext(Dispatchers.IO) {
                                            deepgramClient.transcribe(audioFile)
                                        }

                                        if (transcribedText.isNotBlank()) {
                                            // Update bubble with subtitle (show below bubble)
                                            messages = messages.map { msg ->
                                                if (msg.id == voiceMsg.id) msg.copy(subtitle = transcribedText)
                                                else msg
                                            }

                                            // Step 3: Send transcribed text to Gemini
                                            val profile = resolveProfile()
                                            if (personChat != null && profile != null) {
                                                try {
                                                    val result = personChat.sendMessage(
                                                        p = profile,
                                                        history = chatHistory,
                                                        userText = transcribedText
                                                    )
                                                    // Update chat history with user text + AI response
                                                    chatHistory = chatHistory + Message(Role.USER, transcribedText) + Message(Role.ASSISTANT, result.replyText)
                                                    messages = messages + ChatMessage(
                                                        text = result.replyText,
                                                        isFromUser = false
                                                    )
                                                } catch (e: Exception) {
                                                    Log.e("ChatScreen", "AI Error: ${e.message}")
                                                    messages = messages + ChatMessage(
                                                        text = "Sorry, I encountered an error. Please try again.",
                                                        isFromUser = false
                                                    )
                                                }
                                            } else {
                                                // Fallback: just show user text and mock reply
                                                chatHistory = chatHistory + Message(Role.USER, transcribedText)
                                                messages = messages + ChatMessage(
                                                    text = "Thanks! I received your voice message: \"$transcribedText\"",
                                                    isFromUser = false
                                                )
                                            }
                                        } else {
                                            Toast.makeText(context, "Speech not recognized", Toast.LENGTH_SHORT).show()
                                        }
                                        isLoading = false
                                    }
                                }
                            }
                        )

                    } else {
                        // Theme-aware colors for text field
                        val textFieldBgColor = if (isDarkMode) Color(0xFF3D3D3D) else Color(0xFFF8F6FF)
                        val textFieldTextColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
                        val textFieldPlaceholderColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray
                        val textFieldBorderColor = if (isDarkMode) Color(0xFF555555) else Color.LightGray.copy(alpha = 0.5f)

                        // ── Text input ──
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text(Languages.getString(currentLanguage, "TYPE_MESSAGE"), color = textFieldPlaceholderColor) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Lavender,
                                unfocusedBorderColor = textFieldBorderColor,
                                focusedContainerColor = textFieldBgColor,
                                unfocusedContainerColor = textFieldBgColor,
                                focusedTextColor = textFieldTextColor,
                                unfocusedTextColor = textFieldTextColor,
                                cursorColor = Lavender
                            ),
                            maxLines = 3
                        )

                    }

                    Spacer(Modifier.width(4.dp))

                    // Send button (only in text mode)
                    if (!isVoiceMode) {
                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    val userMsg = inputText.trim()
                                    messages = messages + ChatMessage(text = userMsg, isFromUser = true)
                                    inputText = ""
                                    isLoading = true

                                    // Update chat history
                                    chatHistory = chatHistory + Message(Role.USER, userMsg)

                                    // Use real AI if personChat and profile are available
                                    val profile = resolveProfile()
                                    if (personChat != null && profile != null) {
                                        scope.launch {
                                            try {
                                                val result = personChat.sendMessage(
                                                    p = profile,
                                                    history = chatHistory,
                                                    userText = userMsg
                                                )
                                                messages = messages + ChatMessage(
                                                    text = result.replyText,
                                                    isFromUser = false
                                                )
                                                // Update chat history with AI response
                                                chatHistory = chatHistory + Message(Role.ASSISTANT, result.replyText)
                                                Log.d("ChatScreen", "AI Response: ${result.replyText}")
                                            } catch (e: Exception) {
                                                Log.e("ChatScreen", "AI Error: ${e.message}")
                                                messages = messages + ChatMessage(
                                                    text = "Sorry, I encountered an error. Please try again.",
                                                    isFromUser = false
                                                )
                                            }
                                            isLoading = false
                                        }
                                    } else {
                                        // Fallback to mock response if AI not available
                                        messages = messages + ChatMessage(
                                            text = "Thanks for your message! I received: \"$userMsg\"",
                                            isFromUser = false
                                        )
                                        isLoading = false
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isLoading) Color.Gray else Peach
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}

// ─── Avatar Picker Bar ──────────────────────────────────────────────────────

@Composable
fun AvatarPickerBar(
    chatbots: List<ChatbotAvatar>,
    selectedBot: ChatbotAvatar,
    onSelect: (ChatbotAvatar) -> Unit,
    onDelete: ((ChatbotAvatar) -> Unit)? = null
) {
    val context = LocalContext.current

    Surface(
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                LocalizedString("CHOOSE_CHATBOT"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatbots) { bot ->
                    val isSelected = bot.id == selectedBot.id
                    AvatarPickerItem(
                        bot = bot,
                        isSelected = isSelected,
                        context = context,
                        onClick = { onSelect(bot) },
                        onDelete = if (bot.isCustomCharacter && onDelete != null) {
                            { onDelete(bot) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarPickerItem(
    bot: ChatbotAvatar,
    isSelected: Boolean,
    context: android.content.Context,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Box {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isSelected) bot.color.copy(alpha = 0.12f) else Color(0xFFF5F5F5),
            border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(2.dp, bot.color)
            } else null,
            modifier = Modifier
                .clickable { onClick() }
                .width(80.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                // Check if it's a custom avatar (file path) or emoji
                val isEmoji = !bot.emoji.startsWith("/") &&
                              !bot.emoji.contains("avatar_") &&
                              !bot.emoji.contains(".jpg") &&
                              !bot.emoji.contains(".png") &&
                              !bot.emoji.contains(".webp") &&
                              bot.emoji.all {
                                  val c = it.code
                                  c in 0x2600..0x27BF ||  // Misc Symbols, Dingbats
                                  c in 0xD800..0xDFFF ||  // Surrogate pairs (emoji > U+FFFF)
                                  c == 0x200D ||           // ZWJ (Zero Width Joiner, e.g. 👩‍🏫)
                                  c == 0xFE0F ||           // Variation Selector-16
                                  c == 0x20E3              // Combining Enclosing Keycap
                              }

                if (isEmoji) {
                    // Emoji avatar
                    Text(bot.emoji, fontSize = 28.sp)
                } else {
                    // Custom image avatar
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(bot.emoji)
                            .crossfade(true)
                            .build(),
                        contentDescription = bot.name,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    bot.name,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) bot.color else DarkText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Delete button for custom characters
        if (bot.isCustomCharacter && onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Delete",
                    tint = Color.Red,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ─── WeChat-style Voice Record Button ───────────────────────────────────────

@Composable
fun VoiceRecordButton(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    currentLanguage: String = "en",
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit = {}
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // 拖拽状态（右滑取消）
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val cancelThresholdPx = with(density) { 80.dp.toPx() }
    val isCancelZone = dragOffsetX > cancelThresholdPx   // 右滑进入取消区

    val bgColor by animateColorAsState(
        targetValue = when {
            isCancelZone -> Color(0xFFFF4444).copy(alpha = 0.15f)  // 红色：取消
            isRecording  -> Color(0xFF4CAF50).copy(alpha = 0.15f)  // 绿色：录音中
            else         -> Color(0xFFF0F0F0)                      // 灰色：默认
        },
        label = "recordBg"
    )
    val textColor by animateColorAsState(
        targetValue = when {
            isCancelZone -> Color(0xFFFF4444)
            isRecording  -> Color(0xFF388E3C)
            else         -> Color.Gray
        },
        label = "recordText"
    )

    // 录音中脉冲动画（取消区内暂停脉冲）
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isCancelZone) 1.03f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulseScale"
    )
    // 箭头提示闪烁
    val arrowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "arrowAlpha"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        modifier = modifier
            .height(48.dp)
            .scale(pulseScale)
            .pointerInput(Unit) {
                awaitEachGesture {
                    // ① 按下 → 开始录音
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onStartRecording()
                    dragOffsetX = 0f
                    var wasInCancelZone = false

                    // ② 追踪手指横向移动
                    while (true) {
                        val event = awaitPointerEvent()
                        val ptr = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ptr.pressed) { ptr.consume(); break }
                        ptr.consume()
                        dragOffsetX = (ptr.position.x - down.position.x).coerceIn(-50f, 400f)

                        // 进入/离开取消区各震动一次
                        val inCancel = dragOffsetX > cancelThresholdPx
                        if (inCancel != wasInCancelZone) {
                            wasInCancelZone = inCancel
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }

                    // ③ 松手：取消区 → 取消；正常区 → 发送
                    val shouldCancel = dragOffsetX > cancelThresholdPx
                    dragOffsetX = 0f
                    if (shouldCancel) onCancelRecording() else onStopRecording()
                }
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (!isRecording) {
                // 空闲：文字居中
                Text(
                    text = Languages.getString(currentLanguage, "HOLD_TO_TALK"),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 录音中：左侧点+文字，右侧箭头/垃圾桶
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RecordingIndicator(isCancelZone = isCancelZone)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isCancelZone)
                                Languages.getString(currentLanguage, "LIVE_RELEASE_TO_CANCEL")
                            else
                                Languages.getString(currentLanguage, "RELEASE_TO_SEND"),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = textColor
                        )
                    }
                    if (isCancelZone) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "→ →",
                            color = Color(0xFF388E3C).copy(alpha = arrowAlpha),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingIndicator(isCancelZone: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotColor = if (isCancelZone) Color(0xFFFF4444) else Color(0xFF4CAF50)

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = alpha))
            )
        }
    }
}

// ─── Chat Bubble ────────────────────────────────────────────────────────────

@Composable
fun ChatBubble(
    message: ChatMessage,
    currentUser: FirebaseUser? = null,
    botAvatar: ChatbotAvatar? = null,
    isDarkMode: Boolean = false,
    showSubtitle: Boolean = false,
    onToggleSubtitle: (() -> Unit)? = null,
    onTts: ((String) -> Unit)? = null,
    onStt: ((ChatMessage) -> Unit)? = null
) {
    val defaultBot = botAvatar ?: DefaultChatbots.get().firstOrNull() ?: ChatbotAvatar(
        id = "assistant",
        name = "AI Assistant",
        emoji = "🤖",
        color = Color(0xFF7C5CFC),
        greeting = "Hi!",
        isCustomCharacter = false
    )
    val isUser = message.isFromUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    // Theme-aware colors
    val userBubbleColor = Brush.horizontalGradient(listOf(Peach, Peach.copy(alpha = 0.8f)))
    val botBubbleColorLight = Brush.horizontalGradient(listOf(Color.White, Color(0xFFF8F6FF)))
    val botBubbleColorDark = Brush.horizontalGradient(listOf(Color(0xFF2D2D2D), Color(0xFF3D3D3D)))

    val bubbleColor = if (isUser) {
        userBubbleColor
    } else {
        if (isDarkMode) botBubbleColorDark else botBubbleColorLight
    }

    val textColor = if (isUser) Color.White else if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF2D2D2D)
    val bubbleShape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = alignment
        ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            // Bot avatar (left side)
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = defaultBot.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(defaultBot.emoji, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            // Message bubble + CC toggle + subtitle — tightly grouped in a Column
            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                Surface(
                    shape = bubbleShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .then(
                            if (message.isVoice && onStt != null)
                                Modifier.clickable { onStt(message) }
                            else Modifier
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .background(bubbleColor)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (message.isVoice) {
                            // Voice message display
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                // Waveform bars
                                VoiceWaveform(
                                    color = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .width((message.voiceDurationSec * 30).coerceIn(40, 120).dp)
                                        .height(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${message.voiceDurationSec}\"",
                                    color = textColor.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Text(
                                text = message.text,
                                color = textColor,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // CC toggle button — shown for all voice messages (transcribes on demand if needed)
                if (isUser && message.isVoice && onToggleSubtitle != null) {
                    IconButton(
                        onClick = { onToggleSubtitle() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showSubtitle) Icons.Rounded.Subtitles else Icons.Rounded.SubtitlesOff,
                            contentDescription = if (showSubtitle) "Hide subtitle" else "Show subtitle",
                            tint = if (showSubtitle) Lavender else Color.Gray.copy(alpha = 0.45f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Subtitle — secondary style: small, muted, with subtle background
                // NOTE: subtitle sits outside the bubble (on chat background), so use
                // chat-background-contrast color, NOT textColor (which is bubble-contrast)
                if (message.isVoice && !message.subtitle.isNullOrBlank() && showSubtitle) {
                    val subtitleTextColor = if (isDarkMode) Color(0xFFBBBBBB) else Color(0xFF555555)
                    val subtitleBgColor = if (isDarkMode)
                        Color.White.copy(alpha = 0.08f)
                    else
                        Color.Black.copy(alpha = 0.05f)
                    Box(
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .background(subtitleBgColor, shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = message.subtitle!!,
                            color = subtitleTextColor,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // User avatar (right side)
            if (isUser) {
                Spacer(Modifier.width(8.dp))
                if (currentUser?.photoUrl != null) {
                    AsyncImage(
                        model = currentUser.photoUrl.toString(),
                        contentDescription = "Your avatar",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = Peach.copy(alpha = 0.2f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                currentUser?.displayName?.firstOrNull()?.uppercase() ?: "U",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Peach
                            )
                        }
                    }
                }
            }
        }
        }
        if (!isUser && onTts != null && !message.isVoice) {
            IconButton(
                onClick = { onTts(message.text) },
                modifier = Modifier.padding(start = 36.dp).size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.VolumeUp,
                    contentDescription = "Read aloud",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ─── Character List Screen ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    isDarkMode: Boolean,
    characters: List<Profile>,
    selectedCharacterId: String,
    onSelectCharacter: (Profile) -> Unit,
    onCreateNew: () -> Unit,
    onEdit: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onBack: () -> Unit,
    onVoiceCall: (Profile) -> Unit
) {
    val backgroundColor = if (isDarkMode) Color(0xFF1A1A2E) else Color(0xFFFFFBFE)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D44) else Color.White
    val onSurfaceColor = if (isDarkMode) Color.White else Color(0xFF1C1B1F)
    val primaryColor = Lavender

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocalizedString("CHARACTERS"),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = LocalizedString("BACK")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = onSurfaceColor
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNew,
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(LocalizedString("CREATE_NEW"))
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(characters) { character ->
                CharacterListItem(
                    character = character,
                    isSelected = character.id == selectedCharacterId,
                    onClick = { onSelectCharacter(character) },
                    onEdit = { onEdit(character) },
                    onDelete = { onDelete(character) },
                    onVoiceCall = { onVoiceCall(character) },
                    surfaceColor = surfaceColor,
                    onSurfaceColor = onSurfaceColor,
                    primaryColor = primaryColor,
                    isDarkMode = isDarkMode
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterListItem(
    character: Profile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVoiceCall: () -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color,
    primaryColor: Color,
    isDarkMode: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryColor.copy(alpha = 0.12f) else surfaceColor
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, primaryColor) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = character.name.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = primaryColor
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )
                Text(
                    text = "${character.gender} · ${character.relationship}",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceColor.copy(alpha = 0.7f)
                )
            }

            // Voice call button
            IconButton(onClick = onVoiceCall) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = LocalizedString("VOICE_CALL"),
                    tint = primaryColor
                )
            }

            // Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = LocalizedString("MORE_OPTIONS"),
                        tint = onSurfaceColor.copy(alpha = 0.6f)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(LocalizedString("EDIT")) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalizedString("DELETE")) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    )
                }
            }
        }
    }
}

// ─── Voice Waveform Visual ──────────────────────────────────────────────────

@Composable
fun VoiceWaveform(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val barCount = 12
        val barWidth = size.width / (barCount * 2f)
        val maxHeight = size.height

        for (i in 0 until barCount) {
            // Create a wave pattern
            val height = maxHeight * when {
                i < 2 || i > barCount - 3 -> 0.3f
                i % 3 == 0 -> 0.8f
                i % 2 == 0 -> 0.5f
                else -> 0.65f
            }
            val x = (i * 2 + 1) * barWidth
            drawLine(
                color = color,
                start = Offset(x, (maxHeight - height) / 2),
                end = Offset(x, (maxHeight + height) / 2),
                strokeWidth = barWidth * 0.8f
            )
        }
    }
}
