package com.example.chatpart.i18n

data class Language(
    val code: String,
    val displayName: String,  // 显示的名字（用该语言本身）
    val flag: String,         // 国旗 emoji
    val nativeName: String    // 本地名称
)

object Languages {
    // App UI 语言支持 - 仅 3 种
    val SUPPORTED_LANGUAGES = listOf(
        Language("en", "English", "🇨🇦", "English"),
        Language("fr", "Français", "🇨🇦", "Français"),
        Language("zh", "中文", "🇨🇳", "中文")
    )

    // TTS 语音语言支持 - 6 种
    val TTS_LANGUAGES = listOf(
        Language("en", "English", "🇬🇧", "English"),
        Language("zh", "中文", "🇨🇳", "中文"),
        Language("fr", "Français", "🇫🇷", "Français"),
        Language("ja", "日本語", "🇯🇵", "日本語"),
        Language("ko", "한국어", "🇰🇷", "한국어"),
        Language("es", "Español", "🇪🇸", "Español")
    )

    fun getString(lang: String, key: String): String {
        return when (lang) {
            "en" -> EnglishStrings.strings[key] ?: key
            "fr" -> FrenchStrings.strings[key] ?: key
            "zh" -> ChineseStrings.strings[key] ?: key
            else -> EnglishStrings.strings[key] ?: key
        }
    }

    fun getLanguageByCode(code: String): Language? {
        return SUPPORTED_LANGUAGES.find { it.code == code }
    }

    fun getTtsLanguageByCode(code: String): Language? {
        return TTS_LANGUAGES.find { it.code == code }
    }
}
