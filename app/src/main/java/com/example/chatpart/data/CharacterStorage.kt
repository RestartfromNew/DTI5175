package com.example.chatpart.data

import android.content.Context
import com.example.chatpart.domain.Profile

/**
 * Character profile storage using SharedPreferences.
 *
 * IMPORTANT: Pass [uid] so each Firebase account gets its own storage namespace.
 * Without uid isolation, switching accounts shows the previous user's characters.
 *
 * SharedPreferences file naming:
 *   - Logged-in user:  "characters_{uid}"
 *   - Guest / no uid:  "characters"  (fallback, should not normally be used)
 */
class CharacterStorage(private val context: Context, uid: String = "") {

    private val prefsName = if (uid.isNotEmpty()) "characters_${uid}" else "characters"
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CHARACTERS = "character_list"
        private const val KEY_SELECTED_ID = "selected_character_id"
    }

    fun saveCharacters(characters: List<Profile>) {
        val json = characters.joinToString("|||") { profile ->
            listOf(
                profile.id,
                profile.name,
                profile.gender,
                profile.background,
                profile.relationship,
                profile.personality,
                profile.voiceId ?: "",
                profile.facetimeUrl ?: "",
                profile.customAvatarPath ?: "",
                profile.speakStyle.joinToString(",,"),
                profile.doRules.joinToString(",,"),
                profile.dontRules.joinToString(",,")
            ).joinToString(":::")
        }
        prefs.edit().putString(KEY_CHARACTERS, json).apply()
    }

    fun loadCharacters(): List<Profile> {
        val json = prefs.getString(KEY_CHARACTERS, "") ?: ""
        if (json.isEmpty()) return getDefaultCharacters()

        return json.split("|||").mapNotNull { profileStr ->
            try {
                val parts = profileStr.split(":::")
                if (parts.size >= 12) {
                    Profile(
                        id = parts[0],
                        name = parts[1],
                        gender = parts[2],
                        background = parts[3],
                        relationship = parts[4],
                        personality = parts[5],
                        voiceId = parts[6].ifEmpty { null },
                        facetimeUrl = parts[7].ifEmpty { null },
                        customAvatarPath = parts[8].ifEmpty { null },
                        speakStyle = parts[9].split(",,").filter { it.isNotEmpty() },
                        doRules = parts[10].split(",,").filter { it.isNotEmpty() },
                        dontRules = parts[11].split(",,").filter { it.isNotEmpty() }
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    fun saveSelectedCharacterId(id: String) {
        prefs.edit().putString(KEY_SELECTED_ID, id).apply()
    }

    fun getSelectedCharacterId(): String? {
        return prefs.getString(KEY_SELECTED_ID, null)
    }

    fun getSelectedCharacter(): Profile? {
        val selectedId = getSelectedCharacterId()
        val characters = loadCharacters()
        return characters.find { it.id == selectedId } ?: characters.firstOrNull()
    }

    private fun getDefaultCharacters(): List<Profile> {
        val defaultProfile = Profile(
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
        saveCharacters(listOf(defaultProfile))
        saveSelectedCharacterId(defaultProfile.id)
        return listOf(defaultProfile)
    }

    fun addCharacter(profile: Profile) {
        val characters = loadCharacters().toMutableList()
        characters.add(profile)
        saveCharacters(characters)
    }

    fun updateCharacter(profile: Profile) {
        val characters = loadCharacters().toMutableList()
        val index = characters.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            characters[index] = profile
            saveCharacters(characters)
        }
    }

    fun deleteCharacter(id: String) {
        val characters = loadCharacters().toMutableList()
        characters.removeAll { it.id == id }
        // Allow deleting all custom characters - users can still use the 3 default bots
        saveCharacters(characters)
        // If deleted character was selected, reset to the first remaining one (if any)
        if (getSelectedCharacterId() == id) {
            if (characters.isNotEmpty()) {
                saveSelectedCharacterId(characters.first().id)
            } else {
                // If no custom characters left, clear the selection (will use default bot)
                saveSelectedCharacterId("")
            }
        }
    }
}
