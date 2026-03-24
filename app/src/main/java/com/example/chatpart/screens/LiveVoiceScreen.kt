package com.example.chatpart.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.chatpart.api.BotVideoSource
import com.example.chatpart.api.SdpAdapter
import com.example.chatpart.api.WebRTCManager
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.chatpart.audio.LiveVoiceController
import com.example.chatpart.api.MiniMaxAudioClient
import com.example.chatpart.data.PersonChat
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Profile
import com.example.chatpart.i18n.Languages
import com.example.chatpart.ui.theme.DeepSpaceBlack
import com.example.chatpart.ui.theme.ElectricPurple
import com.example.chatpart.ui.theme.EnergyBallGradient
import com.example.chatpart.ui.theme.FlowGradient
import com.example.chatpart.ui.theme.FlowGreen
import com.example.chatpart.ui.theme.MoonlightWhite
import com.example.chatpart.ui.theme.NebulaGray
import com.example.chatpart.ui.theme.NebulaPink
import com.example.chatpart.ui.theme.QuantumBlue
import com.example.chatpart.ui.theme.StardustGray
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Live Voice 通话状态枚举
 */
enum class LiveVoiceState {
    IDLE,           // 空闲状态
    USER_SPEAKING,  // 用户正在说话
    PROCESSING,     // AI 处理中
    AI_SPEAKING     // AI 正在说话/播放
}

/**
 * 流语 LiveVoice - 沉浸式实时语音对话界面
 *
 * 采用「液态流动（Liquid Flow）」主题，如水银般自然流动，像能量流一样充满生命力
 */
@Composable
fun LiveVoiceScreen(
    character: Profile,
    brain: PersonChat,
    chatHistory: List<Message>,
    onEndCall: () -> Unit,
    onMessageAdded: (userText: String, aiText: String) -> Unit,
    modifier: Modifier = Modifier,
    currentLanguage: String = "en"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Helper function for localized strings
    fun t(key: String) = Languages.getString(currentLanguage, key)

    // 通话时长
    var callDuration by remember { mutableLongStateOf(0L) }

    // 音频振幅数据（来自真实麦克风）
    var amplitudes by remember { mutableStateOf(List(32) { 0.1f }) }

    // 麦克风按下状态
    var isMicPressed by remember { mutableStateOf(false) }

    // 字幕内容
    var userCaption by remember { mutableStateOf("") }
    var aiCaption by remember { mutableStateOf("") }

    // 语音状态
    var voiceState by remember { mutableStateOf(LiveVoiceState.IDLE) }

    // 错误提示
    var errorMessage by remember { mutableStateOf("") }

    // 挂断确认 dialog
    var showEndCallDialog by remember { mutableStateOf(false) }

    // 视频开关 — None=能量球, LocalSample=占位视频, WebRTC=真实推流
    var botVideoSource by remember { mutableStateOf<BotVideoSource>(BotVideoSource.None) }
    var isUserCamOn by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isUserCamOn = true
            botVideoSource = BotVideoSource.LocalSample // 占位视频先显示
            // TODO: 替换为真实地址时改成:
            // botVideoSource = BotVideoSource.WebRTC("ws://your-gpu-server/webrtc")
        }
    }

    // 默认角色名称跟随语言设置
    val displayName = when (character.id) {
        "char_001" -> t("DEFAULT_ASSISTANT")
        "char_002" -> t("DEFAULT_TEACHER")
        "char_003" -> t("DEFAULT_CODER")
        else -> character.name
    }

    // 派生状态
    val isUserSpeaking = voiceState == LiveVoiceState.USER_SPEAKING
    val isAISpeaking = voiceState == LiveVoiceState.AI_SPEAKING

    // 麦克风权限
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            errorMessage = t("LIVE_MIC_PERMISSION")
            scope.launch {
                delay(4000)
                errorMessage = ""
            }
        }
    }

    // 创建 Controller
    val audioClient = remember { MiniMaxAudioClient(context) }
    val controller = remember {
        LiveVoiceController(context, brain, audioClient, scope, currentLanguage)
    }

    // 绑定 Controller 回调
    LaunchedEffect(controller) {
        controller.onStateChanged = { state ->
            voiceState = state
        }
        controller.onUserCaptionUpdated = { text ->
            userCaption = text
        }
        controller.onAICaptionUpdated = { text ->
            aiCaption = text
        }
        controller.onAmplitudesUpdated = { amps ->
            amplitudes = amps
        }
        controller.onError = { msg ->
            errorMessage = msg
            scope.launch {
                delay(3000)
                errorMessage = ""
            }
        }
    }

    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            controller.release()
        }
    }

    // 生命周期监听（防止后台继续播放）
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                controller.stopListening()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 启动计时器
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callDuration += 1000
            // IDLE 状态下保持微弱波形
            if (voiceState == LiveVoiceState.IDLE) {
                amplitudes = List(32) { 0.1f }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBlack)
    ) {
        // 1. 流动背景
        LiquidBackground()

        // 2. 顶部状态栏
        TopStatusBar(
            characterName = displayName,
            duration = callDuration,
            onSettingsClick = { /* TODO: 打开设置 */ },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. 中央区域 — 有视频时显示 bot 视频，否则显示能量球
        if (botVideoSource == BotVideoSource.None) {
            CentralEnergyBall(
                isAISpeaking = isAISpeaking,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            BotVideoArea(
                source = botVideoSource,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(360.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
        }

        // 4. 字幕区域（能量球下方）
        CaptionArea(
            userCaption = userCaption,
            aiCaption = aiCaption,
            voiceState = voiceState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 280.dp),
            currentLanguage = currentLanguage
        )

        // 5. 实时语音波形
        LiquidVoiceWave(
            amplitudes = amplitudes,
            isUserSpeaking = isUserSpeaking,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
        )

        // 6. 状态提示文字（麦克风按钮上方）
        val statusHint = when (voiceState) {
            LiveVoiceState.IDLE         -> t("LIVE_HOLD_TO_SPEAK")
            LiveVoiceState.USER_SPEAKING -> t("LIVE_LISTENING")
            LiveVoiceState.PROCESSING   -> t("LIVE_THINKING")
            LiveVoiceState.AI_SPEAKING  -> t("LIVE_SPEAKING")
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 148.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33FF4757))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = statusHint,
                color = StardustGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        // 8. 挂断确认 dialog
        if (showEndCallDialog) {
            AlertDialog(
                onDismissRequest = { showEndCallDialog = false },
                title = {
                    Text(
                        text = t("LIVE_END_CALL_TITLE"),
                        color = MoonlightWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = t("LIVE_END_CALL_CONFIRM"),
                        color = StardustGray
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEndCallDialog = false
                            controller.release()
                            onEndCall()
                        }
                    ) {
                        Text(
                            text = t("LIVE_END_CALL_YES"),
                            color = Color(0xFFFF4757)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndCallDialog = false }) {
                        Text(
                            text = t("LIVE_END_CALL_NO"),
                            color = ElectricPurple
                        )
                    }
                },
                containerColor = Color(0xFF1A1A2E),
                titleContentColor = MoonlightWhite,
                textContentColor = StardustGray
            )
        }

        // 用户摄像头预览（右上角，视频开启时显示）
        if (isUserCamOn) {
            CameraPreviewView(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(width = 120.dp, height = 160.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        // 7. 底部麦克风按钮
        BottomMicControls(
            isMicPressed = isMicPressed,
            isUserSpeaking = isUserSpeaking,
            onMicPress = {
                isMicPressed = true
                if (voiceState == LiveVoiceState.IDLE) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        controller.startListening(character, chatHistory)
                    } else {
                        isMicPressed = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            onMicRelease = {
                isMicPressed = false
                if (voiceState == LiveVoiceState.USER_SPEAKING) {
                    controller.stopListening()
                }
            },
            onEndCall = {
                showEndCallDialog = true
            },
            isVideoOn = botVideoSource != BotVideoSource.None,
            onVideoToggle = {
                if (botVideoSource == BotVideoSource.None) {
                    val hasCamera = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasCamera) {
                        isUserCamOn = true
                        botVideoSource = BotVideoSource.LocalSample
                        // TODO: botVideoSource = BotVideoSource.WebRTC("ws://your-gpu-server/webrtc")
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                } else {
                    isUserCamOn = false
                    botVideoSource = BotVideoSource.None
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}

/**
 * 字幕显示区域
 */
@Composable
fun CaptionArea(
    userCaption: String,
    aiCaption: String,
    voiceState: LiveVoiceState,
    modifier: Modifier = Modifier,
    currentLanguage: String = "en"
) {
    fun t(key: String) = Languages.getString(currentLanguage, key)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 用户字幕
        if (userCaption.isNotEmpty()) {
            CaptionBubble(
                text = userCaption,
                isUser = true,
                isPartial = voiceState == LiveVoiceState.USER_SPEAKING
            )
        }

        Spacer(Modifier.height(12.dp))

        // AI 字幕 / 处理中提示
        when (voiceState) {
            LiveVoiceState.PROCESSING -> {
                CaptionBubble(
                    text = t("LIVE_THINKING"),
                    isUser = false,
                    isPartial = true
                )
            }
            LiveVoiceState.AI_SPEAKING -> {
                if (aiCaption.isNotEmpty()) {
                    CaptionBubble(
                        text = aiCaption,
                        isUser = false,
                        isPartial = false
                    )
                }
            }
            else -> {}
        }
    }
}

/**
 * 字幕气泡组件
 */
@Composable
fun CaptionBubble(
    text: String,
    isUser: Boolean,
    isPartial: Boolean,
    modifier: Modifier = Modifier
) {
    // snap() 放进 infiniteRepeatable 会导致 duration=0 → ArithmeticException: divide by zero
    // 修复：始终用 tween(600)，当 isPartial=false 时直接覆盖为 1f
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPartial) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    val alpha = if (isPartial) alphaAnim else 1f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isUser)
                    Color(0x33B6A6FF)  // 紫色半透明
                else
                    Color(0x3300D4FF)  // 蓝色半透明
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (isPartial) "$text ▌" else text,
            color = MoonlightWhite.copy(alpha = if (isPartial) alpha else 1f),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 液态流动背景
 */
@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    // 背景流动偏移
    val bgOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset1"
    )

    val bgOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgOffset2"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 渐变背景
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A0A2E).copy(alpha = 0.8f),
                    DeepSpaceBlack
                ),
                center = Offset(
                    width * (0.3f + bgOffset1 * 0.4f),
                    height * (0.3f + bgOffset2 * 0.4f)
                ),
                radius = width * 0.8f
            )
        )

        // 流动光点
        for (i in 0..5) {
            val x = width * (0.1f + (i * 0.15f + bgOffset1) % 1f)
            val y = height * (0.2f + (i * 0.2f + bgOffset2) % 1f)
            val alpha = 0.1f + 0.1f * sin((bgOffset1 + i) * 2 * PI).toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricPurple.copy(alpha = alpha),
                        Color.Transparent
                    )
                ),
                radius = 50f + i * 20f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * 顶部状态栏
 */
@Composable
fun TopStatusBar(
    characterName: String,
    duration: Long,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seconds = (duration / 1000) % 60
    val minutes = (duration / 1000 / 60) % 60

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 角色名称
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(FlowGreen)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = characterName,
                color = MoonlightWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 通话时长
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            color = StardustGray,
            fontSize = 16.sp
        )

        // 设置按钮
        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "Settings",
                tint = StardustGray
            )
        }
    }
}

/**
 * 中央能量球（AI 思考/说话状态）
 */
@Composable
fun CentralEnergyBall(
    isAISpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "energyBall")

    // 脉冲动画
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1500,
                easing = androidx.compose.animation.core.CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // 旋转动画
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // AI 说话时的振幅
    val speakScale = if (isAISpeaking) {
        val amplitude = sin(System.currentTimeMillis() / 150.0).toFloat().coerceIn(0.9f, 1.1f)
        scale * amplitude
    } else {
        scale
    }

    Box(
        modifier = modifier
            .size(200.dp)
            .scale(speakScale),
        contentAlignment = Alignment.Center
    ) {
        // 外圈光晕
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            // 绘制多层光晕
            for (i in 3 downTo 0) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            QuantumBlue.copy(alpha = 0.2f - i * 0.04f),
                            ElectricPurple.copy(alpha = 0.1f - i * 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = size.width * (0.4f + i * 0.15f)
                    ),
                    radius = size.width * (0.4f + i * 0.15f),
                    center = Offset(centerX, centerY)
                )
            }
        }

        // 主能量球
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = NebulaGray
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                QuantumBlue.copy(alpha = 0.8f),
                                ElectricPurple.copy(alpha = 0.6f),
                                NebulaGray
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // AI 图标
                AnimatedVisibility(
                    visible = !isAISpeaking,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = "AI",
                        tint = QuantumBlue,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // 说话中的波形指示
                AnimatedVisibility(
                    visible = isAISpeaking,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Canvas(modifier = Modifier.size(60.dp)) {
                        val barCount = 5
                        val barWidth = size.width / barCount * 0.4f
                        val gap = size.width / barCount * 0.6f
                        val centerY = size.height / 2

                        for (i in 0 until barCount) {
                            val amplitude = sin((System.currentTimeMillis() / 100.0 + i * 0.8)).toFloat().coerceIn(0.3f, 1f)
                            val barHeight = size.height * 0.8f * amplitude
                            val x = i * (barWidth + gap)

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(QuantumBlue, ElectricPurple)
                                ),
                                topLeft = Offset(x, centerY - barHeight / 2),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 液态语音波形
 *
 * 用户说话时，底部出现动态液体声波。波峰向上流动，像水银柱一样有惯性
 */
@Composable
fun LiquidVoiceWave(
    amplitudes: List<Float>,
    isUserSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

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

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        val barCount = amplitudes.size.coerceAtLeast(32)
        val barWidth = width / barCount * 0.7f
        val gap = width / barCount * 0.3f

        amplitudes.forEachIndexed { index, amplitude ->
            // 液态波浪：从底部延伸，带有流动相位
            val phaseOffset = (index.toFloat() / barCount + flowOffset) % 1f
            val waveHeight = amplitude * height * 0.9f

            // 计算位置（从底部向上流动）
            val x = index * (barWidth + gap) + gap / 2

            // 液滴形状（两端圆滑）
            val path = Path().apply {
                val topY = centerY - waveHeight / 2
                val bottomY = centerY + waveHeight / 2

                // 流动效果：顶部带有轻微波动
                val waveTop = topY + sin(phaseOffset * 2 * PI).toFloat().dp.toPx() * 4

                moveTo(x, bottomY)
                lineTo(x, waveTop)
                quadraticBezierTo(
                    x + barWidth / 2, topY - 4.dp.toPx(),
                    x + barWidth, waveTop
                )
                lineTo(x + barWidth, bottomY)
                close()
            }

            // 渐变色（紫色到蓝色）
            val gradient = Brush.verticalGradient(
                colors = listOf(
                    ElectricPurple,
                    QuantumBlue
                ),
                startY = centerY - waveHeight / 2,
                endY = centerY + waveHeight / 2
            )

            drawPath(path, gradient)

            // 高光效果（液体反光）
            if (amplitude > 0.3f) {
                drawPath(
                    path = Path().apply {
                        val topY = centerY - waveHeight / 2
                        val waveTop = topY + sin(phaseOffset * 2 * PI).toFloat().dp.toPx() * 4

                        moveTo(x + barWidth * 0.2f, waveTop + 2.dp.toPx())
                        quadraticBezierTo(
                            x + barWidth / 2, topY + 6.dp.toPx(),
                            x + barWidth * 0.8f, waveTop + 2.dp.toPx()
                        )
                    },
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
            }
        }
    }
}

/**
 * 底部麦克风控制按钮（LiveVoice 专用，简单的按住说话）
 */
@Composable
fun BottomMicControls(
    isMicPressed: Boolean,
    isUserSpeaking: Boolean,
    onMicPress: () -> Unit,
    onMicRelease: () -> Unit,
    onEndCall: () -> Unit,
    isVideoOn: Boolean = false,
    onVideoToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 结束通话按钮
        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onEndCall()
            },
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xFFFF4757),
                contentColor = Color.White
            )
        ) {
            Icon(
                Icons.Rounded.CallEnd,
                contentDescription = "End Call",
                modifier = Modifier.size(28.dp)
            )
        }

        // 麦克风按钮（按住说话）
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    brush = if (isUserSpeaking) {
                        Brush.radialGradient(listOf(FlowGreen, FlowGreen.copy(alpha = 0.6f)))
                    } else {
                        FlowGradient
                    }
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMicPress()
                            tryAwaitRelease()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onMicRelease()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = if (isUserSpeaking) FlowGreen else NebulaGray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = "Hold to Speak",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 视频切换按钮
        VideoToggleButton(
            isVideoOn = isVideoOn,
            onClick = onVideoToggle
        )
    }
}

@Composable
fun VideoToggleButton(
    isVideoOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isVideoOn) Color(0xFF1565C0) else Color(0xFF2A2A2A),
        label = "videoBg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isVideoOn) Color.White else Color(0xFF888888),
        label = "videoTint"
    )
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(64.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = bgColor)
    ) {
        Icon(
            imageVector = if (isVideoOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
            contentDescription = if (isVideoOn) "Turn off camera" else "Turn on camera",
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun CameraPreviewView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = CameraPreview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

// ── Bot Video ─────────────────────────────────────────────────────────────────

/**
 * Switches between local sample video (placeholder) and live WebRTC stream.
 * Wire up BotVideoSource.WebRTC when your GPU server's signaling URL is ready.
 */
@Composable
fun BotVideoArea(source: BotVideoSource, modifier: Modifier = Modifier) {
    when (source) {
        is BotVideoSource.None -> {} // shouldn't reach here — energy ball is shown instead
        is BotVideoSource.LocalSample -> LocalSampleVideoView(modifier = modifier)
        is BotVideoSource.WebRTC -> WebRTCVideoView(signalingUrl = source.signalingUrl, modifier = modifier)
    }
}

/** Loop bot_sample.mp4 as a placeholder for the bot's video feed */
@Composable
fun LocalSampleVideoView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/raw/bot_sample")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // muted — audio comes from LiveVoiceController
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = modifier
    )
}

/**
 * Renders live WebRTC video from the GPU server.
 *
 * To activate: set botVideoSource = BotVideoSource.WebRTC("ws://your-server/webrtc")
 * in LiveVoiceScreen's onVideoToggle.
 */
@Composable
fun WebRTCVideoView(signalingUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val eglBase = remember { EglBase.create() }
    val manager = remember { WebRTCManager(context) }
    val renderer = remember { SurfaceViewRenderer(context) }

    LaunchedEffect(signalingUrl) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setMirror(false)
        manager.initialize(eglBase)
        manager.connect(signalingUrl, renderer)
    }

    DisposableEffect(Unit) {
        onDispose {
            manager.release()
            renderer.release()
        }
    }

    AndroidView(factory = { renderer }, modifier = modifier)
}
