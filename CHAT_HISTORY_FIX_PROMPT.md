# Bug Fix 任务：聊天记录持久化

## 任务目标

修复聊天记录在应用关闭后丢失的问题。实现聊天记录的本地持久化存储。

---

## 问题分析

### 当前代码 (ChatScreen.kt)

```kotlin
// 聊天消息存储在内存中，应用关闭后丢失
var messages by remember {
    mutableStateOf(listOf(ChatMessage(defaultChatbots[0].greeting, isFromUser = false)))
}
```

### 问题

- 使用 `remember` 存储消息
- 应用退出/杀死后，内存数据被清除
- 重新打开应用时，聊天记录为空

---

## 修复方案

### 方案：使用 DataStore 持久化聊天记录

#### 1. 创建 ChatDataStore

创建文件 `app/src/main/java/com/example/chatpart/data/ChatDataStore.kt`：

```kotlin
package com.example.chatpart.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.chatDataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_history")

class ChatDataStore(private val context: Context) {

    companion object {
        private val CHAT_MESSAGES_KEY = stringPreferencesKey("chat_messages")
    }

    // 保存消息到 DataStore (JSON 格式)
    suspend fun saveMessages(messages: List<ChatMessageData>) {
        val json = messagesToJson(messages)
        context.chatDataStore.edit { preferences ->
            preferences[CHAT_MESSAGES_KEY] = json
        }
    }

    // 从 DataStore 读取消息
    fun getMessages(): Flow<List<ChatMessageData>> {
        return context.chatDataStore.data.map { preferences ->
            val json = preferences[CHAT_MESSAGES_KEY] ?: ""
            if (json.isEmpty()) emptyList() else jsonToMessages(json)
        }
    }

    // 清空聊天记录
    suspend fun clearMessages() {
        context.chatDataStore.edit { preferences ->
            preferences.remove(CHAT_MESSAGES_KEY)
        }
    }

    // 简单的 JSON 序列化/反序列化
    private fun messagesToJson(messages: List<ChatMessageData>): String {
        return messages.joinToString("|||") { msg ->
            "${msg.text}|||${msg.isFromUser}|||${msg.isVoice}|||${msg.voiceDurationSec}"
        }
    }

    private fun jsonToMessages(json: String): List<ChatMessageData> {
        return json.split("|||").chunked(4).mapNotNull { parts ->
            if (parts.size >= 2) {
                ChatMessageData(
                    text = parts[0],
                    isFromUser = parts[1].toBoolean(),
                    isVoice = parts.getOrNull(2)?.toBoolean() ?: false,
                    voiceDurationSec = parts.getOrNull(3)?.toIntOrNull() ?: 0
                )
            } else null
        }
    }
}

// 数据类（用于序列化）
data class ChatMessageData(
    val text: String,
    val isFromUser: Boolean,
    val isVoice: Boolean = false,
    val voiceDurationSec: Int = 0
)
```

#### 2. 修改 ChatScreen.kt

```kotlin
// 在 ChatScreen 中添加
val context = LocalContext.current
val chatDataStore = remember { ChatDataStore(context) }

// 加载保存的消息
var messages by remember {
    mutableStateOf(listOf(ChatMessage(defaultChatbots[0].greeting, isFromUser = false)))
}

// 首次加载时从 DataStore 读取
LaunchedEffect(Unit) {
    chatDataStore.getMessages().collect { savedMessages ->
        if (savedMessages.isNotEmpty()) {
            messages = savedMessages.map {
                ChatMessage(
                    text = it.text,
                    isFromUser = it.isFromUser,
                    isVoice = it.isVoice,
                    voiceDurationSec = it.voiceDurationSec
                )
            }
        }
    }
}

// 每次消息变化时保存到 DataStore
LaunchedEffect(messages) {
    val dataMessages = messages.map {
        ChatMessageData(
            text = it.text,
            isFromUser = it.isFromUser,
            isVoice = it.isVoice,
            voiceDurationSec = it.voiceDurationSec
        )
    }
    chatDataStore.saveMessages(dataMessages)
}
```

#### 3. 或者更简单的方案 - 使用 SharedPreferences

如果只需要存储少量消息，可以使用 SharedPreferences：

```kotlin
// 创建简单的存储帮助类
class ChatHistoryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)

    fun saveMessages(messages: List<ChatMessageData>) {
        val json = messages.joinToString(";;") { msg ->
            "${msg.text};;${msg.isFromUser};;${msg.isVoice};;${msg.voiceDurationSec}"
        }
        prefs.edit().putString("messages", json).apply()
    }

    fun loadMessages(): List<ChatMessageData> {
        val json = prefs.getString("messages", "") ?: ""
        if (json.isEmpty()) return emptyList()
        return json.split(";;").mapNotNull { parts ->
            val arr = parts.split(";;")
            if (arr.size >= 2) {
                ChatMessageData(arr[0], arr[1].toBoolean(), arr.getOrNull(2)?.toBoolean() ?: false, arr.getOrNull(3)?.toIntOrNull() ?: 0)
            } else null
        }
    }

    fun clearMessages() {
        prefs.edit().clear().apply()
    }
}
```

---

## 交付要求

1. **必须验证**: `./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`
2. 发送消息后，关闭应用
3. 重新打开应用，聊天记录仍然存在
4. 可以选择清空聊天记录的功能（可选）

---

## 注意事项

- 使用 DataStore 或 SharedPreferences 都可以
- 首次启动时，如果存储中有消息，需要加载并显示
- 新消息需要实时保存到本地存储
- 考虑只保存最近的 N 条消息，避免存储过大