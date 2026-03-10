# Android 项目合并任务：ChatPart + dti-5175-main UI

## 任务目标

将 `~/Desktop/ChatPart_副本/` （核心对话逻辑 + AI 引擎）与 `~/Downloads/dti-5175-main/` （完整 UI + Onboarding）合并成一个可运行的 Android 应用。

---

## 项目 A：ChatPart（核心逻辑）- 位置：~/Desktop/ChatPart_副本/

### 核心组件

1. **PersonChat.kt** (`app/src/main/java/com/example/chatpart/data/PersonChat.kt`)
   - 对话引擎，调用 Gemini AI
   - 向量记忆检索
   - `[MEM: xxx]` 记忆提取
   - `[EMOTION: xxx]` 情绪识别

2. **Profile.kt** (`app/src/main/java/com/example/chatpart/domain/Profile.kt`)
   - 角色配置数据类
   ```kotlin
   data class Profile(
       val id: String,
       val name: String,
       val gender: String,
       val background: String,
       val relationship: String,
       val personality: String = "",
       val speakStyle: List<String> = emptyList(),
       val doRules: List<String> = emptyList(),
       val dontRules: List<String> = emptyList()
   )
   ```

3. **LlmClient.kt** - AI 接口
4. **AITest.kt** - Gemini 2.5 Flash 实现
5. **EmbeddingLlm.kt** - 向量化实现（Google text-embedding-004）
6. **InMemoryStore.kt** - 内存向量存储
7. **Prompt.kt** - 提示词构建器（构建角色 Prompt + 对话历史）
8. **Message.kt** - 消息数据结构（role: USER/ASSISTANT）
9. **Result.kt** - AI 回复结果（包含 replyText 和 emotion）

---

## 项目 B：dti-5175-main（UI + Onboarding）- 位置：~/Downloads/dti-5175-main/

### 核心组件

1. **MainActivity.kt** - 主入口，包含 Onboarding + 底部导航
2. **ChatScreen.kt** - 聊天界面（当前是 Mock 实现）
3. **OnBoarding.kt** - Onboarding 管理器
4. **CloudOnBoarding.kt** - 第二页 Onboarding
5. **LoginScreen.kt** - Google 登录
6. **HistoryScreen.kt** - 历史记录
7. **SettingsScreen.kt** - 设置页面

---

## 合并要求

### 1. 项目结构

保留 `ChatPart` 作为项目名称，使用包名 `com.example.chatpart`

### 2. 保留的核心功能

- **AI 对话**：使用 PersonChat 的真实 Gemini 调用
- **角色系统**：使用 Profile 定义角色
- **记忆系统**：保留向量记忆（可用内存实现，后续可持久化）
- **情绪识别**：从 AI 回复中提取情绪

### 3. 整合 UI

将 `dti-5175-main` 的 UI 组件整合进来：
- MainActivity 的导航结构
- Onboarding 流程
- ChatScreen（替换为真实 AI 对接）
- HistoryScreen, SettingsScreen

### 4. ChatScreen 改造

替换 Mock 实现为真实 AI 对话：

```kotlin
// 需要在 ChatScreen 中添加
class MainActivity : ComponentActivity() {
    // 初始化 AI 组件
    private val llmClient = AITest()
    private val memoryStore = InMemoryStore()
    private val embeddingClient = EmbeddingLlm()
    private val personChat = PersonChat(llmClient, memoryStore, embeddingClient)

    // 当前角色（可以从 UI 选择或默认）
    private val currentProfile = Profile(
        id = "char_001",
        name = "AI 助手",
        gender = "女",
        relationship = "用户的朋友",
        background = "你是一个友好、乐观的女孩",
        personality = "善良活泼",
        speakStyle = listOf("友好", "活泼"),
        doRules = listOf("保持朋友身份"),
        dontRules = listOf("不要承认是AI")
    )
}
```

在 ChatScreen 的发送消息逻辑中：
- 获取用户输入
- 调用 `personChat.sendMessage(currentProfile, history, userText)`
- 将返回的 replyText 显示在聊天界面

### 5. 依赖整合

在 `app/build.gradle.kts` 中整合所有依赖：

```kotlin
dependencies {
    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)

    // Google Sign-In
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // Image Loading
    implementation(libs.coil.compose)

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // AI - Gemini
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")
}
```

### 6. API Key 配置

在 `local.properties` 中确保有：
```properties
API_Key=你的Google_API_Key
```

### 7. google-services.json

从 `dti-5175-main` 复制 `google-services.json` 到项目根目录

---

## Build 要求

**必须确保一次性 build 成功 - 这是最高优先级**

### 必须验证的步骤

1. **每次修改后必须运行验证**
   ```bash
   ./gradlew assembleDebug
   ```
   只有当这条命令完全成功（BUILD SUCCESSFUL）才算完成

2. **禁止留下需要手动修复的问题**
   - 如果 build 失败，必须在当前任务中修复
   - 禁止交付 "需要手动运行 XXX 命令" 或 "需要手动修改 XXX" 的状态
   - 依赖冲突、版本不兼容等所有问题必须在交付前解决

3. **常见 Build 问题排查清单**
   - [ ] 依赖版本冲突（检查 libs.versions.toml）
   - [ ] 缺失的 google-services.json
   - [ ] API_Key 未配置在 local.properties
   - [ ] 包名不一致
   - [ ] minSdk / targetSdk 版本问题
   - [ ] Kotlin 版本不兼容
   - [ ] 缺失的资源文件（drawable, values 等）

### 验证标准

- `./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`
- 无任何 warning 导致 build 失败
- 生成的 APK 文件存在于 `app/build/outputs/apk/debug/`

---

## 交付物

1. 完整合并的 Android 项目（代码完整，无缺失文件）
2. **必须验证**: `./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`
3. App 可安装在设备上运行
4. Onboarding -> 登录 -> 聊天界面 完整流程可工作
5. 发送消息能获得 AI 回复

### 交付前自检清单

- [ ] 运行 `./gradlew assembleDebug` 成功
- [ ] APK 文件生成在 `app/build/outputs/apk/debug/`
- [ ] 所有核心 AI 组件（PersonChat, LlmClient, EmbeddingLlm）已集成
- [ ] ChatScreen 使用真实的 AI 对话而非 Mock
- [ ] Onboarding 流程完整
- [ ] 没有 "TODO" 或 "FIXME" 标记的未完成代码

---

## 注意事项

- 保留两个项目的最佳实践
- 不要删除任何核心功能
- UI 保持 dti-5175-main 的风格
- 确保 Firebase 和 Gemini API Key 正确配置
- 如果遇到依赖冲突，明确列出需要解决的冲突