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
import com.example.chatpart.api.MiniMaxVoiceCloneManager
import com.example.chatpart.data.CharacterStorage
import com.example.chatpart.data.DefaultVoice
import com.example.chatpart.data.DefaultVoices
import com.example.chatpart.domain.Profile
import com.example.chatpart.i18n.LanguageManager
import com.example.chatpart.i18n.Languages
import com.example.chatpart.voice.SlotUsageSummary
import com.example.chatpart.voice.VoiceSlot
import com.example.chatpart.voice.VoiceSlotManager
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

    val translate: (String) -> String = { key -> Languages.getString(currentLanguage, key) }

    // Tab state
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(translate("CLONED_VOICES"), translate("DEFAULT_VOICES"))

    // Load characters with voiceId
    var charactersWithVoice by remember { mutableStateOf<List<Profile>>(emptyList()) }
    // API voices: voice_ids that exist on MiniMax server but may not be in local storage
    var apiOnlyVoiceIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Profile?>(null) }

    // Voice Slot 状态 (新增)
    var slotUsage by remember { mutableStateOf<SlotUsageSummary?>(null) }
    val totalSlots = 1 // 默认 1 slot，可扩展

    // Audio player
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // MiniMax clients
    val audioClient = remember { MiniMaxAudioClient(context) }
    val voiceCloneManager = remember { MiniMaxVoiceCloneManager(context) }
    val voiceSlotManager = remember { VoiceSlotManager(context, voiceCloneManager) }
    val scope = rememberCoroutineScope()

    // Load local characters that have a voiceId
    fun loadCharacters() {
        charactersWithVoice = characterStorage.loadCharacters().filter { it.voiceId != null }
    }

    // Fetch all cloned voices from MiniMax API and find any not in local storage
    fun syncFromApi() {
        scope.launch {
            isSyncing = true
            voiceCloneManager.fetchClonedVoices()
                .onSuccess { apiVoiceIds ->
                    val localVoiceIds = characterStorage.loadCharacters()
                        .mapNotNull { it.voiceId }.toSet()
                    // Only keep IDs that aren't already linked to a local character
                    apiOnlyVoiceIds = apiVoiceIds.filter { it !in localVoiceIds }
                }
                .onFailure { e ->
                    Log.w("VoiceManagement", "API sync failed (offline?): ${e.message}")
                }
            isSyncing = false
        }
    }

    // 加载 Voice Slot 使用情况 (新增)
    fun loadSlotUsage() {
        scope.launch {
            try {
                // TODO: 暂时使用 placeholder userIdentifier，后端提供 dbUserId 后替换
                val userIdentifier = voiceSlotManager.getUserIdentifier(
                    googleUserId = "placeholder_google",
                    firebaseUid = "placeholder_firebase"
                )
                slotUsage = voiceSlotManager.getSlotUsage(userIdentifier, totalSlots)
            } catch (e: Exception) {
                Log.e("VoiceManagement", "Failed to load slot usage: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCharacters()
        syncFromApi()
        loadSlotUsage()
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

    // Delete voice function (修改: 同时删除 MiniMax 服务器上的 voice)
    fun deleteVoice(profile: Profile) {
        scope.launch {
            try {
                // TODO: 暂时使用 placeholder userIdentifier，后端提供 dbUserId 后替换
                val userIdentifier = voiceSlotManager.getUserIdentifier(
                    googleUserId = "placeholder_google",
                    firebaseUid = "placeholder_firebase"
                )

                // 如果 profile 有 voiceId，删除 MiniMax 服务器上的 voice
                if (profile.voiceId != null) {
                    voiceCloneManager.deleteMiniMaxVoice(profile.voiceId)
                }

                // 找到对应的 slot 并删除
                val slots = voiceSlotManager.getUserSlots(userIdentifier)
                val slot = slots.find { it.voiceId == profile.voiceId }
                if (slot != null) {
                    voiceSlotManager.deleteSlotVoice(slot.slotId, userIdentifier)
                }

                // 更新本地 character
                val updatedProfile = profile.copy(voiceId = null)
                characterStorage.updateCharacter(updatedProfile)
                loadCharacters()
                loadSlotUsage()
                onVoicesChanged()
            } catch (e: Exception) {
                Log.e("VoiceManagement", "Delete voice failed: ${e.message}")
                errorMessage = translate("DELETE_FAILED")
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
                            slotUsage = slotUsage,
                            playingVoiceId = playingVoiceId,
                            isLoading = isLoading,
                            isDarkMode = isDarkMode,
                            onPlayClick = { voiceId -> playVoice(voiceId) },
                            onDeleteClick = { profile -> showDeleteDialog = profile },
                            showDeleteDialog = showDeleteDialog,
                            onDismissDelete = { showDeleteDialog = null },
                            onConfirmDelete = {
                                deleteVoice(showDeleteDialog!!)
                                showDeleteDialog = null
                            },
                            onNavigateToVoiceClone = onNavigateToVoiceClone,
                            t = translate,
                            subtitleColor = subtitleColor,
                            surfaceColor = surfaceColor,
                            textColor = textColor
                        )
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

// Card for voices that exist on MiniMax API but aren't linked to a local character
@Composable
private fun ApiOnlyVoiceItem(
    voiceId: String,
    isDarkMode: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayClick: () -> Unit,
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
    slotUsage: SlotUsageSummary?,
    playingVoiceId: String?,
    isLoading: Boolean,
    isDarkMode: Boolean,
    onPlayClick: (String) -> Unit,
    onDeleteClick: (Profile) -> Unit,
    showDeleteDialog: Profile?,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onNavigateToVoiceClone: () -> Unit,
    t: (String) -> String,
    subtitleColor: Color,
    surfaceColor: Color,
    textColor: Color
) {
    val usedSlots = slotUsage?.usedSlots ?: 0
    val totalSlots = slotUsage?.totalSlots ?: 1
    val isFull = usedSlots >= totalSlots

    // MiniMax 服务器 X/Y 显示
    Text(
        text = "────────── · MiniMax 服务器 $usedSlots/$totalSlots · ──────────",
        fontSize = 12.sp,
        color = subtitleColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )

    if (usedSlots == 0 && apiOnlyVoiceIds.isEmpty()) {
        // Empty state - 没有克隆的声音
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(),
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

        // 克隆第一个声音按钮
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
    } else {
        // 有声音时的列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 本地角色关联的声音
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
                        text = "· MiniMax 服务器 ·",
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
                        subtitleColor = subtitleColor,
                        surfaceColor = surfaceColor,
                        textColor = textColor
                    )
                }
            }
        }

        // 满了时的提示和购买按钮
        if (isFull) {
            Spacer(modifier = Modifier.height(12.dp))

            // Slot 已满提示
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF3E0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️ 声音slot已满",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // TODO: 购买 UI 预留 (暂不实现具体逻辑)
                    Button(
                        onClick = { /* 跳转购买页面 */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800),
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "💰 购买更多slot")
                    }
                }
            }
        } else {
            // 克隆新声音按钮
            Spacer(modifier = Modifier.height(12.dp))
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
