package com.example.chatpart.data

import android.content.Context

/**
 * Stores voice-to-character assignment mappings.
 *
 * Priority order when resolving a voice for TTS:
 *   1. VoiceAssignmentPreferences[characterId] — explicit per-character assignment
 *   2. Profile.voiceId                        — per-character voice set during onboarding
 *   3. System default voice                    — last resort fallback
 */
class VoiceAssignmentPreferences(context: Context, uid: String = "") {

    private val prefsName = if (uid.isNotEmpty()) "voice_assignments_$uid" else "voice_assignments"
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    /** Get the assigned voiceId for a character. Returns null if none assigned. */
    fun getVoiceId(characterId: String): String? =
        prefs.getString(characterId, null)

    /** Assign a voiceId to a character. Pass null to clear. */
    fun setVoiceId(characterId: String, voiceId: String?) {
        if (voiceId == null) {
            prefs.edit().remove(characterId).apply()
        } else {
            prefs.edit().putString(characterId, voiceId).apply()
        }
    }

    /** Get all characterIds that have this voiceId assigned. */
    fun getCharactersForVoice(voiceId: String): Set<String> =
        prefs.all
            .filter { it.value == voiceId }
            .keys

    /** Get all current assignments as a map of characterId → voiceId. */
    fun getAllAssignments(): Map<String, String> =
        prefs.all.mapValues { it.value as String }

    /** Remove all assignments for a given voiceId (e.g. when the voice is deleted). */
    fun clearVoice(voiceId: String) {
        val editor = prefs.edit()
        prefs.all.filter { it.value == voiceId }.keys.forEach { editor.remove(it) }
        editor.apply()
    }
}
