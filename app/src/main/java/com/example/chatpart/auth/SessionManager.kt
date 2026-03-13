package com.example.chatpart.auth

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences("chatpart_session", Context.MODE_PRIVATE)

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        username: String,
        email: String
    ) {

        prefs.edit().apply {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
            putString("user_id", userId)
            putString("username", username)
            putString("email", email)
            apply()
        }
    }

    fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun getUserName(): String? {
        return prefs.getString("username", null)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}