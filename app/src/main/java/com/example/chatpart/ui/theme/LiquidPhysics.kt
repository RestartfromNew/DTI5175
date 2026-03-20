package com.example.chatpart.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 流语 LiveVoice - 液态物理动画配置
 *
 * 所有交互都带有物理弹性、惯性和阻尼，给用户「活的界面」的感受
 */
object LiquidPhysics {

    // ==================== 弹性动画参数 ====================
    val SpringDamping = 0.6f
    val SpringStiffness = 300f
    val SpringMass = 1f

    /**
     * 麦克风按钮弹性动画
     */
    val MicButtonSpring = spring<Float>(
        dampingRatio = SpringDamping,
        stiffness = SpringStiffness
    )

    /**
     * 气泡弹出动画（更弹性）
     */
    val BubbleSpring = spring<Float>(
        dampingRatio = 0.5f,
        stiffness = 200f
    )

    // ==================== 缓动曲线 ====================

    /**
     * 液态流动缓动（标准）
     */
    val LiquidEase = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1f)

    /**
     * 液态流动缓动（弹性）
     */
    val LiquidBounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

    /**
     * 快速出缓动
     */
    val QuickOut = FastOutSlowInEasing

    /**
     * 线性缓动
     */
    val Linear = LinearEasing

    // ==================== 时长配置 ====================
    val ShortDuration = 200  // 短时动画 (ms)
    val MediumDuration = 400 // 中时动画 (ms)
    val LongDuration = 800   // 长时动画 (ms)
    val WaveRefreshRate = 16 // 波形刷新率 (ms) -> 60fps

    // ==================== 流动参数 ====================
    val MetaballSpeed = 0.0005f
    val ParticleFadeSpeed = 0.02f
    val WaveAmplitude = 24.dp
    val FlowSpeed = 0.0003f

    // ==================== 波形参数 ====================
    const val WaveBarCount = 32
    const val WaveBarWidthRatio = 0.7f
    const val WaveGapRatio = 0.3f
    const val WaveMaxHeightRatio = 0.9f

    // ==================== 能量球参数 ====================
    const val EnergyBallMinScale = 0.8f
    const val EnergyBallMaxScale = 1.2f
    const val EnergyBallPulseSpeed = 1500 // ms
    const val EnergyBallRotationSpeed = 3000 // ms

    // ==================== 打断动画参数 ====================
    const val InterruptDrainSpeed = 500 // ms
    const val InterruptSplashCount = 5
}

/**
 * 液态流动无限动画 Transition
 */
@Composable
fun rememberLiquidFlowTransition(
    label: String = "liquidFlow"
): Pair<Float, Float> {
    val infiniteTransition = rememberInfiniteTransition(label = label)

    // 流动偏移量（永不停歇）
    val flowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flowOffset"
    )

    // 脉冲强度（能量球呼吸）
    val pulseIntensity by infiniteTransition.animateFloat(
        initialValue = LiquidPhysics.EnergyBallMinScale,
        targetValue = LiquidPhysics.EnergyBallMaxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                LiquidPhysics.EnergyBallPulseSpeed,
                easing = LiquidPhysics.LiquidBounce
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseIntensity"
    )

    return flowOffset to pulseIntensity
}

/**
 * 波形相位动画
 */
@Composable
fun rememberWavePhase(): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "waveFlow")
    return infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    ).value
}
