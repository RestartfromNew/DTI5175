# meetsee U Technical Sequence Diagrams

This document provides a detailed breakdown of the underlying technical interactions within `meetsee U`. It visualizes the synergy between ASR, LLM, TTS, and video rendering engines.

---

### 1. Synchronized Voice & Video Interaction Flow
Describes the end-to-end path from microphone input to AI reasoning and simultaneous audio-visual playback.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as Android UI (LiveVoiceScreen)
    participant ASR as AssemblyAI / Deepgram
    participant AI as Gemini API
    participant TTS as MiniMax TTS Engine
    participant Video as Video Manager (Avatar API)
    participant Storage as Local Cache (wav)

    User->>App: Hold mic or toggle voice chat
    App->>ASR: Stream PCM audio data
    ASR-->>App: Return Transcript (text)
    App->>AI: Send Transcript + Character Context
    AI-->>App: Return AI Reply Text + Emotion
    
    par Parallel Processing
        App->>TTS: Request TTS (MiniMax)
        TTS->>Storage: Save rendered .wav file locally
        TTS-->>App: Signal audio ready
    and If Video Mode is Enabled
        App->>Video: Request Video Generation (Avatar Rendering)
        Video-->>App: Return Remote Video URL
    end

    App->>App: Sync & Latency Compensation
    App->>User: Play Audio + Render Video + Show Visual Feedback
```

---

### 2. Voice Cloning & Profile Management Flow
Details the secure voice sampling process and the critical local cache invalidation strategy.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant CloneUI as VoiceCloneScreen
    participant MM as MiniMax Clone API
    participant DB as Firestore (UserVoiceManager)
    participant Store as CharacterStorage
    participant Cache as ChatHistoryManager (Disk)

    User->>CloneUI: Record 10s+ sample audio
    User->>CloneUI: Input Reference Transcript
    CloneUI->>MM: Upload audio + transcript for cloning
    MM-->>CloneUI: Return new Voice ID

    alt Success
        CloneUI->>DB: Link Voice ID to Character in Firestore
        DB-->>CloneUI: Confirm successful save
        
        Note over CloneUI, Cache: Critical: Purging stale audio caches
        CloneUI->>Cache: Call clearVoiceFiles(characterId)
        Cache->>Cache: Physically delete all .wav files in child directory
        
        CloneUI->>Store: Update CharacterInfo & persist locally
        CloneUI-->>User: Visual success feedback
    else Failure
        CloneUI-->>User: Error message & Rollback logic (Delete from MiniMax if needed)
    end
```

---

### 3. On-Demand "Dynamic Refresh" Logic (Cache Invalidation)
Illustrates how the system transparently re-generates audio when a voice is updated and old files are missing.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Chat as ChatScreen
    participant Cache as ChatHistoryManager
    participant TTS as MiniMax API

    User->>Chat: Click "Play" on a history message
    Chat->>Cache: Verify if local .wav exists
    
    alt Cache Hit (File found)
        Cache-->>Chat: Return file path
        Chat->>User: Immediate local playback
    else Cache Miss (File deleted due to voice update)
        Chat->>Chat: Detect stale audio or cache purge
        Chat->>TTS: Re-request TTS with the LATEST Voice ID
        TTS-->>Chat: Return fresh audio buffer
        Chat->>Cache: Re-save file to local storage
        Chat->>User: Play updated audio
    end
```

---
> **Rendering Note**: 
> These diagrams are powered by Mermaid. 
> To view them, use a Markdown editor with Mermaid support (like VS Code or GitHub) in **Preview Mode**.


# meetsee U 技术交互时序图 (Technical Sequence Diagrams)

本文档详细描述了 `meetsee U` 核心功能的底层技术交互流程，旨在帮助开发者理解 ASR、LLM、TTS 以及视频渲染引擎之间的协作关系。

---

### 1. 实时语音视频对话流程 (Live Voice & Video Flow)
描述用户开启麦克风讲话后，系统如何从声音识别一路走到视频联动播放。

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as Android UI (LiveVoiceScreen)
    participant ASR as AssemblyAI / Deepgram
    participant AI as Gemini API
    participant TTS as MiniMax TTS Engine
    participant Video as Video Manager (Avatar API)
    participant Storage as Local Cache (wav)

    User->>App: 按住/开启麦克风讲话
    App->>ASR: 实时流式音频上传
    ASR-->>App: 返回转录文本 (Transcript)
    App->>AI: 发送文本 + Character Context
    AI-->>App: 返回 AI 回复文本 (Reply Text)
    
    par 并行执行 (Parallel Processing)
        App->>TTS: 请求语音合成 (MiniMax)
        TTS->>Storage: 保存合成后的 .wav 文件
        TTS-->>App: 返回音频就绪信号
    and 仅当视频模式开启 (If Video Mode On)
        App->>Video: 请求视频生成 (Avatar Rendering)
        Video-->>App: 返回远程视频 URL
    end

    App->>App: 步调同步 (Sync Playback)
    App->>User: 播放语音 + 视频渲染 + 动效反馈
```

---

### 2. 声音克隆与管理流程 (Voice Cloning & Management)
描述用户录音克隆声音，并确保数据库记录与本地缓存同步清理的闭环。

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant CloneUI as VoiceCloneScreen
    participant MM as MiniMax Clone API
    participant DB as Firestore (UserVoiceManager)
    participant Store as CharacterStorage
    participant Cache as ChatHistoryManager (Disk)

    User->>CloneUI: 录制 10s+ 参考音频
    User->>CloneUI: 输入参考文本 (Reference Transcript)
    CloneUI->>MM: 上传音频 + 文本进行克隆
    MM-->>CloneUI: 返回 Voice ID

    alt 成功 (Success)
        CloneUI->>DB: 存储 Voice ID 与 Character 绑定关系
        DB-->>CloneUI: 确认存储成功
        
        Note over CloneUI, Cache: 核心逻辑：物理清理旧声音缓存
        CloneUI->>Cache: 调用 clearVoiceFiles(characterId)
        Cache->>Cache: 删除目录下所有旧的 .wav 缓存
        
        CloneUI->>Store: 更新 CharacterInfo 并本地持久化
        CloneUI-->>User: 提示克隆成功，返回聊天
    else 失败 (Failure)
        CloneUI-->>User: 报错，执行回滚逻辑 (若已克隆则删 MiniMax ID)
    end
```

---

### 3. 基于缓存失效的“动态刷新”逻辑 (Dynamic Refresh Strategy)
描述在声音 ID 更新后，系统如何通过“物理删除 -> 重新拉取”实现旧消息的无缝语音切换。

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Chat as ChatScreen
    participant Cache as ChatHistoryManager
    participant TTS as MiniMax API

    User->>Chat: 点击历史消息的 "播放" 按钮
    Chat->>Cache: 检查本地是否有对应的 wav 文件
    
    alt 文件存在 (Cache Hit)
        Cache-->>Chat: 返回路径，直接播放
    else 文件不存在 (Cache Miss / 刚被物理删除)
        Chat->>Chat: 检测到需要重新同步声音
        Chat->>TTS: 使用当前角色最新的 Voice ID 请求 TTS
        TTS-->>Chat: 返回新语音音频
        Chat->>Cache: 保存新音频到本地
        Chat->>User: 播放最新的声音
    end
```

---

> **阅读建议**：
> *   本图表使用 Mermaid 渲染。
> *   如有修改需求，请直接编辑本文件的 `.md` 源码。

