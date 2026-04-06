# 时序图 / Sequence Diagrams

本文档描述了 DTI5175 AI 伴侣 Android 应用中各主要功能的完整时序关系。

> 图表使用 [Mermaid](https://mermaid.js.org/) 语法，可在 GitHub、VS Code 等平台直接渲染。

---

## 目录

1. [应用启动 & 用户认证](#1-应用启动--用户认证)
2. [文字聊天流程](#2-文字聊天流程)
3. [语音聊天流程（微信录音风格）](#3-语音聊天流程微信录音风格)
4. [实时语音流程（推送对讲）](#4-实时语音流程推送对讲)
5. [角色创建 & 声音克隆流程](#5-角色创建--声音克隆流程)
6. [Slot 购买流程（Stripe 支付）](#6-slot-购买流程stripe-支付)

---

## 1. 应用启动 & 用户认证

```mermaid
sequenceDiagram
    participant U as User (用户)
    participant App as MyApp<br/>(Application)
    participant MA as MainActivity
    participant OB as OnBoardingScreen
    participant LS as LoginScreen
    participant GAM as GoogleAuthManager
    participant FA as Firebase Auth
    participant FS as Firestore

    U->>App: 启动应用
    App->>App: onCreate()<br/>初始化 PersonChat 单例<br/>(EmbeddingLlm + InMemoryStore<br/>+ MiniMaxLlmClient)
    App->>MA: 启动 MainActivity

    MA->>MA: Compose 导航初始化
    MA->>OB: 检查是否已完成引导<br/>(OnboardingManager)

    alt 首次启动
        OB->>U: 显示引导页面
        U->>OB: 完成引导
        OB->>MA: 导航至 LoginScreen
    end

    MA->>LS: 显示登录界面
    U->>LS: 点击 "Google 登录"
    LS->>GAM: signIn(activityContext)
    GAM->>GAM: 构建 GetSignInWithGoogleOption<br/>(Credentials API)
    GAM->>FA: 请求 Google ID Token
    FA-->>GAM: 返回 Google ID Token
    GAM->>FA: 用 ID Token 换取 FirebaseCredential
    FA-->>GAM: 返回 FirebaseUser (uid)
    GAM-->>LS: Result.Success(FirebaseUser)

    LS->>FS: 检查/创建 users/{uid} 文档
    FS-->>LS: 用户数据 (slotLimit 等)

    LS->>MA: 登录成功，导航至 CharacterListScreen
```

---

## 2. 文字聊天流程

```mermaid
sequenceDiagram
    participant U as User (用户)
    participant CS as ChatScreen (UI)
    participant CHM as ChatHistoryManager
    participant PC as PersonChat
    participant EL as EmbeddingLlm<br/>(Gemini API)
    participant IMS as InMemoryStore<br/>(向量内存)
    participant PB as Prompt
    participant LLM as MiniMaxLlmClient<br/>(MiniMax API)

    U->>CS: 输入文字，点击发送
    CS->>CS: 更新 UI（显示用户消息）
    CS->>CHM: saveMessages(characterId, messages)
    CHM->>CHM: 持久化到 SharedPreferences<br/>(最多保存100条)

    CS->>PC: sendMessage(profile, history, userText)

    Note over PC: 步骤1: 向量化用户输入
    PC->>EL: embed(userText)
    EL->>EL: POST /v1/models/text-embedding-004<br/>(Google Gemini API)
    EL-->>PC: FloatArray[768]（768维向量）

    Note over PC: 步骤2: 检索相关记忆
    PC->>IMS: search(profileId, queryVector, topK=5)
    IMS->>IMS: 对所有记忆计算余弦相似度<br/>取相似度最高的5条
    IMS-->>PC: List<MemoryItem>（相关记忆）

    Note over PC: 步骤3: 构建系统提示词
    PC->>PB: buildPrompt(profile, memorySummary)
    PB-->>PC: 系统提示词（含角色性格、规则、记忆）
    PC->>PB: buildDialog(history, userText)
    PB-->>PC: 对话历史列表（最近20条）

    Note over PC: 步骤4: 调用大语言模型
    PC->>LLM: reply(systemPrompt, dialogHistory)
    LLM->>LLM: POST /v1/text/chatcompletion_v2<br/>model=MiniMax-M2.1<br/>temperature=0.7
    LLM-->>PC: 原始响应文本（含[MEM:...]标签）

    Note over PC: 步骤5: 提取并保存新记忆
    PC->>PC: 解析 [MEM: fact] 标签
    loop 每个新记忆
        PC->>EL: embed(memoryFact)
        EL-->>PC: FloatArray[768]
        PC->>IMS: add(profileId, fact, embedding)
    end

    Note over PC: 步骤6: 提取情绪 & 清理响应
    PC->>LLM: extractEmotion(rawText)
    LLM-->>PC: emotion（happy/sad/angry 等）
    PC->>PC: 移除标签，获得干净回复

    PC-->>CS: Result(replyText, emotion)
    CS->>CS: 更新 UI（显示 AI 回复）
    CS->>CHM: saveMessages（保存 AI 消息）
```

---

## 3. 语音聊天流程（微信录音风格）

```mermaid
sequenceDiagram
    participant U as User (用户)
    participant CS as ChatScreen (UI)
    participant ARM as AudioRecordManager
    participant ASR as DeepgramAsrClient<br/>(Deepgram API)
    participant VC as VoiceChat
    participant PC as PersonChat
    participant MAC as MiniMaxAudioClient<br/>(MiniMax TTS API)
    participant MP as MediaPlayer

    U->>CS: 长按麦克风按钮
    CS->>ARM: startRecording()
    ARM->>ARM: 打开 AudioRecord (16kHz PCM)<br/>写入 PCM 数据至缓冲区

    U->>CS: 松开麦克风按钮
    CS->>ARM: stopRecording()
    ARM->>ARM: 将 PCM 编码为 WAV 格式<br/>保存至 filesDir/voice_messages/
    ARM-->>CS: wavFilePath

    CS->>ASR: transcribe(wavFile)
    ASR->>ASR: POST https://api.deepgram.com/v1/listen<br/>model=nova-3（发送 WAV 字节）
    ASR-->>CS: userText（转录文本）

    CS->>CS: 显示用户消息（转录文本）

    CS->>VC: sendVoiceMsg(profile, history, userText)

    VC->>PC: sendMessage(profile, history, userText)
    Note over PC: [同文字聊天流程的步骤1-6]
    PC-->>VC: Result(replyText, emotion)

    Note over VC: 解析 TTS 声音 ID
    VC->>VC: VoiceAssignmentPreferences[characterId]<br/>→ Profile.voiceId<br/>→ 默认 "English_Graceful_Lady"

    VC->>MAC: textToVoice(replyText, voiceId, emotion)
    MAC->>MAC: POST /v1/t2a_v2<br/>model=speech-2.6-turbo<br/>voice_id + emotion + language_boost
    MAC-->>MAC: 响应体（Hex 编码 MP3）
    MAC->>MAC: Hex → ByteArray → 写入 .mp3 文件
    MAC-->>VC: mp3FilePath

    VC-->>CS: VoiceResult(userText, aiText, mp3Path, emotion)

    CS->>MP: play(mp3FilePath)
    MP-->>U: 播放 AI 语音回复
    CS->>CS: 显示 AI 文字回复
```

---

## 4. 实时语音流程（推送对讲）

```mermaid
sequenceDiagram
    participant U as User (用户)
    participant LVS as LiveVoiceScreen (UI)
    participant LVC as LiveVoiceController
    participant SR as Android SpeechRecognizer
    participant PC as PersonChat
    participant MAC as MiniMaxAudioClient
    participant MP as MediaPlayer

    U->>LVS: 进入 LiveVoiceScreen
    LVS->>LVC: 初始化控制器

    U->>LVS: 按下麦克风按钮
    LVS->>LVC: startListeningWithCallbacks(...)
    LVC->>SR: startListening(Intent)
    SR->>SR: 开始音频捕获

    loop 实时语音识别
        SR->>LVC: onRmsChanged(rms)
        LVC-->>LVS: 更新波形可视化
        SR->>LVC: onPartialResults(partialText)
        LVC-->>LVS: 显示实时转录文本
    end

    U->>LVS: 松开麦克风按钮
    LVS->>LVC: stopListening()
    LVC->>SR: stopListening()
    SR->>LVC: onResults(finalText)
    LVC-->>LVS: 最终转录文本

    LVS->>PC: sendMessage(profile, history, finalText)
    Note over PC: [同文字聊天流程的步骤1-6]
    PC-->>LVS: Result(replyText, emotion)

    LVS->>MAC: textToVoice(replyText, voiceId, emotion)
    MAC-->>LVS: mp3FilePath

    LVS->>MP: play(mp3FilePath)
    MP-->>U: 播放 AI 语音回复
    LVS->>LVS: 显示对话记录
```

---

## 5. 角色创建 & 声音克隆流程

```mermaid
sequenceDiagram
    participant U as User (用户)
    participant CCS as CreateCharacterScreen
    participant CS as CharacterStorage
    participant VCS as VoiceCloneScreen
    participant ARM as AudioRecordManager
    participant MMVC as MiniMaxVoiceCloneManager<br/>(MiniMax API)
    participant VAP as VoiceAssignmentPreferences
    participant UVM as UserVoiceManager
    participant FS as Firestore

    U->>CCS: 打开创建角色界面
    U->>CCS: 填写角色信息<br/>（名字、性别、背景、性格等）
    U->>CCS: 设置 Do/Don't 规则

    opt 克隆声音
        U->>CCS: 点击 "克隆声音"
        CCS->>VCS: 跳转至 VoiceCloneScreen

        loop 录制样本（至少1条）
            U->>VCS: 按住按钮，朗读示例文本
            VCS->>ARM: startRecording()
            U->>VCS: 松开按钮
            VCS->>ARM: stopRecording()
            ARM-->>VCS: wavFilePath
            VCS->>VCS: 显示已录制样本列表
        end

        U->>VCS: 点击 "提交克隆"
        VCS->>MMVC: cloneVoice(sampleWavFiles, voiceName)
        MMVC->>MMVC: POST /v1/voice_clone/submit<br/>（上传 WAV 样本）
        MMVC-->>VCS: clonedVoiceId

        VCS->>UVM: saveVoice(uid, voiceName, clonedVoiceId)
        UVM->>FS: 写入 users/{uid}/voices/{voiceId}
        FS-->>UVM: 保存成功

        VCS-->>CCS: clonedVoiceId
    end

    U->>CCS: 点击 "保存角色"
    CCS->>CS: saveProfile(profile)
    CS->>CS: 序列化为 JSON，存入 SharedPreferences

    opt 已克隆声音
        CCS->>VAP: setVoiceId(characterId, clonedVoiceId)
        VAP->>VAP: 持久化至 SharedPreferences (per-uid)
    end

    CCS-->>U: 角色创建成功，返回角色列表
```

---

## 6. Slot 购买流程（Stripe 支付）

```mermaid
sequenceDiagram
    participant U as User (用户)
    participant SPS as SlotPurchaseScreen
    participant CF as Firebase Cloud Function<br/>(createPaymentIntent)
    participant SA as Stripe API
    participant FS as Firestore
    participant SPE as Stripe Payment UI<br/>(PaymentSheet)
    participant WH as Firebase Cloud Function<br/>(stripeWebhook)

    U->>SPS: 进入购买界面，选择套餐
    SPS->>CF: 调用 createPaymentIntent({ priceId })
    Note over CF: 验证用户已登录 (auth.uid)

    CF->>SA: stripe.prices.retrieve(priceId)
    SA-->>CF: Price (unit_amount, currency)

    CF->>SA: stripe.products.retrieve(product)
    SA-->>CF: Product (metadata.slot_increment)

    CF->>FS: 读取 users/{uid}.slotLimit
    FS-->>CF: currentLimit

    alt currentLimit + slotIncrement > 20
        CF-->>SPS: HttpsError("failed-precondition")<br/>"超出最大 Slot 上限"
        SPS-->>U: 显示错误提示
    else 未超限
        CF->>SA: paymentIntents.create({<br/>  amount, currency,<br/>  metadata: {uid, slotIncrement, priceId}<br/>})
        SA-->>CF: PaymentIntent.client_secret

        CF-->>SPS: { clientSecret }

        SPS->>SPE: 初始化 PaymentSheet(clientSecret)
        SPE->>U: 显示支付界面（信用卡/其他）
        U->>SPE: 输入支付信息，确认支付

        SPE->>SA: 处理支付
        SA-->>SPE: 支付成功
        SPE-->>SPS: PaymentSheet.Result.Completed

        SA->>WH: POST /stripeWebhook<br/>event=payment_intent.succeeded
        Note over WH: 验证 Stripe 签名

        WH->>FS: 检查 processed_payments/{paymentIntentId}
        FS-->>WH: 不存在（未处理过）

        WH->>FS: 原子事务：<br/>1. users/{uid}.slotLimit += slotIncrement<br/>2. 写入 processed_payments/{id}
        FS-->>WH: 事务成功

        WH-->>SA: HTTP 200 { received: true }

        SPS->>FS: 监听 users/{uid}.slotLimit 变化
        FS-->>SPS: 新的 slotLimit
        SPS-->>U: 显示 "购买成功，Slot 已增加！"
    end
```

---

## 整体组件交互概览

```mermaid
sequenceDiagram
    participant UI as UI Layer<br/>(Screens)
    participant BL as Business Logic<br/>(PersonChat / VoiceChat)
    participant MEM as Memory Layer<br/>(InMemoryStore)
    participant LLM as LLM<br/>(MiniMax M2.1)
    participant EMB as Embedding<br/>(Gemini API)
    participant TTS as TTS/STT<br/>(MiniMax Audio)
    participant AUTH as Auth<br/>(Firebase Auth)
    participant DB as Database<br/>(Firestore)
    participant PAY as Payment<br/>(Stripe)

    UI->>AUTH: 登录/登出
    AUTH-->>UI: FirebaseUser

    UI->>BL: 发送消息 / 语音输入
    BL->>EMB: 文本向量化
    EMB-->>BL: 768维向量
    BL->>MEM: 检索相关记忆
    MEM-->>BL: 相关记忆列表
    BL->>LLM: 发送提示词 + 对话历史
    LLM-->>BL: AI 回复文本
    BL->>MEM: 保存新记忆
    BL-->>UI: Result(replyText, emotion)

    UI->>TTS: 文本转语音
    TTS-->>UI: MP3 文件路径
    UI->>TTS: 语音转文本（STT）
    TTS-->>UI: 转录文本

    UI->>DB: 读写角色/历史/配置
    DB-->>UI: 数据

    UI->>PAY: 发起支付（Cloud Function）
    PAY-->>DB: 更新 slotLimit（Webhook）
    DB-->>UI: 实时更新
```
