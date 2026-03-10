package com.example.chatpart

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chatpart.auth.GoogleAuthManager
import com.example.chatpart.screens.ChatScreen
import com.example.chatpart.screens.HistoryScreen
import com.example.chatpart.screens.LoginScreen
import com.example.chatpart.screens.SettingsScreen
import com.example.chatpart.screens.CharacterListScreen
import com.example.chatpart.screens.CharacterEditorScreen
import com.example.chatpart.screens.GenderSelectionScreen
import com.example.chatpart.screens.AvatarSelectionScreen
import com.example.chatpart.screens.CharacterBasicScreen
import com.example.chatpart.screens.CharacterDetailScreen
import com.example.chatpart.screens.LanguageSelectionScreen
import com.example.chatpart.screens.VoiceCloneScreen
import com.example.chatpart.data.CharacterStorage
import com.example.chatpart.ui.theme.ChatPartTheme
import com.example.chatpart.ui.theme.Peach
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.FirebaseUser

// ChatPart AI components
import com.example.chatpart.llm.AITest
import com.example.chatpart.llm.EmbeddingLlm
import com.example.chatpart.memory.InMemoryStore
import com.example.chatpart.data.PersonChat
import com.example.chatpart.domain.Profile
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Role
import java.util.UUID

data class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {

    // AI Components
    private val llmClient = AITest()
    private val memoryStore = InMemoryStore()
    private val embeddingClient = EmbeddingLlm()
    private val personChat = PersonChat(llmClient, memoryStore, embeddingClient)

    // Default profile for chat (fallback only)
    private val defaultProfile = Profile(
        id = "char_001",
        name = "AI 助手",
        gender = "女",
        relationship = "用户的朋友",
        background = "你是一个友好、乐观的女孩，喜欢帮助用户",
        personality = "善良活泼",
        speakStyle = listOf("友好", "活泼"),
        doRules = listOf("保持朋友身份"),
        dontRules = listOf("不要承认是AI")
    )

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    // Page constants
    companion object {
        const val PAGE_ONBOARDING_1 = 0
        const val PAGE_ONBOARDING_2 = 1
        const val PAGE_LOGIN = 2
        const val PAGE_MAIN = 3
        const val PAGE_CHARACTER_LIST = 4
        const val PAGE_CHARACTER_EDITOR = 5
        // New onboarding flow pages
        const val PAGE_GENDER_SELECT = 6
        const val PAGE_AVATAR_SELECT = 7
        const val PAGE_CHARACTER_BASIC = 8
        const val PAGE_CHARACTER_DETAIL = 9
        const val PAGE_LANGUAGE_SELECT = 10
        const val PAGE_VOICE_CLONE = 11
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Initialize Firebase Analytics
        firebaseAnalytics = Firebase.analytics
        Log.d("Firebase", "✅ Firebase initialized successfully!")

        // Log a test event to verify Firebase Analytics is working
        firebaseAnalytics.logEvent("app_opened") {
            param("platform", "android")
            param("timestamp", System.currentTimeMillis().toString())
        }
        Log.d("Firebase", "✅ Test analytics event 'app_opened' logged!")

        enableEdgeToEdge()
        setContent {
            var currentPage by remember { mutableIntStateOf(0) }
            var isDarkMode by remember { mutableStateOf(false) }
            val context = LocalContext.current
            val manager = remember { OnboardingManager(context) }
            val authManager = remember { GoogleAuthManager(context) }

            // Character storage
            val characterStorage = remember { CharacterStorage(context) }
            var characters by remember { mutableStateOf(characterStorage.loadCharacters()) }
            var selectedCharacter by remember { mutableStateOf(characterStorage.getSelectedCharacter() ?: defaultProfile) }
            var editingCharacter by remember { mutableStateOf<Profile?>(null) }

            // Track auth state
            var currentUser by remember { mutableStateOf(authManager.currentUser) }

            // Onboarding flow state
            var onboardingGender by remember { mutableStateOf<String?>(null) }
            var onboardingAvatar by remember { mutableStateOf<String?>(null) }
            var onboardingName by remember { mutableStateOf("") }
            var onboardingRelationship by remember { mutableStateOf("") }
            var pendingVoiceCloneProfile by remember { mutableStateOf<Profile?>(null) }

            // Apply theme based on dark mode
            ChatPartTheme(darkTheme = isDarkMode) {
                when (currentPage) {
                    PAGE_ONBOARDING_1 -> {
                        OnboardingScreen(
                            onSkip = {
                                currentPage = if (authManager.isSignedIn) PAGE_MAIN else PAGE_LOGIN
                            },
                            onNext = {
                                currentPage = PAGE_ONBOARDING_2
                            }
                        )
                    }
                    PAGE_ONBOARDING_2 -> {
                        CloudOnboardingScreen(
                            onSkip = {
                                // 新引导流程: Onboarding → 语言选择 → 性别选择 → 头像选择 → 基本资料 → 详细资料 → 主界面
                                if (authManager.isSignedIn) {
                                    // 已登录用户直接进入语言选择
                                    currentPage = PAGE_LANGUAGE_SELECT
                                } else {
                                    // 未登录用户先登录
                                    currentPage = PAGE_LOGIN
                                }
                            },
                            onNext = {
                                // 新引导流程: Onboarding → 语言选择 → 性别选择 → 头像选择 → 基本资料 → 详细资料 → 主界面
                                if (authManager.isSignedIn) {
                                    currentPage = PAGE_LANGUAGE_SELECT
                                } else {
                                    currentPage = PAGE_LOGIN
                                }
                            }
                        )
                    }
                    PAGE_LOGIN -> {
                        // 首次使用登录后跳转新引导流程
                        LoginScreen(
                            authManager = authManager,
                            onSignInSuccess = {
                                currentUser = authManager.currentUser
                                Log.d("Firebase", "✅ User signed in: ${currentUser?.displayName}")
                                // 登录后跳转到语言选择
                                currentPage = PAGE_LANGUAGE_SELECT
                            }
                        )
                    }
                    PAGE_CHARACTER_LIST -> {
                        CharacterListScreen(
                            isDarkMode = isDarkMode,
                            characters = characters,
                            selectedCharacterId = selectedCharacter.id,
                            onSelectCharacter = { profile ->
                                characterStorage.saveSelectedCharacterId(profile.id)
                                selectedCharacter = profile
                                currentPage = PAGE_MAIN
                            },
                            onCreateNew = {
                                editingCharacter = null
                                currentPage = PAGE_CHARACTER_EDITOR
                            },
                            onEdit = { profile ->
                                editingCharacter = profile
                                currentPage = PAGE_CHARACTER_EDITOR
                            },
                            onDelete = { profile ->
                                characterStorage.deleteCharacter(profile.id)
                                characters = characterStorage.loadCharacters()
                                selectedCharacter = characterStorage.getSelectedCharacter() ?: defaultProfile
                            },
                            onBack = {
                                currentPage = PAGE_MAIN
                            }
                        )
                    }
                    PAGE_CHARACTER_EDITOR -> {
                        CharacterEditorScreen(
                            isDarkMode = isDarkMode,
                            existingProfile = editingCharacter,
                            onSave = { profile ->
                                if (editingCharacter != null) {
                                    characterStorage.updateCharacter(profile)
                                } else {
                                    characterStorage.addCharacter(profile)
                                }
                                characters = characterStorage.loadCharacters()
                                // If creating new character, select it
                                if (editingCharacter == null) {
                                    characterStorage.saveSelectedCharacterId(profile.id)
                                    selectedCharacter = profile
                                }
                                // 创建完成后直接进入主界面
                                currentPage = PAGE_MAIN
                            },
                            onCancel = {
                                // 取消后返回角色列表或主界面
                                currentPage = if (characters.isEmpty()) PAGE_ONBOARDING_2 else PAGE_CHARACTER_LIST
                            }
                        )
                    }
                    // New onboarding flow pages
                    PAGE_LANGUAGE_SELECT -> {
                        LanguageSelectionScreen(
                            isDarkMode = isDarkMode,
                            onLanguageSelected = { langCode ->
                                currentPage = PAGE_GENDER_SELECT
                            },
                            onSkip = {
                                currentPage = PAGE_GENDER_SELECT
                            }
                        )
                    }
                    PAGE_GENDER_SELECT -> {
                        GenderSelectionScreen(
                            isDarkMode = isDarkMode,
                            onGenderSelected = { gender ->
                                onboardingGender = gender
                                currentPage = PAGE_AVATAR_SELECT
                            },
                            onSkip = {
                                // Skip to main (for editing existing character)
                                currentPage = if (characters.isEmpty()) PAGE_CHARACTER_LIST else PAGE_MAIN
                            }
                        )
                    }
                    PAGE_AVATAR_SELECT -> {
                        AvatarSelectionScreen(
                            isDarkMode = isDarkMode,
                            selectedGender = onboardingGender ?: "其他",
                            onAvatarSelected = { avatar ->
                                onboardingAvatar = avatar
                                currentPage = PAGE_CHARACTER_BASIC
                            },
                            onBack = {
                                currentPage = PAGE_GENDER_SELECT
                            },
                            onSkip = {
                                // Skip to main
                                currentPage = if (characters.isEmpty()) PAGE_CHARACTER_LIST else PAGE_MAIN
                            }
                        )
                    }
                    PAGE_CHARACTER_BASIC -> {
                        CharacterBasicScreen(
                            isDarkMode = isDarkMode,
                            selectedGender = onboardingGender ?: "其他",
                            selectedAvatar = onboardingAvatar ?: "👤",
                            onNext = { name, relationship ->
                                onboardingName = name
                                onboardingRelationship = relationship
                                currentPage = PAGE_CHARACTER_DETAIL
                            },
                            onBack = {
                                currentPage = PAGE_AVATAR_SELECT
                            },
                            onSkip = {
                                // 如果没有填写信息就跳过，返回角色列表
                                currentPage = PAGE_CHARACTER_LIST
                            }
                        )
                    }
                    PAGE_CHARACTER_DETAIL -> {
                        val basicProfile = Profile(
                            id = "char_${UUID.randomUUID()}",
                            name = onboardingName,
                            gender = onboardingGender ?: "其他",
                            relationship = onboardingRelationship,
                            background = "",
                            personality = "",
                            customAvatarPath = onboardingAvatar
                        )
                        CharacterDetailScreen(
                            isDarkMode = isDarkMode,
                            basicProfile = basicProfile,
                            onSave = { profile ->
                                // Go to voice clone screen
                                pendingVoiceCloneProfile = profile
                                currentPage = PAGE_VOICE_CLONE
                            },
                            onBack = {
                                currentPage = PAGE_CHARACTER_BASIC
                            },
                            onSkip = {
                                // Go to voice clone screen without additional details
                                pendingVoiceCloneProfile = basicProfile
                                currentPage = PAGE_VOICE_CLONE
                            }
                        )
                    }
                    PAGE_VOICE_CLONE -> {
                        VoiceCloneScreen(
                            isDarkMode = isDarkMode,
                            characterName = pendingVoiceCloneProfile?.name ?: "Character",
                            onVoiceCloned = { voiceId ->
                                // Update profile with voice ID and save
                                val profile = pendingVoiceCloneProfile?.copy(voiceId = voiceId)
                                if (profile != null) {
                                    characterStorage.addCharacter(profile)
                                    characters = characterStorage.loadCharacters()
                                    characterStorage.saveSelectedCharacterId(profile.id)
                                    selectedCharacter = profile
                                }
                                currentPage = PAGE_MAIN
                            },
                            onSkip = {
                                // Save profile without voice cloning
                                val profile = pendingVoiceCloneProfile
                                if (profile != null) {
                                    characterStorage.addCharacter(profile)
                                    characters = characterStorage.loadCharacters()
                                    characterStorage.saveSelectedCharacterId(profile.id)
                                    selectedCharacter = profile
                                }
                                currentPage = PAGE_MAIN
                            },
                            onBack = {
                                currentPage = PAGE_CHARACTER_DETAIL
                            }
                        )
                    }
                    else -> {
                        // Main page with tab navigation
                        MainTabScreen(
                            currentUser = currentUser,
                            personChat = personChat,
                            currentProfile = selectedCharacter,
                            isDarkMode = isDarkMode,
                            customCharacters = characters,
                            onDarkModeChange = { isDarkMode = it },
                            onNavigateToCharacters = {
                                currentPage = PAGE_CHARACTER_LIST
                            },
                            onSignOut = {
                                authManager.signOut()
                                currentUser = null
                                currentPage = PAGE_LOGIN
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainTabScreen(
    currentUser: FirebaseUser? = null,
    personChat: PersonChat? = null,
    currentProfile: Profile? = null,
    isDarkMode: Boolean = false,
    customCharacters: List<Profile> = emptyList(),
    onDarkModeChange: (Boolean) -> Unit = {},
    onNavigateToCharacters: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val tabs = listOf(
        TabItem("Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
        TabItem("History", Icons.Filled.History, Icons.Outlined.History),
        TabItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    // Theme-aware colors
    val navBarColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val navBarContentColor = if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF2D2D2D)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = navBarColor,
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                tab.title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Peach,
                            selectedTextColor = Peach,
                            unselectedIconColor = if (isDarkMode) Color(0xFF888888) else Color.Gray,
                            unselectedTextColor = if (isDarkMode) Color(0xFF888888) else Color.Gray,
                            indicatorColor = Peach.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ChatScreen(
                    currentUser = currentUser,
                    personChat = personChat,
                    currentProfile = currentProfile,
                    isDarkMode = isDarkMode,
                    customCharacters = customCharacters
                )
                1 -> HistoryScreen(isDarkMode = isDarkMode)
                2 -> SettingsScreen(
                    currentUser = currentUser,
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    onNavigateToCharacters = onNavigateToCharacters,
                    onSignOut = onSignOut
                )
            }
        }
    }
}
