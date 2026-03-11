```mermaid
flowchart TD
    A([🚀 Launch App]) --> B{First Launch?}

    B -->|Yes| OB1[Onboarding Page 1]
    B -->|No| C{Already Signed In?}

    OB1 -->|Next| OB2[Onboarding Page 2\nCloud Features]
    OB1 -->|Skip| C

    OB2 -->|Next / Skip\nSigned In| LANG
    OB2 -->|Next / Skip\nNot Signed In| LOGIN

    C -->|Yes| MAIN
    C -->|No| LOGIN

    LOGIN[🔐 Google Sign-In] -->|Success| LANG

    subgraph NEW_CHARACTER_WIZARD [🧙 New Character Setup Wizard]
        LANG[🌐 Language Selection]
        GENDER[⚧ Gender Selection]
        AVATAR[🖼️ Avatar Selection]
        BASIC[📝 Basic Info\nName & Relationship]
        DETAIL[📋 Detail Info\nPersonality & Background]
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

    GENDER -->|Skip\nHas Characters| MAIN
    GENDER -->|Skip\nNo Characters| CHAR_LIST
    AVATAR -->|Skip\nHas Characters| MAIN
    AVATAR -->|Skip\nNo Characters| CHAR_LIST
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
        CHAR_EDITOR[Character Editor\nCreate / Edit]

        CHAR_LIST -->|Create New| CHAR_EDITOR
        CHAR_LIST -->|Edit| CHAR_EDITOR
        CHAR_EDITOR -->|Save| CHAR_LIST
        CHAR_EDITOR -->|Cancel\nHas Chars| CHAR_LIST
        CHAR_EDITOR -->|Cancel\nNo Chars| OB2
    end

    CHAR_LIST -->|Select Character| MAIN
    CHAR_LIST -->|Back| MAIN
```

### Todo

- [ ] Work on Models
  - [ ] AI, TTS, Video

### In Progress

- [ ] Work on the buttons

### Done ✓

- [x] Google Sign-in
