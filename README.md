```mermaid
flowchart TD
    A([🚀 Launch App]) --> B{First Launch?}

    B -->|Yes| OB1[Onboarding Page 1]
    B -->|No| C{Already Signed In?}

    OB1 -->|Next| OB2[Onboarding Page 2<br/>Cloud Features]
    OB1 -->|Skip| C

    OB2 -->|Next / Skip<br/>Signed In| LANG
    OB2 -->|Next / Skip<br/>Not Signed In| LOGIN

    C -->|Yes| MAIN
    C -->|No| LOGIN

    LOGIN[🔐 Google Sign-In] -->|Success| LANG

    subgraph NEW_CHARACTER_WIZARD [🧙 New Character Setup Wizard]
        LANG[🌐 Language Selection]
        GENDER[⚧ Gender Selection]
        AVATAR[🖼️ Avatar Selection]
        BASIC[📝 Basic Info<br/>Name & Relationship]
        DETAIL[📋 Detail Info<br/>Personality & Background]
        VOICE[🎤 Voice Clone]

        LANG -->|Select / Skip| GENDER
        GENDER -->|Select| AVATAR
        AVATAR -->|Select| BASIC
        AVATAR -->|Back| GENDER
        BASIC -->|Next| DETAIL
        BASIC -->|Back| AVATAR
        DETAIL -->|Save / Skip| VOICE
        DETAIL -->|Back| BASIC
        VOICE -->|Clone / Skip| MAIN
        VOICE -->|Back| DETAIL
    end

    GENDER -->|Skip<br/>Has Characters| MAIN
    GENDER -->|Skip<br/>No Characters| CHAR_LIST
    AVATAR -->|Skip<br/>Has Characters| MAIN
    AVATAR -->|Skip<br/>No Characters| CHAR_LIST
    BASIC -->|Skip| CHAR_LIST

    subgraph MAIN [🏠 Main App]
        direction LR
        CHAT[💬 Chat Tab]
        HISTORY[📜 History Tab]
        SETTINGS[⚙️ Settings Tab]
    end

    HISTORY -->|Tap Conversation| CHAT
    SETTINGS -->|Manage Characters| CHAR_LIST
    SETTINGS -->|Sign Out| SIGNOUT[🔓 Sign Out] --> LOGIN

    subgraph CHAR_MGMT [👥 Character Management]
        CHAR_LIST[Character List]
        CHAR_EDITOR[Character Editor<br/>Create / Edit]

        CHAR_LIST -->|Create New| CHAR_EDITOR
        CHAR_LIST -->|Edit| CHAR_EDITOR
        CHAR_EDITOR -->|Save| CHAR_LIST
        CHAR_EDITOR -->|Cancel<br/>Has Chars| CHAR_LIST
        CHAR_EDITOR -->|Cancel<br/>No Chars| OB2
    end

    CHAR_LIST -->|Select Character| MAIN
    CHAR_LIST -->|Back| MAIN

    LIVE_VOICE[🔮 Live Voice Mode<br/>Push-to-Talk]

    subgraph CHAT_FLOW [💬 Chat Flow]
        direction TB
        TEXT_CHAT[💬 Text Chat Mode]
        VOICE_CHAT[🎤 Voice Chat Mode<br/>WeChat-style]

        TEXT_CHAT --> TEXT_INPUT[📝 User types text]
        TEXT_INPUT --> STT_PASS[(User text used directly)]
        STT_PASS --> PERSONCHAT[🧠 PersonChat.sendMessage]

        VOICE_CHAT --> HOLD_MIC[🔘 Hold-to-Record]
        HOLD_MIC --> RECORD_AUDIO[🎙️ AudioRecord records PCM]
        RECORD_AUDIO --> STOP_RECORD[🔓 Release to stop]
        STOP_RECORD --> DEEPGRAM_STT[🌐 Deepgram ASR<br/>STT: WAV → text]
        DEEPGRAM_STT --> PERSONCHAT

        LIVE_VOICE --> PUSH_MIC[🔘 Push-to-Talk]
        PUSH_MIC --> LIVE_RECORD[🎙️ AudioRecord records PCM<br/>with RMS waveform]
        LIVE_RECORD --> LIVE_STT[🌐 Deepgram ASR<br/>Transcribe WAV]
        LIVE_STT --> PERSONCHAT

        PERSONCHAT --> EMBED_QUERY[🔢 EmbeddingLlm<br/>Text → FloatArray-768]
        EMBED_QUERY --> VECTOR_SEARCH[🔍 LocalVectorMemory<br/>Cosine Similarity Search<br/>topK=5 memories]
        VECTOR_SEARCH --> BUILD_PROMPT[📋 Prompt.buildPrompt<br/>Profile + Memory + History]
        BUILD_PROMPT --> LLM_REPLY[🤖 MiniMaxLlmClient<br/>MiniMax-M2.1 API]
        LLM_REPLY --> EXTRACT_MEM[💾 Extract MEM tags<br/>Save to vector memory]
        EXTRACT_MEM --> RETURN_RESULT[✅ Result<br/>replyText, emotion]

        RETURN_RESULT --> TTS_TEXT[🔈 TTS optional<br/>MiniMax TTS<br/>text → .mp3]
        TTS_TEXT --> VOICE_OUTPUT[🔊 Playback<br/>via MediaPlayer]
    end

    CHAT -->|Switch Mode| TEXT_CHAT
    CHAT -->|Switch Mode| VOICE_CHAT
    CHAR_LIST -->|Voice Call| LIVE_VOICE
```

### Architecture: AI Pipeline

```mermaid
flowchart LR
    subgraph INPUT
        USER_TEXT[User Text Input]
        USER_VOICE[User Voice<br/>WAV File]
    end

    subgraph STT_LAYER [🎤 Speech-to-Text]
        DEEPGRAM[DeepgramAsrClient]
        ANDROID_STT["(Deprecated)"]
    end

    subgraph CORE_BRAIN [🧠 PersonChat]
        EMBED[EmbeddingLlm<br/>text-embedding-004]
        MEMORY[LocalVectorMemory<br/>InMemoryStore]
        PROMPT[Prompt.buildPrompt]
        LLM[MiniMaxLlmClient<br/>MiniMax-M2.1]
    end

    subgraph TTS_LAYER [🔈 Text-to-Speech]
        MINIMAX_TTS[MiniMaxAudioClient<br/>MiniMax TTS API]
        VOICE_CLONE[VoiceClone<br/>Custom voice_id]
    end

    USER_TEXT --> PROMPT
    USER_VOICE --> DEEPGRAM
    DEEPGRAM --> PROMPT
    PROMPT --> LLM
    LLM --> RESPONSE[AI Response]
    EMBED --> MEMORY
    MEMORY --> PROMPT
    RESPONSE --> MINIMAX_TTS
    MINIMAX_TTS --> AUDIO_OUT[.mp3 Audio]

    style INPUT fill:#1a1a2e,color:#fff
    style STT_LAYER fill:#2d2d44,color:#fff
    style CORE_BRAIN fill:#16213e,color:#fff
    style TTS_LAYER fill:#0f3460,color:#fff
```

### Todo

- [ ] Work on Models
  - [ ] AI, TTS, Video

### In Progress

- [ ] Manage memory

### Done ✓

- [x] Google Sign-in
- [x] Clone Voice 
- [x] Delete Voice
- [ ] Data segregation for different accounts
