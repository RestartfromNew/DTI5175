package com.example.chatpart.screens

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.Peach
import com.example.chatpart.SoftWhite
import com.example.chatpart.api.MiniMaxAudioClient
import com.example.chatpart.data.CharacterStorage
import com.example.chatpart.domain.Profile
import com.example.chatpart.i18n.LanguageManager
import com.example.chatpart.i18n.Languages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VoiceManagementScreen(
    isDarkMode: Boolean = false,
    currentLanguage: String = "en",
    characterStorage: CharacterStorage,
    onNavigateToVoiceClone: () -> Unit,
    onBack: () -> Unit,
    onVoicesChanged: () -> Unit = {}
) {
    // Theme-aware colors
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else SoftWhite
    val gradientEnd = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF3F0FF)
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray

    val context = LocalContext.current
    val languageManager = remember { LanguageManager(context) }
    val currentLang = languageManager.getCurrentLanguage()

    fun t(key: String) = Languages.getString(currentLanguage, key)

    // Load characters with voiceId
    var charactersWithVoice by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Profile?>(null) }

    // Audio player
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // MiniMax Audio Client
    val audioClient = remember { MiniMaxAudioClient(context) }
    val scope = rememberCoroutineScope()

    // Load characters
    fun loadCharacters() {
        charactersWithVoice = characterStorage.loadCharacters().filter { it.voiceId != null }
    }

    LaunchedEffect(Unit) {
        loadCharacters()
    }

    // Clean up MediaPlayer on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Play voice function
    fun playVoice(voiceId: String) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                playingVoiceId = voiceId

                // Stop any currently playing audio
                mediaPlayer?.release()

                // Generate TTS with the cloned voice
                val testText = when (currentLang) {
                    "zh" -> "你好，这是我的声音。"
                    "fr" -> "Bonjour, c'est ma voix."
                    else -> "Hello, this is my voice."
                }

                val audioPath = withContext(Dispatchers.IO) {
                    audioClient.textToVoice(
                        text = testText,
                        voiceId = voiceId,
                        emotion = "calm",
                        languageBoost = if (currentLang == "zh") "zh" else "en"
                    )
                }

                // Play the generated audio
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioPath)
                    prepare()
                    setOnCompletionListener {
                        playingVoiceId = null
                        release()
                    }
                    start()
                }

            } catch (e: Exception) {
                Log.e("VoiceManagement", "Failed to play voice: ${e.message}")
                errorMessage = t("VOICE_ERROR")
                playingVoiceId = null
            } finally {
                isLoading = false
            }
        }
    }

    // Delete voice function
    fun deleteVoice(profile: Profile) {
        val updatedProfile = profile.copy(voiceId = null)
        characterStorage.updateCharacter(updatedProfile)
        loadCharacters()
        onVoicesChanged()
    }

    // Delete confirmation dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(t("DELETE")) },
            text = { Text(t("CONFIRM_DELETE_VOICE")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteVoice(showDeleteDialog!!)
                        showDeleteDialog = null
                    }
                ) {
                    Text(t("DELETE"), color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(t("CANCEL"))
                }
            }
        )
    }

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
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
                
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t("MANAGE_VOICES"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(end = 48.dp) // Offset the back button width to center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice count subtitle
            Text(
                text = t("VOICE_COUNT").replace("{n}", charactersWithVoice.size.toString()),
                fontSize = 14.sp,
                color = subtitleColor,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (charactersWithVoice.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RecordVoiceOver,
                            contentDescription = null,
                            tint = subtitleColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = t("NO_VOICES"),
                            fontSize = 16.sp,
                            color = subtitleColor
                        )
                    }
                }
            } else {
                // Voice list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(charactersWithVoice, key = { it.id }) { profile ->
                        VoiceItem(
                            profile = profile,
                            isDarkMode = isDarkMode,
                            isPlaying = playingVoiceId == profile.voiceId,
                            isLoading = isLoading && playingVoiceId == profile.voiceId,
                            onPlayClick = { profile.voiceId?.let { playVoice(it) } },
                            onDeleteClick = { showDeleteDialog = profile }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clone new voice button
            Button(
                onClick = onNavigateToVoiceClone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Peach,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = t("CLONE_FIRST_VOICE"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Error message
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    fontSize = 14.sp,
                    color = Color(0xFFE53935),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceItem(
    profile: Profile,
    isDarkMode: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val surfaceColor = if (isDarkMode) Color(0xFF2D2D2D) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF2D2D2D)
    val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice icon
            Icon(
                imageVector = Icons.Rounded.RecordVoiceOver,
                contentDescription = null,
                tint = Peach,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Voice info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = profile.voiceId ?: "",
                    fontSize = 12.sp,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play button
            IconButton(
                onClick = onPlayClick,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Peach,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.PlayArrow else Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isPlaying) Peach else textColor
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFE53935)
                )
            }
        }
    }
}
