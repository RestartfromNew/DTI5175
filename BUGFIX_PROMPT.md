# Bug Fix 任务：ChatPart 应用修复

## 任务目标

修复以下三个问题：
1. Dark Mode Toggle 不工作
2. 语言问题（AI 总是回复中文）
3. Hold to Talk 按钮无反应 + 添加语音转文字按钮 + 触感反馈

---

## 问题 1：Dark Mode Toggle 不工作

### 当前代码 (SettingsScreen.kt)

```kotlin
var darkModeEnabled by remember { mutableStateOf(false) }
```

这个状态只是 SettingsScreen 本地的，没有传递给 Theme。

### 修复方案

1. **在 MainActivity 中添加 Dark Mode 状态管理**：
   - 使用 DataStore 或 remember 保存 darkModeEnabled 状态
   - 将状态通过 MainTabScreen 传递到 ChatPartTheme

2. **修改 MainActivity.kt**：
   ```kotlin
   // 添加全局状态
   var isDarkMode by remember(false) }

   { mutableStateOf // 传递给 MainTabScreen
   MainTabScreen(
       isDarkMode = isDarkMode,
       // ...其他参数
   )
   ```

3. **修改 MainTabScreen.kt**：
   ```kotlin
   @Composable
   fun MainTabScreen(
       isDarkMode: Boolean = false,
       onDarkModeChange: (Boolean) -> Unit = {},
       // ...
   ) {
       ChatPartTheme(darkTheme = isDarkMode) {
           // 内容
       }
   }
   ```

4. **修改 SettingsScreen.kt**：
   - 接收 isDarkMode 和 onDarkModeChange 参数
   - 将 Switch 的 checked/onCheckedChange 连接到传入的参数

---

## 问题 2：语言问题（AI 总是回复中文）

### 当前 Prompt.kt 问题

```kotlin
val systemRules = """
    - Follow the user's using language.  // 语法错误
    - Whenever the user mentions... [MEM: 简短的中文事实]  // 暗示中文
"""
```

### 修复方案

1. **修改 Prompt.kt**：

```kotlin
val systemRules = """
    - Reply in the SAME language as the user uses.
    - If user writes in English, reply in English.
    - If user writes in Chinese, reply in Chinese.
    - When user mentions personal preferences, habits, or important info,
      add a hidden tag at the end: [MEM: brief fact in the SAME language as user's message]
    - Tags are invisible to users but important for memory.
    - If new info contradicts previous memory, prioritize new info.
""".trimIndent()
```

2. **修改 AITest.kt 中的提示**：

```kotlin
val finalPrompt = "$systemPrompt \nPlease show the reply including [EMOTION: 状态] tag. Select one from: HAPPY, SAD, ANGRY, SHY, NEUTRAL. Reply in the SAME language as the user's message."
```

---

## 问题 3：Hold to Talk 改进

### 当前问题

- 按钮按了没有反应
- 缺少触感反馈 (Haptic)
- 缺少独立的语音转文字按钮

### 修复方案

1. **修复 VoiceRecordButton**：

```kotlin
@Composable
fun VoiceRecordButton(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
) {
    val context = LocalContext.current
    val haptic = remember {(context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?)}

    val bgColor by animateColorAsState(
        if (isRecording) Color(0xFFFF4444).copy(alpha = 0.1f) else Color(0xFFF0F0F0),
        label = "recordBg"
    )

    Surface(
        onClick = {}, // 不使用 onClick
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        modifier = modifier
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // 按下时：开始录音 + 触感反馈
                        haptic?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        onStartRecording()
                        tryAwaitRelease()
                        // 松开时：停止录音 + 触感反馈
                        haptic?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        onStopRecording()
                    }
                )
            }
    ) {
        // ... UI
    }
}
```

2. **添加独立的语音转文字按钮**：

在 ChatScreen.kt 的输入区域，添加一个独立的按钮（MIC 图标），点击后：
- 开始录音
- 显示录音中的 UI
- 录音结束后自动转为文字
- 显示文字在输入框中，允许用户编辑后发送

```kotlin
// 添加状态
var isSpeechToTextMode by remember { mutableStateOf(false) }

// 在输入区域添加按钮
IconButton(
    onClick = {
        if (!isSpeechToTextMode) {
            // 开始语音识别
            isSpeechToTextMode = true
            // TODO: 实现语音转文字
        } else {
            // 停止语音识别
            isSpeechToTextMode = false
        }
    }
) {
    Icon(
        if (isSpeechToTextMode) Icons.Filled.Mic else Icons.Filled.Mic,
        contentDescription = "Speech to text",
        tint = if (isSpeechToTextMode) Peach else Color.Gray
    )
}
```

3. **整体输入区域布局改进**：

```
[Voice/Keyboard] [Input Field] [Speech to Text] [Send]
```

- Voice/Keyboard: 切换语音输入模式
- Input Field: 文本输入
- Speech to Text: 独立的语音转文字按钮（点击开始，松开停止，识别结果放入输入框）
- Send: 发送按钮

---

## 交付要求

1. **必须验证**: `./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`
2. Dark Mode Toggle 可以切换主题
3. AI 回复语言与用户输入语言一致
4. Hold to Talk 有触感反馈
5. 有独立的语音转文字按钮

---

## 注意事项

- 使用 Android 的 Vibrator API 实现触感反馈
- 语音转文字可以使用 Android 的 SpeechRecognizer 或第三方 API
- 确保权限：RECORD_AUDIO, VIBRATE