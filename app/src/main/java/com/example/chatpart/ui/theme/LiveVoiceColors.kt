package com.example.chatpart.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 流语 LiveVoice - 液态流动主题色彩系统
 *
 * 设计灵感：深空中的能量流动，如水银般自然流动，像能量流一样充满生命力
 */

// ==================== 深空背景色 ====================
val DeepSpaceBlack = Color(0xFF0A0A0A)       // 主背景
val AbyssGray = Color(0xFF141414)            // 次级背景（卡片、面板）
val NebulaGray = Color(0xFF1E1E1E)           // 表面层（按钮、输入框）
val NebulaPurple = Color(0xFF1A0A2E)          // 背景渐变中心

// ==================== 主渐变色 ====================
val ElectricPurple = Color(0xFF6E00FF)      // 主渐变起点（电光紫）
val QuantumBlue = Color(0xFF00F5FF)         // 主渐变终点（量子蓝）
val FlowCyan = Color(0xFF00D9FF)            // 流动青

// ==================== 点缀色 ====================
val NebulaPink = Color(0xFFFF6B9D)          // 用户消息气泡（星云粉）
val EnergyOrange = Color(0xFFFF9D5C)        // AI 回复气泡（能量橙）
val EnergyYellow = Color(0xFFFFD93D)        // 高亮强调

// ==================== 文字色 ====================
val MoonlightWhite = Color(0xFFF5F5F7)       // 主要文字（月光白）
val StardustGray = Color(0xFF8E8E93)         // 次要文字（星尘灰）
val DimGray = Color(0xFF5C5C5C)              // 暗色文字

// ==================== 状态色 ====================
val FlowGreen = Color(0xFF00D68F)           // 录音状态、成功提示（流动绿）
val AlertRed = Color(0xFFFF4757)            // 错误提示（警示红）
val WarningAmber = Color(0xFFFFB800)        // 警告色

// ==================== 透明度变体 ====================
val ElectricPurpleAlpha50 = Color(0x806E00FF)
val ElectricPurpleAlpha30 = Color(0x4D6E00FF)
val QuantumBlueAlpha50 = Color(0x8000F5FF)
val QuantumBlueAlpha30 = Color(0x4D00F5FF)

// ==================== 渐变刷子 ====================

/**
 * 主要流动渐变（用于按钮、波形）
 */
val FlowGradient = Brush.horizontalGradient(
    colors = listOf(ElectricPurple, QuantumBlue)
)

/**
 * 垂直流动渐变
 */
val VerticalFlowGradient = Brush.verticalGradient(
    colors = listOf(ElectricPurple, QuantumBlue)
)

/**
 * 背景流动渐变（慢速、柔和）
 */
val BackgroundFlowGradient = Brush.radialGradient(
    colors = listOf(
        NebulaPurple,
        DeepSpaceBlack
    ),
    radius = 1200f
)

/**
 * 能量球渐变（AI 思考状态）
 */
val EnergyBallGradient = Brush.radialGradient(
    colors = listOf(
        QuantumBlue,     // 中心亮蓝
        ElectricPurple,  // 边缘紫
        Color.Transparent
    )
)

/**
 * 用户消息气泡渐变
 */
val UserBubbleGradient = Brush.horizontalGradient(
    colors = listOf(
        NebulaPink,
        NebulaPink.copy(alpha = 0.8f)
    )
)

/**
 * AI 消息气泡渐变
 */
val AIBubbleGradient = Brush.horizontalGradient(
    colors = listOf(
        EnergyOrange,
        EnergyOrange.copy(alpha = 0.8f)
    )
)

/**
 * 麦克风按钮渐变
 */
val MicButtonGradient = Brush.radialGradient(
    colors = listOf(
        ElectricPurple,
        QuantumBlue
    ),
    radius = 1f
)
