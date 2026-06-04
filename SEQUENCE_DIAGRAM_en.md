# Sequence Diagrams

This document describes the complete timing and flow of all major features in the DTI5175 AI Companion Android application.

> Diagrams use [Mermaid](https://mermaid.js.org/) syntax and can be directly rendered on GitHub, VS Code, and other platforms.

---

## Table of Contents

1. [App Launch & User Authentication](#1-app-launch--user-authentication)
2. [Text Chat Flow](#2-text-chat-flow)
3. [Voice Chat Flow (WeChat Recording Style)](#3-voice-chat-flow-wechat-recording-style)
4. [Live Voice Flow (Push-to-Talk)](#4-live-voice-flow-push-to-talk)
5. [Character Creation & Voice Cloning Flow](#5-character-creation--voice-cloning-flow)
6. [Slot Purchase Flow (Stripe Payment)](#6-slot-purchase-flow-stripe-payment)

---

## 1. App Launch & User Authentication

```mermaid
sequenceDiagram
    participant U as User
    participant App as MyApp<br/>(Application)
    participant MA as MainActivity
    participant OB as OnBoardingScreen
    participant LS as LoginScreen
    participant GAM as GoogleAuthManager
    participant FA as Firebase Auth
    participant FS as Firestore

    U->>App: Launch app
    App->>App: onCreate()<br/>Initialize PersonChat singleton<br/>(EmbeddingLlm + InMemoryStore<br/>+ MiniMaxLlmClient)
    App->>MA: Start MainActivity

    MA->>MA: Compose navigation init
    MA->>OB: Check if onboarding completed<br/>(OnboardingManager)

    alt First Launch
        OB->>U: Show onboarding screens
        U->>OB: Complete onboarding
        OB->>MA: Navigate to LoginScreen
    end

    MA->>LS: Show login screen
    U->>LS: Tap "Google Sign-In"
    LS->>GAM: signIn(activityContext)
    GAM->>GAM: Build GetSignInWithGoogleOption<br/>(Credentials API)
    GAM->>FA: Request Google ID Token
    FA-->>GAM: Return Google ID Token
    GAM->>FA: Exchange ID Token for FirebaseCredential
    FA-->>GAM: Return FirebaseUser (uid)
    GAM-->>LS: Result.Success(FirebaseUser)

    LS->>FS: Check/create users/{uid} document
    FS-->>LS: User data (slotLimit, etc.)

    LS->>MA: Login success, navigate to CharacterListScreen
```

---

## 2. Text Chat Flow

```mermaid
sequenceDiagram
    participant U as User
    participant CS as ChatScreen (UI)
    participant CHM as ChatHistoryManager
    participant PC as PersonChat
    participant EL as EmbeddingLlm<br/>(Gemini API)
    participant IMS as InMemoryStore<br/>(Vector Memory)
    participant PB as Prompt
    participant LLM as MiniMaxLlmClient<br/>(MiniMax API)

    U->>CS: Enter text, tap send
    CS->>CS: Update UI (show user message)
    CS->>CHM: saveMessages(characterId, messages)
    CHM->>CHM: Persist to SharedPreferences<br/>(max 100 messages)

    CS->>PC: sendMessage(profile, history, userText)

    Note over PC: Step 1: Vectorize user input
    PC->>EL: embed(userText)
    EL->>EL: POST /v1/models/text-embedding-004<br/>(Google Gemini API)
    EL-->>PC: FloatArray-768 (768-dim vector)

    Note over PC: Step 2: Retrieve relevant memories
    PC->>IMS: search(profileId, queryVector, topK=5)
    IMS->>IMS: Compute cosine similarity<br/>for all memories<br/>Select top 5 most similar
    IMS-->>PC: List<MemoryItem> (relevant memories)

    Note over PC: Step 3: Build system prompt
    PC->>PB: buildPrompt(profile, memorySummary)
    PB-->>PC: System prompt (character personality, rules, memories)
    PC->>PB: buildDialog(history, userText)
    PB-->>PC: Dialog history list (last 20 messages)

    Note over PC: Step 4: Call LLM
    PC->>LLM: reply(systemPrompt, dialogHistory)
    LLM->>LLM: POST /v1/text/chatcompletion_v2<br/>model=MiniMax-M2.1<br/>temperature=0.7
    LLM-->>PC: Raw response text (contains MEM tags)

    Note over PC: Step 5: Extract and save new memories
    PC->>PC: Parse MEM tags
    loop For each new memory
        PC->>EL: embed(memoryFact)
        EL-->>PC: FloatArray-768
        PC->>IMS: add(profileId, fact, embedding)
    end

    Note over PC: Step 6: Extract emotion & clean response
    PC->>LLM: extractEmotion(rawText)
    LLM-->>PC: emotion (happy/sad/angry, etc.)
    PC->>PC: Remove tags, get clean reply

    PC-->>CS: Result(replyText, emotion)
    CS->>CS: Update UI (show AI reply)
    CS->>CHM: saveMessages (save AI message)
```

---

## 3. Voice Chat Flow (WeChat Recording Style)

```mermaid
sequenceDiagram
    participant U as User
    participant CS as ChatScreen (UI)
    participant ARM as AudioRecordManager
    participant ASR as DeepgramAsrClient<br/>(Deepgram API)
    participant VC as VoiceChat
    participant PC as PersonChat
    participant MAC as MiniMaxAudioClient<br/>(MiniMax TTS API)
    participant MP as MediaPlayer

    U->>CS: Long press mic button
    CS->>ARM: startRecording()
    ARM->>ARM: Open AudioRecord (16kHz PCM)<br/>Write PCM data to buffer

    U->>CS: Release mic button
    CS->>ARM: stopRecording()
    ARM->>ARM: Encode PCM to WAV<br/>Save to filesDir/voice_messages/
    ARM-->>CS: wavFilePath

    CS->>ASR: transcribe(wavFile)
    ASR->>ASR: POST https://api.deepgram.com/v1/listen<br/>model=nova-3 (send WAV bytes)
    ASR-->>CS: userText (transcribed text)

    CS->>CS: Show user message (transcribed text)

    CS->>VC: sendVoiceMsg(profile, history, userText)

    VC->>PC: sendMessage(profile, history, userText)
    Note over PC: [Same as text chat flow steps 1-6]
    PC-->>VC: Result(replyText, emotion)

    Note over VC: Resolve TTS voice ID
    VC->>VC: VoiceAssignmentPreferences[characterId]<br/>→ Profile.voiceId<br/>→ Default "English_Graceful_Lady"

    VC->>MAC: textToVoice(replyText, voiceId, emotion)
    MAC->>MAC: POST /v1/t2a_v2<br/>model=speech-2.6-turbo<br/>voice_id + emotion + language_boost
    MAC-->>MAC: Response body (Hex-encoded MP3)
    MAC->>MAC: Hex → ByteArray → Write to .mp3 file
    MAC-->>VC: mp3FilePath

    VC-->>CS: VoiceResult(userText, aiText, mp3Path, emotion)

    CS->>MP: play(mp3FilePath)
    MP-->>U: Play AI voice reply
    CS->>CS: Show AI text reply
```

---

## 4. Live Voice Flow (Push-to-Talk)

```mermaid
sequenceDiagram
    participant U as User
    participant LVS as LiveVoiceScreen (UI)
    participant LVC as LiveVoiceController
    participant SR as Android SpeechRecognizer
    participant PC as PersonChat
    participant MAC as MiniMaxAudioClient
    participant MP as MediaPlayer

    U->>LVS: Enter LiveVoiceScreen
    LVS->>LVC: Initialize controller

    U->>LVS: Press mic button
    LVS->>LVC: startListeningWithCallbacks(...)
    LVC->>SR: startListening(Intent)
    SR->>SR: Start audio capture

    loop Real-time speech recognition
        SR->>LVC: onRmsChanged(rms)
        LVC-->>LVS: Update waveform visualization
        SR->>LVC: onPartialResults(partialText)
        LVC-->>LVS: Show real-time transcribed text
    end

    U->>LVS: Release mic button
    LVS->>LVC: stopListening()
    LVC->>SR: stopListening()
    SR->>LVC: onResults(finalText)
    LVC-->>LVS: Final transcribed text

    LVS->>PC: sendMessage(profile, history, finalText)
    Note over PC: [Same as text chat flow steps 1-6]
    PC-->>LVS: Result(replyText, emotion)

    LVS->>MAC: textToVoice(replyText, voiceId, emotion)
    MAC-->>LVS: mp3FilePath

    LVS->>MP: play(mp3FilePath)
    MP-->>U: Play AI voice reply
    LVS->>LVS: Show conversation history
```

---

## 5. Character Creation & Voice Cloning Flow

```mermaid
sequenceDiagram
    participant U as User
    participant CCS as CreateCharacterScreen
    participant CS as CharacterStorage
    participant VCS as VoiceCloneScreen
    participant ARM as AudioRecordManager
    participant MMVC as MiniMaxVoiceCloneManager<br/>(MiniMax API)
    participant VAP as VoiceAssignmentPreferences
    participant UVM as UserVoiceManager
    participant FS as Firestore

    U->>CCS: Open character creation screen
    U->>CCS: Fill in character info<br/>(name, gender, background, personality, etc.)
    U->>CCS: Set Do/Don't rules

    opt Clone voice
        U->>CCS: Tap "Clone Voice"
        CCS->>VCS: Navigate to VoiceCloneScreen

        loop Record samples (at least 1)
            U->>VCS: Hold button, read sample text
            VCS->>ARM: startRecording()
            U->>VCS: Release button
            VCS->>ARM: stopRecording()
            ARM-->>VCS: wavFilePath
            VCS->>VCS: Show recorded samples list
        end

        U->>VCS: Tap "Submit Clone"
        VCS->>MMVC: cloneVoice(sampleWavFiles, voiceName)
        MMVC->>MMVC: POST /v1/voice_clone/submit<br/>(upload WAV samples)
        MMVC-->>VCS: clonedVoiceId

        VCS->>UVM: saveVoice(uid, voiceName, clonedVoiceId)
        UVM->>FS: Write to users/{uid}/voices/{voiceId}
        FS-->>UVM: Save success

        VCS-->>CCS: clonedVoiceId
    end

    U->>CCS: Tap "Save Character"
    CCS->>CS: saveProfile(profile)
    CS->>CS: Serialize to JSON, save to SharedPreferences

    opt Has cloned voice
        CCS->>VAP: setVoiceId(characterId, clonedVoiceId)
        VAP->>VAP: Persist to SharedPreferences (per-uid)
    end

    CCS-->>U: Character created successfully, return to character list
```

---

## 6. Slot Purchase Flow (Stripe Payment)

```mermaid
sequenceDiagram
    participant U as User
    participant SPS as SlotPurchaseScreen
    participant CF as Firebase Cloud Function<br/>(createPaymentIntent)
    participant SA as Stripe API
    participant FS as Firestore
    participant SPE as Stripe Payment UI<br/>(PaymentSheet)
    participant WH as Firebase Cloud Function<br/>(stripeWebhook)

    U->>SPS: Enter purchase screen, select plan
    SPS->>CF: Call createPaymentIntent({ priceId })
    Note over CF: Verify user is logged in (auth.uid)

    CF->>SA: stripe.prices.retrieve(priceId)
    SA-->>CF: Price (unit_amount, currency)

    CF->>SA: stripe.products.retrieve(product)
    SA-->>CF: Product (metadata.slot_increment)

    CF->>FS: Read users/{uid}.slotLimit
    FS-->>CF: currentLimit

    alt currentLimit + slotIncrement > 20
        CF-->>SPS: HttpsError("failed-precondition")<br/>"Exceeds maximum slot limit"
        SPS-->>U: Show error message
    else Within limit
        CF->>SA: paymentIntents.create({<br/>  amount, currency,<br/>  metadata: {uid, slotIncrement, priceId}<br/>})
        SA-->>CF: PaymentIntent.client_secret

        CF-->>SPS: { clientSecret }

        SPS->>SPE: Initialize PaymentSheet(clientSecret)
        SPE->>U: Show payment UI (credit card/etc.)
        U->>SPE: Enter payment info, confirm

        SPE->>SA: Process payment
        SA-->>SPE: Payment success
        SPE-->>SPS: PaymentSheet.Result.Completed

        SA->>WH: POST /stripeWebhook<br/>event=payment_intent.succeeded
        Note over WH: Verify Stripe signature

        WH->>FS: Check processed_payments/{paymentIntentId}
        FS-->>WH: Does not exist (not processed yet)

        WH->>FS: Atomic transaction:<br/>1. users/{uid}.slotLimit += slotIncrement<br/>2. Write to processed_payments/{id}
        FS-->>WH: Transaction success

        WH-->>SA: HTTP 200 { received: true }

        SPS->>FS: Listen to users/{uid}.slotLimit changes
        FS-->>SPS: New slotLimit
        SPS-->>U: Show "Purchase successful, slots increased!"
    end
```

---

## Overall Component Interaction Overview

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

    UI->>AUTH: Login/logout
    AUTH-->>UI: FirebaseUser

    UI->>BL: Send message / voice input
    BL->>EMB: Vectorize text
    EMB-->>BL: 768-dim vector
    BL->>MEM: Retrieve relevant memories
    MEM-->>BL: Relevant memories list
    BL->>LLM: Send prompt + dialog history
    LLM-->>BL: AI response text
    BL->>MEM: Save new memories
    BL-->>UI: Result(replyText, emotion)

    UI->>TTS: Text to speech
    TTS-->>UI: MP3 file path
    UI->>TTS: Speech to text (STT)
    TTS-->>UI: Transcribed text

    UI->>DB: Read/write characters/history/config
    DB-->>UI: Data

    UI->>PAY: Initiate payment (Cloud Function)
    PAY-->>DB: Update slotLimit (Webhook)
    DB-->>UI: Real-time update
```
