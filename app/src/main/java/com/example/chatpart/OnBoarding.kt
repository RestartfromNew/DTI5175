package com.example.chatpart

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class OnboardingManager(private val context: Context) {
    private val onBoardingStatus = booleanPreferencesKey("status")

    // SharedPreferences for synchronous access
    private val prefs: SharedPreferences = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)


    val counterFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[onBoardingStatus] ?: false
        }

    suspend fun completeOnboardingStatus() {
        context.dataStore.edit { preferences ->
            preferences[onBoardingStatus] = true
        }
    }

    // Synchronous method to check if onboarding is completed
    fun isCompleted(): Boolean {
        return prefs.getBoolean("onboarding_completed", false)
    }

    // Synchronous method to mark onboarding as completed
    fun setCompleted() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
    }

    // Reset onboarding status (for development/testing)
    fun reset() {
        prefs.edit().remove("onboarding_completed").apply()
    }
}