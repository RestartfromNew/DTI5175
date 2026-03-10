package com.example.chatpart.i18n

data class Language(
    val code: String,
    val displayName: String,  // 显示的名字（用该语言本身）
    val flag: String          // 国旗 emoji
)

object Languages {
    val SUPPORTED_LANGUAGES = listOf(
        Language("en", "English", "🇨🇦"),
        Language("fr", "Français", "🇨🇦"),
        Language("zh", "中文", "🇨🇳")
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
}
