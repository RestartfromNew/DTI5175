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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
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
import com.example.chatpart.data.DefaultVoice
import com.example.chatpart.data.DefaultVoices
import com.example.chatpart.data.SlotStatus
import com.example.chatpart.domain.Profile
import com.example.chatpart.firestore.FirestoreError
import com.example.chatpart.firestore.UserVoiceManager
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
    userVoiceManager: UserVoiceManager,
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

    val translate: (String) -> String = { key -> Languages.getString(currentLanguage, key) }

    // Tab state
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(translate("CLONED_VOICES"), translate("DEFAULT_VOICES"))

    // Load characters with voiceId
    var charactersWithVoice by remember { mutableStateOf<List<Profile>>(emptyList()) }
    // API voices: voice_ids that exist on MiniMax server but may not be in local storage
    var apiOnlyVoiceIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Profile?>(null) }
    var showDeleteApiVoiceDialog by remember { mutableStateOf<String?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Slot status from Firestore
    var slotStatus by remember { mutableStateOf<SlotStatus?>(null) }
    var slotStatusError by remember { mutableStateOf<String?>(null) }

    // Audio player
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // MiniMax clients
    val audioClient = remember { MiniMaxAudioClient(context) }
    val voiceCloneManager = remember { com.example.chatpart.api.MiniMaxVoiceCloneManager(context) }
    val scope = rememberCoroutineScope()

    // Load local characters that have a voiceId
    fun loadCharacters() {
        charactersWithVoice = characterStorage.loadCharacters().filter { it.voiceId != null }
    }

    // Fetch all cloned voices from MiniMax API and find any not in local storage
    fun syncFromApi() {
        scope.launch {
            isSyncing = true
            syncError = null
            voiceCloneManager.fetchClonedVoices()
                .onSuccess { apiVoiceIds ->
                    val localVoiceIds = characterStorage.loadCharacters()
                        .mapNotNull { it.voiceId }.toSet()
                    // Only keep IDs that aren't already linked to a local character
                    apiOnlyVoiceIds = apiVoiceIds.filter { it !in localVoiceIds }
                    syncError = null
                }
                .onFailure { e ->
                    Log.w("VoiceManagement", "API sync failed (offline?): ${e.message}")
                    syncError = "Failed to sync from server"
                    // Show snackbar with retry
                    snackbarHostState.showSnackbar(
                        message = "Sync failed: ${e.message ?: "Network error"}",
                        actionLabel = "Retry",
                        duration = SnackbarDuration.Long
                    )
                }
            isSyncing = false
        }
    }

    LaunchedEffect(Unit) {
        loadCharacters()
        syncFromApi()
        // Listen to slot status changes from Firestore
        userVoiceManager.observeSlotStatus().collect { result ->
            result.onSuccess { status ->
                slotStatus = status
                slotStatusError = null
            }.onFailure { error ->
                slotStatusError = userVoiceManager.getUserFriendlyErrorMessage(error)
                Log.e("VoiceManagement", "Slot status error: ${error.message}")
            }
        }
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
                errorMessage = translate("VOICE_ERROR")
                playingVoiceId = null
            } finally {
                isLoading = false
            }
        }
    }

    // Delete voice function
    fun deleteVoice(profile: Profile) {
        scope.launch {
            deleteError = null
            profile.voiceId?.let { voiceId ->
                userVoiceManager.deleteClonedVoice(voiceId)
                    .onSuccess {
                        // Also delete from MiniMax
                        voiceCloneManager.deleteVoice(voiceId)
                            .onSuccess {
                                Log.d("VoiceManagement", "Successfully deleted from both Firestore and MiniMax: $voiceId")
                            }
                            .onFailure { miniMaxError ->
                                Log.w("VoiceManagement", "Failed to delete from MiniMax: ${miniMaxError.message}")
                                // Still consider it a success if Firestore delete succeeded
                            }

                        // Update local storage after successful Firestore delete
                        val updatedProfile = profile.copy(voiceId = null)
                        characterStorage.updateCharacter(updatedProfile)
                        loadCharacters()
                        onVoicesChanged()
                    }
                    .onFailure { e ->
                        Log.e("VoiceManagement", "Failed to delete from Firestore: ${e.message}")
                        deleteError = userVoiceManager.getUserFriendlyErrorMessage(e)

                        // Show snackbar with retry option
                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = "Failed to delete: $deleteError",
                            actionLabel = "Retry",
                            duration = SnackbarDuration.Long
                        )
                        when (snackbarResult) {
                            SnackbarResult.ActionPerformed -> {
                                // Retry delete
                                deleteVoice(profile)
                            }
                            SnackbarResult.Dismissed -> {
                                // User dismissed - do NOT proceed with local deletion
                                // Only proceed if Firestore delete succeeded
                                deleteError = "Delete failed: $deleteError"
                            }
                        }
                    }
            } ?: run {
                // No voiceId, just update local
                val updatedProfile = profile.copy(voiceId = null)
                characterStorage.updateCharacter(updatedProfile)
                loadCharacters()
                onVoicesChanged()
            }
        }
    }

    // Delete API-only voice (exists on MiniMax/Firestore but not linked to local character)
    fun deleteApiOnlyVoice(voiceId: String) {
        scope.launch {
            deleteError = null
            userVoiceManager.deleteClonedVoice(voiceId)
                .onSuccess {
                    // Also delete from MiniMax
                    voiceCloneManager.deleteVoice(voiceId)
                        .onSuccess {
                            Log.d("VoiceManagement", "Successfully deleted from both Firestore and MiniMax: $voiceId")
                        }
                        .onFailure { miniMaxError ->
                            Log.w("VoiceManagement", "Failed to delete from MiniMax: ${miniMaxError.message}")
                            // Still consider it a success if Firestore delete succeeded
                        }

                    apiOnlyVoiceIds = apiOnlyVoiceIds.filter { it != voiceId }
                    onVoicesChanged()
                }
                .onFailure { e ->
                    Log.e("VoiceManagement", "Failed to delete API voice from Firestore: ${e.message}")
                    deleteError = userVoiceManager.getUserFriendlyErrorMessage(e)
                    val snackbarResult = snackbarHostState.showSnackbar(
                        message = "Failed to delete: $deleteError",
                        actionLabel = "Retry",
                        duration = SnackbarDuration.Long
                    )
                    when (snackbarResult) {
                        SnackbarResult.ActionPerformed -> deleteApiOnlyVoice(voiceId)
                        SnackbarResult.Dismissed -> deleteError = "Delete failed: $deleteError"
                    }
                }
        }
    }

    // Play default voice function (for preset voices)
    fun playDefaultVoice(voice: DefaultVoice) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                playingVoiceId = voice.id

                // Stop any currently playing audio
                mediaPlayer?.release()

                // Generate TTS with the default voice
                val testText = when (voice.language) {
                    "zh" -> "你好，这是默认声音。"
                    "fr" -> "Bonjour, c'est une voix par défaut."
                    "ja" -> "こんにちは、これがデフォルト音声です。"
                    "ko" -> "안녕하세요, 기본 음성입니다."
                    "es" -> "Hola, esta es la voz predeterminada."
                    else -> "Hello, this is a default voice."
                }

                val audioPath = withContext(Dispatchers.IO) {
                    audioClient.textToVoice(
                        text = testText,
                        voiceId = voice.id,
                        emotion = "calm",
                        languageBoost = voice.language
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
                Log.e("VoiceManagement", "Failed to play default voice: ${e.message}")
                errorMessage = translate("VOICE_ERROR")
                playingVoiceId = null
            } finally {
                isLoading = false
            }
        }
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
                        text = translate("MANAGE_VOICES"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                // Refresh button to sync from MiniMax API
                IconButton(onClick = { loadCharacters(); syncFromApi() }) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Peach,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Sync from MiniMax",
                            tint = Peach
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = surfaceColor,
                contentColor = Peach
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) Peach else subtitleColor
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Cloned Voices Tab
                        ClonedVoicesTab(
                            charactersWithVoice = charactersWithVoice,
                            apiOnlyVoiceIds = apiOnlyVoiceIds,
                            playingVoiceId = playingVoiceId,
                            isLoading = isLoading,
                            isDarkMode = isDarkMode,
                            onPlayClick = { voiceId -> playVoice(voiceId) },
                            onDeleteClick = { profile -> showDeleteDialog = profile },
                            onDeleteApiVoiceClick = { voiceId -> showDeleteApiVoiceDialog = voiceId },
                            showDeleteDialog = showDeleteDialog,
                            onDismissDelete = { showDeleteDialog = null },
                            onConfirmDelete = {
                                deleteVoice(showDeleteDialog!!)
                                showDeleteDialog = null
                            },
                            t = translate,
                            subtitleColor = subtitleColor,
                            surfaceColor = surfaceColor,
                            textColor = textColor
                        )

                        // Delete confirmation dialog for API-only voices
                        if (showDeleteApiVoiceDialog != null) {
                            AlertDialog(
                                onDismissRequest = { showDeleteApiVoiceDialog = null },
                                title = { Text(translate("DELETE")) },
                                text = { Text(translate("CONFIRM_DELETE_VOICE")) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        deleteApiOnlyVoice(showDeleteApiVoiceDialog!!)
                                        showDeleteApiVoiceDialog = null
                                    }) {
                                        Text(translate("DELETE"), color = Color(0xFFE53935))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteApiVoiceDialog = null }) {
                                        Text(translate("CANCEL"))
                                    }
                                }
                            )
                        }
                    }
                    1 -> {
                        // Default Voices Tab
                        DefaultVoicesTab(
                            isDarkMode = isDarkMode,
                            playingVoiceId = playingVoiceId,
                            isLoading = isLoading,
                            onPlayClick = { voice -> playDefaultVoice(voice) },
                            t = translate,
                            subtitleColor = subtitleColor,
                            surfaceColor = surfaceColor,
                            textColor = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clone new voice button (only show in Cloned Voices tab)
            if (selectedTab == 0) {
                // Slot status indicator
                if (slotStatus != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice Slots: ${slotStatus!!.used}/${slotStatus!!.limit}",
                            fontSize = 12.sp,
                            color = if (slotStatus!!.isFull) Color(0xFFE53935) else subtitleColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        LinearProgressIndicator(
                            progress = { slotStatus!!.percentage },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp),
                            color = if (slotStatus!!.isFull) Color(0xFFE53935) else Peach,
                            trackColor = subtitleColor.copy(alpha = 0.2f),
                        )
                    }
                }

                Button(
                    onClick = onNavigateToVoiceClone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (slotStatus?.isFull == true) subtitleColor else Peach,
                        contentColor = Color.White
                    ),
                    enabled = slotStatus?.isFull != true
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (slotStatus?.isFull == true) {
                            "Slot Full - Delete a voice first"
                        } else {
                            translate("CLONE_FIRST_VOICE")
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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

// Card for voices that exist on MiniMax API but aren't linked to a local character
@Composable
private fun ApiOnlyVoiceItem(
    voiceId: String,
    isDarkMode: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    subtitleColor: Color,
    surfaceColor: Color,
    textColor: Color
) {
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
            Icon(
                imageVector = Icons.Rounded.RecordVoiceOver,
                contentDescription = null,
                tint = subtitleColor,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = voiceId,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "From MiniMax · not linked to a character",
                    fontSize = 11.sp,
                    color = subtitleColor
                )
            }
            IconButton(onClick = onPlayClick, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Peach,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isPlaying) Peach else textColor
                    )
                }
            }
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

@Composable
private fun VoiceItem(
    profile: Profile,
    isDarkMode: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    subtitleColor: Color,
    surfaceColor: Color,
    textColor: Color
) {
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

@Composable
private fun ClonedVoicesTab(
    charactersWithVoice: List<Profile>,
    apiOnlyVoiceIds: List<String>,
    playingVoiceId: String?,
    isLoading: Boolean,
    isDarkMode: Boolean,
    onPlayClick: (String) -> Unit,
    onDeleteClick: (Profile) -> Unit,
    onDeleteApiVoiceClick: (String) -> Unit,
    showDeleteDialog: Profile?,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    t: (String) -> String,
    subtitleColor: Color,
    surfaceColor: Color,
    textColor: Color
) {
    val totalCount = charactersWithVoice.size + apiOnlyVoiceIds.size

    // Voice count subtitle
    Text(
        text = t("VOICE_COUNT").replace("{n}", totalCount.toString()),
        fontSize = 14.sp,
        color = subtitleColor,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )

    if (totalCount == 0) {
        // Empty state
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Local characters with voiceId
            items(charactersWithVoice, key = { it.id }) { profile ->
                VoiceItem(
                    profile = profile,
                    isDarkMode = isDarkMode,
                    isPlaying = playingVoiceId == profile.voiceId,
                    isLoading = isLoading && playingVoiceId == profile.voiceId,
                    onPlayClick = { profile.voiceId?.let { onPlayClick(it) } },
                    onDeleteClick = { onDeleteClick(profile) },
                    subtitleColor = subtitleColor,
                    surfaceColor = surfaceColor,
                    textColor = textColor
                )
            }

            // API-only voices: exist on MiniMax but not linked to any local character
            if (apiOnlyVoiceIds.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "· MiniMax Server ·",
                        fontSize = 12.sp,
                        color = subtitleColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
                items(apiOnlyVoiceIds, key = { it }) { voiceId ->
                    ApiOnlyVoiceItem(
                        voiceId = voiceId,
                        isDarkMode = isDarkMode,
                        isPlaying = playingVoiceId == voiceId,
                        isLoading = isLoading && playingVoiceId == voiceId,
                        onPlayClick = { onPlayClick(voiceId) },
                        onDeleteClick = { onDeleteApiVoiceClick(voiceId) },
                        subtitleColor = subtitleColor,
                        surfaceColor = surfaceColor,
                        textColor = textColor
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(t("DELETE")) },
            text = { Text(t("CONFIRM_DELETE_VOICE")) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete
                ) {
                    Text(t("DELETE"), color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) {
                    Text(t("CANCEL"))
                }
            }
        )
    }
}

@Composable
private fun DefaultVoicesTab(
    isDarkMode: Boolean,
    playingVoiceId: String?,
    isLoading: Boolean,
    onPlayClick: (DefaultVoice) -> Unit,
    t: (String) -> String,
    subtitleColor: Color,
    surfaceColor: Color,
    textColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(DefaultVoices.voices) { voice ->
            DefaultVoiceItem(
                voice = voice,
                isDarkMode = isDarkMode,
                isPlaying = playingVoiceId == voice.id,
                isLoading = isLoading && playingVoiceId == voice.id,
                onPlayClick = { onPlayClick(voice) },
                t = t,
                subtitleColor = subtitleColor,
                surfaceColor = surfaceColor,
                textColor = textColor
            )
        }
    }
}

@Composable
private fun DefaultVoiceItem(
    voice: DefaultVoice,
    isDarkMode: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayClick: () -> Unit,
    t: (String) -> String,
    subtitleColor: Color,
    surfaceColor: Color,
    textColor: Color
) {
    val genderText = if (voice.gender == "male") t("VOICE_MALE") else t("VOICE_FEMALE")

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
                    text = voice.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = "${Languages.getTtsLanguageByCode(voice.language)?.nativeName ?: voice.language} - $genderText",
                    fontSize = 12.sp,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = voice.description,
                    fontSize = 11.sp,
                    color = subtitleColor.copy(alpha = 0.7f),
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
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = t("VOICE_PREVIEW"),
                        tint = if (isPlaying) Peach else textColor
                    )
                }
            }
        }
    }
}
