package com.example.chatpart.i18n

object EnglishStrings {
    val strings = mapOf(
        // App
        "APP_NAME" to "ChatPart",

        // Onboarding
        "ONBOARDING_TITLE_1" to "Welcome to ChatPart",
        "ONBOARDING_DESC_1" to "Your AI companion awaits",
        "ONBOARDING_TITLE_2" to "Cloud Sync",
        "ONBOARDING_DESC_2" to "Your data is safely stored in the cloud",
        "SKIP" to "Skip",
        "NEXT" to "Next",
        "GET_STARTED" to "Get Started",
        "CHOOSE_LANGUAGE" to "Choose Language",

        // Login
        "SIGN_IN" to "Sign In",
        "SIGN_IN_WITH_GOOGLE" to "Sign in with Google",
        "SIGN_OUT" to "Sign Out",
        "WELCOME_BACK" to "Welcome Back",

        // Chat
        "CHAT" to "Chat",
        "TYPE_MESSAGE" to "Type a message...",
        "SEND" to "Send",
        "HOLD_TO_TALK" to "Hold to talk",
        "RELEASE_TO_SEND" to "Release to send",

        // History
        "HISTORY" to "History",
        "NO_HISTORY" to "No chat history yet",
        "CLEAR_HISTORY" to "Clear History",

        // Settings
        "SETTINGS" to "Settings",
        "DARK_MODE" to "Dark Mode",
        "NOTIFICATIONS" to "Notifications",
        "LANGUAGE" to "Language",
        "AI_MODEL" to "AI Model",
        "RESPONSE_STYLE" to "Response Style",
        "VERSION" to "Version",
        "PRIVACY_POLICY" to "Privacy Policy",
        "ABOUT" to "About",
        "MANAGE_CHARACTERS" to "Manage Characters",
        "MANAGE_VOICES" to "Manage Voices",
        "VOICE_COUNT" to "{n} cloned voice(s)",
        "NO_VOICES" to "No cloned voices yet",
        "CLONE_FIRST_VOICE" to "Clone your first voice",
        "VOICE_FOR" to "Voice for {name}",
        "PLAY" to "Play",
        "DELETE" to "Delete",
        "CONFIRM_DELETE_VOICE" to "Delete this voice? This will also remove it from the character.",
        "VOICE_DELETED" to "Voice deleted",
        "VOICE_PLAYING" to "Playing...",
        "VOICE_ERROR" to "Failed to play voice",

        // Character
        "CHARACTERS" to "Characters",
        "CREATE_CHARACTER" to "Create Character",
        "EDIT_CHARACTER" to "Edit Character",
        "DELETE_CHARACTER" to "Delete Character",
        "CHARACTER_NAME" to "Name",
        "CHARACTER_GENDER" to "Gender",
        "CHARACTER_RELATIONSHIP" to "Relationship",
        "CHARACTER_BACKGROUND" to "Background",
        "CHARACTER_PERSONALITY" to "Personality",
        "CHARACTER_SPEAK_STYLE" to "Speak Style",
        "CHARACTER_RULES" to "Rules",
        "SAVE" to "Save",
        "CANCEL" to "Cancel",
        "DELETE" to "Delete",
        "MALE" to "Male",
        "FEMALE" to "Female",
        "OTHER" to "Other",
        "NO_CHARACTERS" to "No characters yet",
        "CREATE_FIRST_CHARACTER" to "Create your first character",

        // Gender Selection (Onboarding)
        "CHOOSE_GENDER" to "Choose Character Gender",
        "GENDER_DESC" to "Select the gender for your AI companion",

        // Avatar Selection
        "CHOOSE_AVATAR" to "Choose Avatar",
        "UPLOAD_PHOTO" to "Upload Photo",
        "OR_CHOOSE_PRESET" to "Or choose a preset avatar",

        // Basic Info
        "BASIC_INFO" to "Basic Information",
        "NAME_REQUIRED" to "Name *",
        "NAME_PLACEHOLDER" to "e.g., Xiaoming, Alex",
        "RELATIONSHIP_REQUIRED" to "Relationship *",
        "RELATIONSHIP_PLACEHOLDER" to "e.g., Friend, Teacher, Partner",
        "QUICK_SELECT" to "Quick select:",
        "ADD_MORE_DETAILS" to "Add More Details",

        // Optional Details
        "MORE_DETAILS" to "More Details",
        "OPTIONAL_SKIP" to "Optional - Can skip",
        "PERSONALITY" to "Personality",
        "PERSONALITY_PLACEHOLDER" to "e.g., Friendly, Quiet, Humorous...",
        "BACKGROUND_STORY" to "Background Story",
        "BACKGROUND_PLACEHOLDER" to "Describe the character's background...",
        "SPEAK_STYLE" to "Speak Style",
        "SPEAK_STYLE_PLACEHOLDER" to "e.g., Humorous, Formal, Cute...",
        "DO_RULES" to "Rules (Should Do)",
        "DO_RULES_PLACEHOLDER" to "One rule per line...",
        "DONT_RULES" to "Rules (Should Not Do)",
        "DONT_RULES_PLACEHOLDER" to "One rule per line...",
        "FINISH" to "Finish",

        // Voice Clone
        "CLONE_VOICE_FOR" to "Clone Voice for {name}",
        "CLONE_VOICE_DESC" to "Record 5-10 seconds of audio to let your AI character speak with your voice",
        "HOLD_TO_RECORD" to "Hold to record",
        "RELEASE_TO_STOP" to "Release to stop",
        "SEND_CLONE" to "Send to Clone",
        "CLONING" to "Cloning your voice...",
        "CLONE_SUCCESS" to "Voice cloned successfully!",

        // Dialogs
        "CONFIRM" to "Confirm",
        "CONFIRM_DELETE" to "Are you sure you want to delete this character?",
        "OK" to "OK",

        // Navigation
        "BACK" to "Back",

        // Relationships
        "REL_FRIEND" to "Friend",
        "REL_LOVER" to "Lover",
        "REL_TEACHER" to "Teacher",
        "REL_ASSISTANT" to "Assistant",
        "REL_FAMILY" to "Family",
        "REL_CLASSMATE" to "Classmate",

        // Default Chatbots
        "DEFAULT_ASSISTANT" to "AI Assistant",
        "DEFAULT_TEACHER" to "Teacher",
        "DEFAULT_CODER" to "Coder",
        "CHOOSE_CHATBOT" to "Choose your chatbot",
        "DELETE_CHARACTER" to "Delete Character",
        "DELETE_CHARACTER_CONFIRM" to "Are you sure you want to delete \"{name}\"? This action cannot be undone.",
        "DELETE" to "Delete",
        "CANCEL" to "Cancel",

        // Settings sections
        "GENERAL" to "General",
        "AI_SETTINGS" to "AI Settings",
        "SELECT_LANGUAGE" to "Select Language",
        "STT_CONVERT" to "Convert to text",

        // TTS
        "TTS_PLAYING" to "Playing, please wait...",

        // Voice Management Tabs
        "CLONED_VOICES" to "My Voices",
        "DEFAULT_VOICES" to "Default Voices",
        "VOICE_MALE" to "Male",
        "VOICE_FEMALE" to "Female",
        "VOICE_PREVIEW" to "Preview",

        // TTS Languages
        "LANG_ENGLISH" to "English",
        "LANG_CHINESE" to "Chinese",
        "LANG_FRENCH" to "French",
        "LANG_JAPANESE" to "日本語",
        "LANG_KOREAN" to "한국어",
        "LANG_SPANISH" to "Español",

        // Live Voice
        "LIVE_HOLD_TO_SPEAK" to "Hold mic to speak",
        "LIVE_LISTENING" to "Listening...",
        "LIVE_THINKING" to "Thinking...",
        "LIVE_SPEAKING" to "Speaking...",
        "LIVE_RECORD_TOO_SHORT" to "Recording too short, hold mic to speak",
        "LIVE_SPEECH_NOT_RECOGNIZED" to "Speech not recognized, try again",
        "LIVE_ERROR" to "Error: {message}",
        "LIVE_PLAYBACK_FAILED" to "Audio playback failed",
        "LIVE_MIC_PERMISSION" to "Microphone permission required for voice features"
    )
}

object FrenchStrings {
    val strings = mapOf(
        // App
        "APP_NAME" to "ChatPart",

        // Onboarding
        "ONBOARDING_TITLE_1" to "Bienvenue sur ChatPart",
        "ONBOARDING_DESC_1" to "Votre compagnon IA vous attend",
        "ONBOARDING_TITLE_2" to "Synchronisation Cloud",
        "ONBOARDING_DESC_2" to "Vos données sont en sécurité dans le cloud",
        "SKIP" to "Passer",
        "NEXT" to "Suivant",
        "GET_STARTED" to "Commencer",
        "CHOOSE_LANGUAGE" to "Choisir la langue",

        // Login
        "SIGN_IN" to "Connexion",
        "SIGN_IN_WITH_GOOGLE" to "Se connecter avec Google",
        "SIGN_OUT" to "Déconnexion",
        "WELCOME_BACK" to "Bon retour",

        // Chat
        "CHAT" to "Chat",
        "TYPE_MESSAGE" to "Tapez un message...",
        "SEND" to "Envoyer",
        "HOLD_TO_TALK" to "Maintenez pour parler",
        "RELEASE_TO_SEND" to "Relâchez pour envoyer",

        // History
        "HISTORY" to "Historique",
        "NO_HISTORY" to "Pas encore d'historique",
        "CLEAR_HISTORY" to "Effacer l'historique",

        // Settings
        "SETTINGS" to "Paramètres",
        "DARK_MODE" to "Mode sombre",
        "NOTIFICATIONS" to "Notifications",
        "LANGUAGE" to "Langue",
        "AI_MODEL" to "Modèle IA",
        "RESPONSE_STYLE" to "Style de réponse",
        "VERSION" to "Version",
        "PRIVACY_POLICY" to "Politique de confidentialité",
        "ABOUT" to "À propos",
        "MANAGE_CHARACTERS" to "Gérer les personnages",
        "MANAGE_VOICES" to "Gérer les voix",
        "VOICE_COUNT" to "{n} voix clonée(s)",
        "NO_VOICES" to "Pas encore de voix clonées",
        "CLONE_FIRST_VOICE" to "Cloner votre première voix",
        "VOICE_FOR" to "Voix pour {name}",
        "PLAY" to "Lecture",
        "DELETE" to "Supprimer",
        "CONFIRM_DELETE_VOICE" to "Supprimer cette voix ? Cela supprimera également la voix du personnage.",
        "VOICE_DELETED" to "Voix supprimée",
        "VOICE_PLAYING" to "Lecture en cours...",
        "VOICE_ERROR" to "Échec de la lecture",

        // Character
        "CHARACTERS" to "Personnages",
        "CREATE_CHARACTER" to "Créer un personnage",
        "EDIT_CHARACTER" to "Modifier le personnage",
        "DELETE_CHARACTER" to "Supprimer le personnage",
        "CHARACTER_NAME" to "Nom",
        "CHARACTER_GENDER" to "Genre",
        "CHARACTER_RELATIONSHIP" to "Relation",
        "CHARACTER_BACKGROUND" to "Contexte",
        "CHARACTER_PERSONALITY" to "Personnalité",
        "CHARACTER_SPEAK_STYLE" to "Style de parole",
        "CHARACTER_RULES" to "Règles",
        "SAVE" to "Enregistrer",
        "CANCEL" to "Annuler",
        "DELETE" to "Supprimer",
        "MALE" to "Homme",
        "FEMALE" to "Femme",
        "OTHER" to "Autre",
        "NO_CHARACTERS" to "Pas encore de personnages",
        "CREATE_FIRST_CHARACTER" to "Créez votre premier personnage",

        // Gender Selection
        "CHOOSE_GENDER" to "Choisir le genre",
        "GENDER_DESC" to "Sélectionnez le genre de votre compagnon IA",

        // Avatar Selection
        "CHOOSE_AVATAR" to "Choisir un avatar",
        "UPLOAD_PHOTO" to "Télécharger une photo",
        "OR_CHOOSE_PRESET" to "Ou choisir un avatar prédéfini",

        // Basic Info
        "BASIC_INFO" to "Informations de base",
        "NAME_REQUIRED" to "Nom *",
        "NAME_PLACEHOLDER" to "ex: Alex, Marie",
        "RELATIONSHIP_REQUIRED" to "Relation *",
        "RELATIONSHIP_PLACEHOLDER" to "ex: Ami, Enseignant, Partenaire",
        "QUICK_SELECT" to "Sélection rapide:",
        "ADD_MORE_DETAILS" to "Ajouter plus de détails",

        // Optional Details
        "MORE_DETAILS" to "Plus de détails",
        "OPTIONAL_SKIP" to "Optionnel - Peut passer",
        "PERSONALITY" to "Personnalité",
        "PERSONALITY_PLACEHOLDER" to "ex: Amical, Calme, Humoristique...",
        "BACKGROUND_STORY" to "Histoire",
        "BACKGROUND_PLACEHOLDER" to "Décrivez l'histoire du personnage...",
        "SPEAK_STYLE" to "Style de parole",
        "SPEAK_STYLE_PLACEHOLDER" to "ex: Humoristique, Formel, Mignon...",
        "DO_RULES" to "Règles (À faire)",
        "DO_RULES_PLACEHOLDER" to "Une règle par ligne...",
        "DONT_RULES" to "Règles (À ne pas faire)",
        "DONT_RULES_PLACEHOLDER" to "Une règle par ligne...",
        "FINISH" to "Terminer",

        // Voice Clone
        "CLONE_VOICE_FOR" to "Cloner la voix pour {name}",
        "CLONE_VOICE_DESC" to "Enregistrez 5-10 secondes d'audio pour que votre personnage IA parle avec votre voix",
        "HOLD_TO_RECORD" to "Maintenez pour enregistrer",
        "RELEASE_TO_STOP" to "Relâchez pour arrêter",
        "SEND_CLONE" to "Envoyer pour cloner",
        "CLONING" to "Clonage de votre voix...",
        "CLONE_SUCCESS" to "Voix clonée avec succès!",

        // Dialogs
        "CONFIRM" to "Confirmer",
        "CONFIRM_DELETE" to "Voulez-vous vraiment supprimer ce personnage?",
        "OK" to "OK",

        // Navigation
        "BACK" to "Retour",

        // Relationships
        "REL_FRIEND" to "Ami",
        "REL_LOVER" to "Amant",
        "REL_TEACHER" to "Enseignant",
        "REL_ASSISTANT" to "Assistant",
        "REL_FAMILY" to "Famille",
        "REL_CLASSMATE" to "Camarade",

        // Default Chatbots
        "DEFAULT_ASSISTANT" to "Assistant IA",
        "DEFAULT_TEACHER" to "Enseignant",
        "DEFAULT_CODER" to "Programmeur",
        "CHOOSE_CHATBOT" to "Choisissez votre chatbot",
        "DELETE_CHARACTER" to "Supprimer le personnage",
        "DELETE_CHARACTER_CONFIRM" to "Êtes-vous sûr de vouloir supprimer \"{name}\" ? Cette action est irréversible.",
        "DELETE" to "Supprimer",
        "CANCEL" to "Annuler",

        // Settings sections
        "GENERAL" to "Général",
        "AI_SETTINGS" to "Paramètres IA",
        "SELECT_LANGUAGE" to "Sélectionner la langue",
        "STT_CONVERT" to "Convertir en texte",

        // TTS
        "TTS_PLAYING" to "Lecture en cours, veuillez patienter...",

        // Voice Management Tabs
        "CLONED_VOICES" to "Mes voix",
        "DEFAULT_VOICES" to "Voix par défaut",
        "VOICE_MALE" to "Homme",
        "VOICE_FEMALE" to "Femme",
        "VOICE_PREVIEW" to "Aperçu",

        // TTS Languages
        "LANG_ENGLISH" to "Anglais",
        "LANG_CHINESE" to "Chinois",
        "LANG_FRENCH" to "Français",
        "LANG_JAPANESE" to "日本語",
        "LANG_KOREAN" to "한국어",
        "LANG_SPANISH" to "Español",

        // Live Voice
        "LIVE_HOLD_TO_SPEAK" to "Maintenez le micro pour parler",
        "LIVE_LISTENING" to "Écoute...",
        "LIVE_THINKING" to "Réflexion...",
        "LIVE_SPEAKING" to "Parle...",
        "LIVE_RECORD_TOO_SHORT" to "Enregistrement trop court, maintenez le micro",
        "LIVE_SPEECH_NOT_RECOGNIZED" to "Parole non reconnue, réessayez",
        "LIVE_ERROR" to "Erreur: {message}",
        "LIVE_PLAYBACK_FAILED" to "Échec de la lecture audio",
        "LIVE_MIC_PERMISSION" to "Permission du microphone requise"
    )
}

object ChineseStrings {
    val strings = mapOf(
        // App
        "APP_NAME" to "ChatPart",

        // Onboarding
        "ONBOARDING_TITLE_1" to "欢迎使用 ChatPart",
        "ONBOARDING_DESC_1" to "你的 AI 伙伴在这里等你",
        "ONBOARDING_TITLE_2" to "云同步",
        "ONBOARDING_DESC_2" to "你的数据安全地存储在云端",
        "SKIP" to "跳过",
        "NEXT" to "下一步",
        "GET_STARTED" to "开始使用",
        "CHOOSE_LANGUAGE" to "选择语言",

        // Login
        "SIGN_IN" to "登录",
        "SIGN_IN_WITH_GOOGLE" to "使用 Google 登录",
        "SIGN_OUT" to "退出登录",
        "WELCOME_BACK" to "欢迎回来",

        // Chat
        "CHAT" to "聊天",
        "TYPE_MESSAGE" to "输入消息...",
        "SEND" to "发送",
        "HOLD_TO_TALK" to "按住说话",
        "RELEASE_TO_SEND" to "松开发送",

        // History
        "HISTORY" to "历史",
        "NO_HISTORY" to "暂无聊天记录",
        "CLEAR_HISTORY" to "清除历史",

        // Settings
        "SETTINGS" to "设置",
        "DARK_MODE" to "深色模式",
        "NOTIFICATIONS" to "通知",
        "LANGUAGE" to "语言",
        "AI_MODEL" to "AI 模型",
        "RESPONSE_STYLE" to "回复风格",
        "VERSION" to "版本",
        "PRIVACY_POLICY" to "隐私政策",
        "ABOUT" to "关于",
        "MANAGE_CHARACTERS" to "管理角色",
        "MANAGE_VOICES" to "管理声音",
        "VOICE_COUNT" to "{n} 个克隆声音",
        "NO_VOICES" to "暂无克隆声音",
        "CLONE_FIRST_VOICE" to "克隆你的第一个声音",
        "VOICE_FOR" to "{name} 的声音",
        "PLAY" to "播放",
        "DELETE" to "删除",
        "CONFIRM_DELETE_VOICE" to "确定删除此声音？这也会从角色中移除该声音。",
        "VOICE_DELETED" to "声音已删除",
        "VOICE_PLAYING" to "播放中...",
        "VOICE_ERROR" to "播放失败",

        // Character
        "CHARACTERS" to "角色",
        "CREATE_CHARACTER" to "创建角色",
        "EDIT_CHARACTER" to "编辑角色",
        "DELETE_CHARACTER" to "删除角色",
        "CHARACTER_NAME" to "名称",
        "CHARACTER_GENDER" to "性别",
        "CHARACTER_RELATIONSHIP" to "关系",
        "CHARACTER_BACKGROUND" to "背景",
        "CHARACTER_PERSONALITY" to "性格",
        "CHARACTER_SPEAK_STYLE" to "说话风格",
        "CHARACTER_RULES" to "规则",
        "SAVE" to "保存",
        "CANCEL" to "取消",
        "DELETE" to "删除",
        "MALE" to "男",
        "FEMALE" to "女",
        "OTHER" to "其他",
        "NO_CHARACTERS" to "暂无角色",
        "CREATE_FIRST_CHARACTER" to "创建你的第一个角色",

        // Gender Selection
        "CHOOSE_GENDER" to "选择角色性别",
        "GENDER_DESC" to "为你的 AI 伙伴选择性别",

        // Avatar Selection
        "CHOOSE_AVATAR" to "选择头像",
        "UPLOAD_PHOTO" to "上传照片",
        "OR_CHOOSE_PRESET" to "或选择预设头像",

        // Basic Info
        "BASIC_INFO" to "基本资料",
        "NAME_REQUIRED" to "角色名称 *",
        "NAME_PLACEHOLDER" to "例如：晓晴、小明、Alex",
        "RELATIONSHIP_REQUIRED" to "与你的关系 *",
        "RELATIONSHIP_PLACEHOLDER" to "例如：朋友、恋人、老师、助手",
        "QUICK_SELECT" to "快速选择：",
        "ADD_MORE_DETAILS" to "添加更多细节",

        // Optional Details
        "MORE_DETAILS" to "更多细节",
        "OPTIONAL_SKIP" to "可选 - 可跳过",
        "PERSONALITY" to "性格描述",
        "PERSONALITY_PLACEHOLDER" to "例如：温柔、活泼、内向...",
        "BACKGROUND_STORY" to "背景故事",
        "BACKGROUND_PLACEHOLDER" to "描述角色的背景经历...",
        "SPEAK_STYLE" to "说话风格",
        "SPEAK_STYLE_PLACEHOLDER" to "例如：幽默、正式、可爱...",
        "DO_RULES" to "行为规则（应该做）",
        "DO_RULES_PLACEHOLDER" to "每行一条规则...",
        "DONT_RULES" to "行为规则（不应该做）",
        "DONT_RULES_PLACEHOLDER" to "每行一条规则...",
        "FINISH" to "完成创建",

        // Voice Clone
        "CLONE_VOICE_FOR" to "为 {name} 克隆声音",
        "CLONE_VOICE_DESC" to "录制 5-10 秒语音，让你的 AI 角色用你的声音说话",
        "HOLD_TO_RECORD" to "按住录音",
        "RELEASE_TO_STOP" to "松开停止",
        "SEND_CLONE" to "发送克隆",
        "CLONING" to "正在克隆声音...",
        "CLONE_SUCCESS" to "声音克隆成功！",

        // Dialogs
        "CONFIRM" to "确认",
        "CONFIRM_DELETE" to "确定要删除这个角色吗？",
        "OK" to "确定",

        // Navigation
        "BACK" to "返回",

        // Relationships
        "REL_FRIEND" to "朋友",
        "REL_LOVER" to "恋人",
        "REL_TEACHER" to "老师",
        "REL_ASSISTANT" to "助手",
        "REL_FAMILY" to "家人",
        "REL_CLASSMATE" to "同学",

        // Default Chatbots
        "DEFAULT_ASSISTANT" to "AI助手",
        "DEFAULT_TEACHER" to "老师",
        "DEFAULT_CODER" to "程序员",
        "CHOOSE_CHATBOT" to "选择你的AI助手",
        "DELETE_CHARACTER" to "删除角色",
        "DELETE_CHARACTER_CONFIRM" to "确定要删除 \"{name}\" 吗？此操作无法撤销。",
        "DELETE" to "删除",
        "CANCEL" to "取消",

        // Settings sections
        "GENERAL" to "常规",
        "AI_SETTINGS" to "AI 设置",
        "SELECT_LANGUAGE" to "选择语言",
        "STT_CONVERT" to "转换为文字",

        // TTS
        "TTS_PLAYING" to "播放中，请稍候...",

        // Voice Management Tabs
        "CLONED_VOICES" to "我的声音",
        "DEFAULT_VOICES" to "默认声音",
        "VOICE_MALE" to "男声",
        "VOICE_FEMALE" to "女声",
        "VOICE_PREVIEW" to "预览",

        // TTS Languages
        "LANG_ENGLISH" to "英语",
        "LANG_CHINESE" to "中文",
        "LANG_FRENCH" to "法语",
        "LANG_JAPANESE" to "日本語",
        "LANG_KOREAN" to "한국어",
        "LANG_SPANISH" to "Español",

        // Live Voice
        "LIVE_HOLD_TO_SPEAK" to "按住麦克风说话",
        "LIVE_LISTENING" to "正在聆听...",
        "LIVE_THINKING" to "思考中...",
        "LIVE_SPEAKING" to "正在回答...",
        "LIVE_RECORD_TOO_SHORT" to "录音太短，请按住麦克风说话",
        "LIVE_SPEECH_NOT_RECOGNIZED" to "未能识别语音，请重新按住麦克风说话",
        "LIVE_ERROR" to "出错了：{message}",
        "LIVE_PLAYBACK_FAILED" to "音频播放失败",
        "LIVE_MIC_PERMISSION" to "需要麦克风权限才能使用语音功能，请在系统设置中允许"
    )
}
