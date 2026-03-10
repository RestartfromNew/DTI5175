package com.example.chatpart.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

class LanguageManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_LANGUAGE = "language"
        const val DEFAULT_LANGUAGE = "en"
    }

    fun getCurrentLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(langCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, langCode).apply()
    }

    fun getString(key: String): String {
        return Languages.getString(getCurrentLanguage(), key)
    }

    // Extension function for convenience
    fun String.localize(): String {
        return Languages.getString(getCurrentLanguage(), this)
    }
}

// Composable function to get localized string
@Composable
fun LocalizedString(key: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = LanguageManager(context)
    return manager.getString(key)
}
