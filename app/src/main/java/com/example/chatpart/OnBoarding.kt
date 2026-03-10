package com.example.chatpart

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class OnboardingManager(private val context: Context) {
    private val onBoardingStatus = booleanPreferencesKey("status")



    val counterFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[onBoardingStatus] ?: false
        }

    suspend fun completeOnboardingStatus() {
        context.dataStore.edit { preferences ->
            preferences[onBoardingStatus] = true
        }
    }
}