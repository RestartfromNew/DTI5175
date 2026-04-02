package com.example.chatpart

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import com.example.chatpart.screens.VoiceManagementScreen
import com.example.chatpart.screens.LiveVoiceScreen
import com.example.chatpart.screens.CreateCharacterScreen
import com.example.chatpart.data.CharacterStorage
import com.example.chatpart.i18n.LanguageManager
import com.example.chatpart.i18n.Languages
import com.example.chatpart.ui.theme.ChatPartTheme
import com.example.chatpart.ui.theme.Lavender
import com.example.chatpart.ui.theme.Peach
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.FirebaseUser

// ChatPart AI components
import com.example.chatpart.llm.MiniMaxLlmClient
import com.example.chatpart.llm.EmbeddingLlm
import com.example.chatpart.memory.InMemoryStore
import com.example.chatpart.data.PersonChat
import com.example.chatpart.data.UIPreferences
import com.example.chatpart.domain.Profile
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Role
import com.example.chatpart.llm.AITest
import com.example.chatpart.firestore.UserVoiceManager
import com.example.chatpart.firestore.FirestoreManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.rememberCoroutineScope
import java.util.UUID

data class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {

    // AI Components
//     private val llmClient = MiniMaxLlmClient()
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
        const val PAGE_VOICE_MANAGEMENT = 12
        const val PAGE_LIVE_VOICE = 13
        const val PAGE_CREATE_CHARACTER = 14  // New unified Create Character Wizard

        // Onboarding pages set (for filtering in goBack)
        val ONBOARDING_PAGES = setOf(
            PAGE_ONBOARDING_1, PAGE_ONBOARDING_2, PAGE_LOGIN,
            PAGE_LANGUAGE_SELECT, PAGE_GENDER_SELECT,
            PAGE_AVATAR_SELECT, PAGE_CHARACTER_BASIC, PAGE_CHARACTER_DETAIL,
            PAGE_CREATE_CHARACTER
        )
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
            var isDarkMode by remember { mutableStateOf(false) }
            // Hoist selectedTab here so it survives navigation away and back
            // (if kept inside MainTabScreen, it resets to 0/Chat every time you return)
            var mainSelectedTab by remember { mutableIntStateOf(0) }
            // For ChatScreen: remember which bot was specifically targeted (e.g. from history or manager)
            // Hoisted here to survive navigation away and back to PAGE_MAIN.
            var chatTargetBotId by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            val manager = remember { OnboardingManager(context) }
            val authManager = remember { GoogleAuthManager(context) }
            val langManager = remember { LanguageManager(context) }
            var currentLanguage by remember { mutableStateOf(langManager.getCurrentLanguage()) }

            // Calculate start page based on onboarding status (FIX for problem A)
            val startPage = when {
                manager.isCompleted() && authManager.isSignedIn -> PAGE_MAIN
                manager.isCompleted() -> PAGE_LOGIN
                else -> PAGE_ONBOARDING_1
            }

            var currentPage by remember { mutableIntStateOf(startPage) }
            // Navigation stack to track page history for back navigation
            var navigationStack by remember { mutableStateOf(listOf(startPage)) }

            // Flag to track if VoiceClone is entered from onboarding flow
            var voiceCloneFromOnboarding by remember { mutableStateOf(false) }

            // Helper function to navigate to a new page and add to stack
            fun navigateTo(page: Int) {
                currentPage = page
                navigationStack = navigationStack + page
            }

            // Complete onboarding and go to main - reset navigation stack (FIX for problem B)
            fun completeOnboardingAndGoToMain() {
                manager.setCompleted()  // Mark onboarding as completed
                currentPage = PAGE_MAIN
                navigationStack = listOf(PAGE_MAIN)  // Clear all onboarding history
            }

            // Helper function to go back to previous page - filter out onboarding pages
            // targetPage: optional specific destination page (for special navigation like LiveVoice → CharacterList)
            fun goBack(targetPage: Int? = null): Boolean {
                if (navigationStack.size <= 1) return false

                // If a specific target is requested, navigate to it directly
                if (targetPage != null) {
                    val targetIndex = navigationStack.indexOf(targetPage)
                    if (targetIndex >= 0) {
                        // Find the last occurrence of targetPage before the current page
                        val currentIndex = navigationStack.lastIndexOf(currentPage)
                        val actualTargetIndex = navigationStack.dropLast(1)
                            .lastIndexOf(targetPage)
                            .takeIf { it >= 0 } ?: targetIndex
                        if (actualTargetIndex >= 0 && actualTargetIndex < navigationStack.size) {
                            val finalStack = navigationStack.take(actualTargetIndex + 1)
                            navigationStack = finalStack
                            currentPage = targetPage
                            return true
                        }
                    }
                    // Fallback: if target not found in stack, just pop
                }

                // Default behavior: pop current page
                val newStack = navigationStack.dropLast(1)

                // Filter out onboarding pages if onboarding is completed
                val filteredStack = if (manager.isCompleted()) {
                    newStack.dropLastWhile { it in ONBOARDING_PAGES }
                } else {
                    newStack
                }

                return if (filteredStack.isEmpty()) {
                    false // Let finish() handle it
                } else {
                    navigationStack = filteredStack
                    currentPage = filteredStack.last()
                    true
                }
            }

            // Handle system back gesture
            BackHandler(enabled = true) {
                if (!goBack()) {
                    // If cannot go back, exit the app
                    finish()
                }
            }

            // Track auth state — declared FIRST so CharacterStorage can use uid
            var currentUser by remember { mutableStateOf(authManager.currentUser) }

            // Character storage — keyed on uid so each account has isolated data.
            // remember(currentUser?.uid) recreates the storage when the user changes.
            val characterStorage = remember(currentUser?.uid) {
                CharacterStorage(context, currentUser?.uid ?: "")
            }
            // Reload characters whenever the storage switches to a different user
            var characters by remember(currentUser?.uid) {
                mutableStateOf(characterStorage.loadCharacters())
            }
            var selectedCharacter by remember(currentUser?.uid) {
                mutableStateOf(characterStorage.getSelectedCharacter() ?: defaultProfile)
            }
            var editingCharacter by remember { mutableStateOf<Profile?>(null) }

            // UserVoiceManager for Firestore operations (created when user signs in)
            var userVoiceManager by remember { mutableStateOf<UserVoiceManager?>(null) }

            // Initialize UserVoiceManager when user signs in.
            // Also clear it on sign-out so old uid is never reused.
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    userVoiceManager = UserVoiceManager(currentUser!!.uid)
                    userVoiceManager?.ensureUserExists(currentUser!!.email ?: "")
                } else {
                    userVoiceManager = null
                }
            }

            // Onboarding flow state
            var onboardingGender by remember { mutableStateOf<String?>(null) }
            var onboardingAvatar by remember { mutableStateOf<String?>(null) }
            var onboardingName by remember { mutableStateOf("") }
            var onboardingRelationship by remember { mutableStateOf("") }
            var pendingVoiceCloneProfile by remember { mutableStateOf<Profile?>(null) }
            var currentVoiceCallCharacter by remember { mutableStateOf<Profile?>(null) }

            // Apply theme based on dark mode
            ChatPartTheme(darkTheme = isDarkMode) {
                when (currentPage) {
                    PAGE_ONBOARDING_1 -> {
                        OnboardingScreen(
                            onSkip = {
                                if (authManager.isSignedIn) {
                                    // Skip onboarding and go directly to main - mark as completed
                                    completeOnboardingAndGoToMain()
                                } else {
                                    navigateTo(PAGE_LOGIN)
                                }
                            },
                            onNext = {
                                navigateTo(PAGE_ONBOARDING_2)
                            }
                        )
                    }
                    PAGE_ONBOARDING_2 -> {
                        CloudOnboardingScreen(
                            onSkip = {
                                if (authManager.isSignedIn) {
                                    navigateTo(PAGE_LANGUAGE_SELECT)
                                } else {
                                    navigateTo(PAGE_LOGIN)
                                }
                            },
                            onNext = {
                                if (authManager.isSignedIn) {
                                    navigateTo(PAGE_LANGUAGE_SELECT)
                                } else {
                                    navigateTo(PAGE_LOGIN)
                                }
                            }
                        )
                    }
                    PAGE_LOGIN -> {
                        LoginScreen(
                            authManager = authManager,
                            onSignInSuccess = {
                                currentUser = authManager.currentUser
                                Log.d("Firebase", "✅ User signed in: ${currentUser?.displayName}")
                                navigateTo(PAGE_LANGUAGE_SELECT)
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
                                // Ensure ChatScreen switches to this custom character
                                chatTargetBotId = "custom_${profile.id}"
                                // When selecting a character from the list, ALWAYS switch to Chat tab
                                mainSelectedTab = 0
                                navigateTo(PAGE_MAIN)
                            },
                            onCreateNew = {
                                editingCharacter = null
                                onboardingGender = null
                                onboardingAvatar = null
                                onboardingName = ""
                                onboardingRelationship = ""
                                navigateTo(PAGE_GENDER_SELECT)
                            },
                            onEdit = { profile ->
                                editingCharacter = profile
                                navigateTo(PAGE_CREATE_CHARACTER)
                            },
                            onDelete = { profile ->
                                characterStorage.deleteCharacter(profile.id)
                                characters = characterStorage.loadCharacters()
                                selectedCharacter = characterStorage.getSelectedCharacter() ?: defaultProfile
                            },
                            onBack = {
                                goBack()
                            },
                            onVoiceCall = { profile ->
                                currentVoiceCallCharacter = profile
                                navigateTo(PAGE_LIVE_VOICE)
                            }
                        )
                    }
                    PAGE_LIVE_VOICE -> {
                        currentVoiceCallCharacter?.let { character ->
                            // ★ 修复：监听页面退出，确保系统返回手势时也能清理状态
                            DisposableEffect(Unit) {
                                onDispose {
                                    // 当 LiveVoiceScreen 退出时（无论是哪种方式退出）
                                    // 清理 currentVoiceCallCharacter 状态，防止泄漏
                                    currentVoiceCallCharacter = null
                                }
                            }

                            LiveVoiceScreen(
                                character = character,
                                brain = personChat,
                                chatHistory = emptyList(),
                                onEndCall = {
                                    goBack(PAGE_CHARACTER_LIST)
                                },
                                onMessageAdded = { _, _ -> },
                                currentLanguage = currentLanguage
                            )
                        } ?: run {
                            LaunchedEffect(Unit) { goBack() }
                        }
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
                                navigateTo(PAGE_MAIN)
                            },
                            onCancel = {
                                navigateTo(if (characters.isEmpty()) PAGE_ONBOARDING_2 else PAGE_CHARACTER_LIST)
                            }
                        )
                    }
                    PAGE_CREATE_CHARACTER -> {
                        CreateCharacterScreen(
                            isDarkMode = isDarkMode,
                            existingProfile = editingCharacter,
                            onSave = { profile ->
                                val isNew = editingCharacter == null
                                if (isNew) {
                                    characterStorage.addCharacter(profile)
                                    characterStorage.saveSelectedCharacterId(profile.id)
                                    selectedCharacter = profile
                                } else {
                                    characterStorage.updateCharacter(profile)
                                }
                                characters = characterStorage.loadCharacters()
                                
                                if (!manager.isCompleted()) {
                                    completeOnboardingAndGoToMain()
                                } else if (isNew) {
                                    // NEW character creation — go straight to chat as requested
                                    chatTargetBotId = "custom_${profile.id}"
                                    mainSelectedTab = 0
                                    navigateTo(PAGE_MAIN)
                                } else {
                                    // Edited existing — return to character list screen
                                    goBack(PAGE_CHARACTER_LIST)
                                }
                            },
                            onCancel = {
                                if (manager.isCompleted()) {
                                    navigateTo(PAGE_CHARACTER_LIST)
                                } else {
                                    navigateTo(PAGE_LANGUAGE_SELECT)
                                }
                            }
                        )
                    }
                    // New onboarding flow pages
                    PAGE_LANGUAGE_SELECT -> {
                        LanguageSelectionScreen(
                            isDarkMode = isDarkMode,
                            onLanguageSelected = { langCode ->
                                navigateTo(PAGE_GENDER_SELECT)
                            },
                            onSkip = {
                                navigateTo(PAGE_GENDER_SELECT)
                            }
                        )
                    }
                    PAGE_GENDER_SELECT -> {
                        GenderSelectionScreen(
                            isDarkMode = isDarkMode,
                            onGenderSelected = { gender ->
                                onboardingGender = gender
                                navigateTo(PAGE_AVATAR_SELECT)
                            },
                            onSkip = {
                                onboardingGender = "其他"
                                navigateTo(PAGE_AVATAR_SELECT)
                            }
                        )
                    }
                    PAGE_AVATAR_SELECT -> {
                        AvatarSelectionScreen(
                            isDarkMode = isDarkMode,
                            selectedGender = onboardingGender ?: "其他",
                            onAvatarSelected = { avatar ->
                                onboardingAvatar = avatar
                                navigateTo(PAGE_CHARACTER_BASIC)
                            },
                            onBack = { goBack() },
                            onSkip = {
                                onboardingAvatar = "👤"
                                navigateTo(PAGE_CHARACTER_BASIC)
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
                                navigateTo(PAGE_CHARACTER_DETAIL)
                            },
                            onBack = { goBack() },
                            onSkip = {
                                onboardingName = "Character"
                                onboardingRelationship = "朋友"
                                navigateTo(PAGE_CHARACTER_DETAIL)
                            }
                        )
                    }
                    PAGE_CHARACTER_DETAIL -> {
                        val basicProfile = Profile(
                            id = "char_${System.currentTimeMillis()}",
                            name = onboardingName.ifBlank { "Character" },
                            gender = onboardingGender ?: "其他",
                            relationship = onboardingRelationship.ifBlank { "朋友" },
                            background = "",
                            customAvatarPath = onboardingAvatar?.takeIf { it.startsWith("/") }
                        )
                        val isOnboarding = !manager.isCompleted()
                        CharacterDetailScreen(
                            isDarkMode = isDarkMode,
                            basicProfile = basicProfile,
                            onSave = { profile ->
                                if (isOnboarding) {
                                    pendingVoiceCloneProfile = profile
                                    voiceCloneFromOnboarding = true
                                    navigateTo(PAGE_VOICE_CLONE)
                                } else {
                                    characterStorage.addCharacter(profile)
                                    characters = characterStorage.loadCharacters()
                                    characterStorage.saveSelectedCharacterId(profile.id)
                                    selectedCharacter = profile
                                    chatTargetBotId = "custom_${profile.id}"
                                    mainSelectedTab = 0
                                    navigateTo(PAGE_MAIN)
                                }
                            },
                            onBack = { goBack() },
                            onSkip = {
                                if (isOnboarding) {
                                    pendingVoiceCloneProfile = basicProfile
                                    voiceCloneFromOnboarding = true
                                    navigateTo(PAGE_VOICE_CLONE)
                                } else {
                                    characterStorage.addCharacter(basicProfile)
                                    characters = characterStorage.loadCharacters()
                                    characterStorage.saveSelectedCharacterId(basicProfile.id)
                                    selectedCharacter = basicProfile
                                    chatTargetBotId = "custom_${basicProfile.id}"
                                    mainSelectedTab = 0
                                    navigateTo(PAGE_MAIN)
                                }
                            }
                        )
                    }
                    PAGE_VOICE_CLONE -> {
                        if (userVoiceManager == null) {
                            goBack()
                        } else {
                            val manager = userVoiceManager!!
                            // SECURITY: Pass uid for account isolation in voice_id
                            val currentUid = currentUser?.uid ?: ""
                            VoiceCloneScreen(
                                isDarkMode = isDarkMode,
                                characterName = pendingVoiceCloneProfile?.name ?: "Character",
                                characterId = pendingVoiceCloneProfile?.id ?: "unknown",
                                avatarPath = pendingVoiceCloneProfile?.customAvatarPath,
                                uid = currentUid,
                                userVoiceManager = manager,
                                onVoiceCloned = { voiceId ->
                                    val profile = pendingVoiceCloneProfile?.copy(voiceId = voiceId)
                                    if (profile != null) {
                                        characterStorage.addCharacter(profile)
                                        characters = characterStorage.loadCharacters()
                                        characterStorage.saveSelectedCharacterId(profile.id)
                                        selectedCharacter = profile
                                    }
                                    if (voiceCloneFromOnboarding) {
                                        completeOnboardingAndGoToMain()
                                    } else {
                                        goBack()
                                    }
                                },
                                onSkip = {
                                    val profile = pendingVoiceCloneProfile
                                    if (profile != null) {
                                        characterStorage.addCharacter(profile)
                                        characters = characterStorage.loadCharacters()
                                        characterStorage.saveSelectedCharacterId(profile.id)
                                        selectedCharacter = profile
                                    }
                                    if (voiceCloneFromOnboarding) {
                                        completeOnboardingAndGoToMain()
                                    } else {
                                        goBack()
                                    }
                                },
                                onBack = {
                                    goBack()
                                }
                            )
                        }
                    }
                    PAGE_VOICE_MANAGEMENT -> {
                        if (userVoiceManager == null) {
                            goBack()
                        } else {
                            val manager = userVoiceManager!!
                            VoiceManagementScreen(
                            isDarkMode = isDarkMode,
                            currentLanguage = currentLanguage,
                            characterStorage = characterStorage,
                            userVoiceManager = manager,
                            onNavigateToVoiceClone = {
                                pendingVoiceCloneProfile = Profile(
                                    id = "temp_voice_${System.currentTimeMillis()}",
                                    name = "New Voice",
                                    gender = "女",
                                    relationship = "朋友",
                                    background = ""
                                )
                                voiceCloneFromOnboarding = false
                                navigateTo(PAGE_VOICE_CLONE)
                            },
                            onBack = {
                                goBack()
                            },
                            onVoicesChanged = {
                                characters = characterStorage.loadCharacters()
                            }
                        )
                        }
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
                                navigateTo(PAGE_CHARACTER_LIST)
                            },
                            onSignOut = {
                                authManager.signOut()
                                currentUser = null
                                navigateTo(PAGE_LOGIN)
                            },
                            onSelectCharacter = { profile ->
                                characterStorage.saveSelectedCharacterId(profile.id)
                                selectedCharacter = profile
                            },
                            currentLanguage = currentLanguage,
                            onLanguageChange = { code -> currentLanguage = code },
                            onNavigateToVoices = { navigateTo(PAGE_VOICE_MANAGEMENT) },
                            onNavigateToLiveVoice = {
                                currentVoiceCallCharacter = selectedCharacter
                                navigateTo(PAGE_LIVE_VOICE)
                            },
                            mainSelectedTab = mainSelectedTab,
                            onSelectedTabChange = { mainSelectedTab = it },
                            chatTargetBotId = chatTargetBotId,
                            onChatTargetBotIdChange = { chatTargetBotId = it }
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
    onNavigateToVoices: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onSelectCharacter: (Profile) -> Unit = {},
    currentLanguage: String = "zh",
    onLanguageChange: (String) -> Unit = {},
    onNavigateToLiveVoice: () -> Unit = {},
    mainSelectedTab: Int = 0,
    onSelectedTabChange: (Int) -> Unit = {},
    chatTargetBotId: String? = null,
    onChatTargetBotIdChange: (String?) -> Unit = {}
) {
    val context = LocalContext.current

    fun t(key: String) = Languages.getString(currentLanguage, key)

    val tabs = listOf(
        TabItem(t("CHAT"), Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
        TabItem(t("HISTORY"), Icons.Filled.History, Icons.Outlined.History),
        TabItem(t("SETTINGS"), Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    // Theme-aware colors
    val navBarColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val navBarContentColor = if (isDarkMode) Color(0xFFE0E0E0) else Color(0xFF2D2D2D)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = navBarColor,
                contentColor = navBarContentColor,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = mainSelectedTab == index,
                        onClick = { onSelectedTabChange(index) },
                        icon = {
                            Icon(
                                imageVector = if (mainSelectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                tab.title,
                                fontWeight = if (mainSelectedTab == index) FontWeight.Bold else FontWeight.Normal,
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
        // 浮动按钮拖动状态
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val buttonSize = 64.dp
        val buttonSizePx = with(density) { buttonSize.toPx() }
        val screenWidthPx = configuration.screenWidthDp * density.density
        val screenHeightPx = configuration.screenHeightDp * density.density
        val bottomPaddingPx = with(density) { innerPadding.calculateBottomPadding().toPx() }
        val topPaddingPx = with(density) { innerPadding.calculateTopPadding().toPx() }
        
        val maxDragX = screenWidthPx - buttonSizePx
        val maxDragY = screenHeightPx - topPaddingPx - bottomPaddingPx - buttonSizePx
        val defaultOffsetX = screenWidthPx - buttonSizePx - with(density) { 16.dp.toPx() }
        val defaultOffsetY = with(density) { 16.dp.toPx() }

        val uiPreferences = remember { UIPreferences(context) }
        val savedOffsetX = uiPreferences.getCallButtonOffsetX()
        val savedOffsetY = uiPreferences.getCallButtonOffsetY()
        val initialOffsetX = if (savedOffsetX >= 0) savedOffsetX.coerceIn(0f, maxDragX) else defaultOffsetX
        val initialOffsetY = if (savedOffsetY >= 0) savedOffsetY.coerceIn(0f, maxDragY) else defaultOffsetY

        var buttonOffsetX by remember { mutableStateOf(initialOffsetX) }
        var buttonOffsetY by remember { mutableStateOf(initialOffsetY) }
        val haptic = LocalHapticFeedback.current

        Box(modifier = Modifier.padding(innerPadding)) {
            when (mainSelectedTab) {
                0 -> ChatScreen(
                    currentUser = currentUser,
                    personChat = personChat,
                    currentProfile = currentProfile,
                    isDarkMode = isDarkMode,
                    customCharacters = customCharacters,
                    targetBotId = chatTargetBotId,
                    currentLanguage = currentLanguage
                )
                1 -> HistoryScreen(
                    isDarkMode = isDarkMode,
                    characters = customCharacters,
                    onChatClick = { characterId ->
                        onChatTargetBotIdChange(characterId)
                        val actualId = if (characterId.startsWith("custom_")) {
                            characterId.removePrefix("custom_")
                        } else {
                            characterId
                        }
                        val character = customCharacters.find { it.id == actualId }
                        if (character != null) {
                            onSelectCharacter(character)
                        } else {
                            val defaultBotProfile = when (characterId) {
                                "assistant" -> Profile(
                                    id = "assistant",
                                    name = "AI Assistant",
                                    gender = "其他",
                                    relationship = "AI助手",
                                    background = "你是一个AI助手",
                                    personality = "helpful",
                                    speakStyle = listOf("helpful"),
                                    doRules = listOf("帮助用户"),
                                    dontRules = listOf("不要假装是人类")
                                )
                                "teacher" -> Profile(
                                    id = "teacher",
                                    name = "Teacher",
                                    gender = "女",
                                    relationship = "老师",
                                    background = "你是一位老师",
                                    personality = "knowledgeable",
                                    speakStyle = listOf("教学风格"),
                                    doRules = listOf("教导用户"),
                                    dontRules = listOf("不要给出错误信息")
                                )
                                "coding" -> Profile(
                                    id = "coding",
                                    name = "Coder",
                                    gender = "其他",
                                    relationship = "编程助手",
                                    background = "你是一个编程助手",
                                    personality = "technical",
                                    speakStyle = listOf("技术风格"),
                                    doRules = listOf("帮助编程"),
                                    dontRules = listOf("不要写恶意代码")
                                )
                                else -> null
                            }
                            if (defaultBotProfile != null) {
                                onSelectCharacter(defaultBotProfile)
                            }
                        }
                        onSelectedTabChange(0)
                    }
                )
                2 -> SettingsScreen(
                    currentUser = currentUser,
                    isDarkMode = isDarkMode,
                    currentLanguage = currentLanguage,
                    onDarkModeChange = onDarkModeChange,
                    onNavigateToCharacters = onNavigateToCharacters,
                    onNavigateToVoices = onNavigateToVoices,
                    onSignOut = onSignOut,
                    onLanguageChange = onLanguageChange
                )
            }

            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .offset { IntOffset(buttonOffsetX.toInt(), buttonOffsetY.toInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                buttonOffsetX = (buttonOffsetX + dragAmount.x).coerceIn(0f, maxDragX)
                                buttonOffsetY = (buttonOffsetY + dragAmount.y).coerceIn(0f, maxDragY)
                                uiPreferences.setCallButtonOffset(buttonOffsetX, buttonOffsetY)
                            }
                        )
                    }
            ) {
                FilledIconButton(
                    onClick = onNavigateToLiveVoice,
                    modifier = Modifier.size(buttonSize),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Lavender,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Live Voice Chat",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
