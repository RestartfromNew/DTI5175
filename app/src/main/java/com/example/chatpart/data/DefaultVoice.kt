package com.example.chatpart.data

/**
 * Default voice model for preset TTS voices
 * These are system voices provided by MiniMax API
 */
data class DefaultVoice(
    val id: String,           // MiniMax voice ID
    val displayName: String,  // Display name
    val language: String,     // Language code (en, zh, fr, ja, ko, es)
    val gender: String,       // male / female
    val description: String  // Short description
)

/**
 * Provider for default preset voices
 * Each language has 2-4 representative voices
 */
object DefaultVoices {
    //精选系统音色 - 每个语言选择 2-4 个最具代表性的
    val voices = listOf(
        // English (2)
        DefaultVoice("English_Trustworthy_Man", "Trustworthy Man", "en", "male", "成熟可靠男声"),
        DefaultVoice("English_Graceful_Lady", "Graceful Lady", "en", "female", "优雅女士"),

        // Chinese - Mandarin (4)
        DefaultVoice("male-qn-jingying", "精英青年", "zh", "male", "精英青年音色"),
        DefaultVoice("female-yujie", "御姐", "zh", "female", "成熟御姐音色"),
        DefaultVoice("Chinese_Mandarin_News_Anchor", "新闻女声", "zh", "female", "新闻主播女声"),
        DefaultVoice("Chinese_Mandarin_Gentleman", "温润男声", "zh", "male", "温润柔和男声"),

        // French (2)
        DefaultVoice("French_Male_Speech_New", "Level-Headed Man", "fr", "male", "稳重男声"),
        DefaultVoice("French_Female_News_Anchor", "Patient Female Presenter", "fr", "female", "温柔女声"),

        // Japanese (2)
        DefaultVoice("Japanese_IntellectualSenior", "Intellectual Senior", "ja", "male", "知性大叔"),
        DefaultVoice("Japanese_DecisivePrincess", "Decisive Princess", "ja", "female", "果断公主"),

        // Korean (2)
        DefaultVoice("Korean_SweetGirl", "Sweet Girl", "ko", "female", "甜美女孩"),
        DefaultVoice("Korean_CheerfulBoyfriend", "Cheerful Boyfriend", "ko", "male", "阳光男友"),

        // Spanish (2)
        DefaultVoice("Spanish_SereneWoman", "Serene Woman", "es", "female", "沉稳女性"),
        DefaultVoice("Spanish_MaturePartner", "Mature Partner", "es", "male", "成熟男性")
    )

    /**
     * Get voices filtered by language
     */
    fun getByLanguage(lang: String): List<DefaultVoice> =
        voices.filter { it.language == lang }

    /**
     * Get all unique languages
     */
    val languages: List<String> = voices.map { it.language }.distinct()
}
