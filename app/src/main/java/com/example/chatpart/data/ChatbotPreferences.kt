package com.example.chatpart.data

import android.content.Context

/**
 * 保存用户偏好的 chatbot 选择，下次打开 App 时自动恢复
 */
class ChatbotPreferences(private val context: Context) {

    private val prefs = context.getSharedPreferences("chatbot_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SELECTED_BOT_ID = "selected_bot_id"
        private const val DEFAULT_BOT_ID = "assistant"  // 默认 chatbot ID
    }

    /**
     * 获取上次选择的 chatbot ID
     * 如果没有记录，返回默认的 "assistant"
     */
    fun getSelectedBotId(): String {
        return prefs.getString(KEY_SELECTED_BOT_ID, DEFAULT_BOT_ID) ?: DEFAULT_BOT_ID
    }

    /**
     * 保存用户选择的 chatbot ID
     */
    fun setSelectedBotId(botId: String) {
        prefs.edit().putString(KEY_SELECTED_BOT_ID, botId).apply()
    }
}

/**
 * 保存 UI 相关的用户偏好设置，如浮动按钮位置
 */
class UIPreferences(private val context: Context) {

    private val prefs = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CALL_BUTTON_OFFSET_X = "call_button_offset_x"
        private const val KEY_CALL_BUTTON_OFFSET_Y = "call_button_offset_y"
        private const val DEFAULT_OFFSET_X = -1f  // -1 表示使用默认值
        private const val DEFAULT_OFFSET_Y = -1f
    }

    /**
     * 获取通话按钮的 X 偏移量
     * 返回 -1 表示未保存，需要使用默认值
     */
    fun getCallButtonOffsetX(): Float {
        return prefs.getFloat(KEY_CALL_BUTTON_OFFSET_X, DEFAULT_OFFSET_X)
    }

    /**
     * 获取通话按钮的 Y 偏移量
     * 返回 -1 表示未保存，需要使用默认值
     */
    fun getCallButtonOffsetY(): Float {
        return prefs.getFloat(KEY_CALL_BUTTON_OFFSET_Y, DEFAULT_OFFSET_Y)
    }

    /**
     * 保存通话按钮的位置
     */
    fun setCallButtonOffset(offsetX: Float, offsetY: Float) {
        prefs.edit()
            .putFloat(KEY_CALL_BUTTON_OFFSET_X, offsetX)
            .putFloat(KEY_CALL_BUTTON_OFFSET_Y, offsetY)
            .apply()
    }
}
