# 功能开发任务：用户自定义 AI 角色设定 UI

## 任务目标

创建一个用户可以自定义 AI 角色设定的 UI 界面，包括角色名称、性格、背景故事、说话风格等。让用户可以创建和管理多个 AI 角色。

---

## 需求分析

### 当前状态
- AI 角色设定硬编码在 `MainActivity.kt` 的 `currentProfile` 中
- `Profile` 数据类已定义在 `domain/Profile.kt`

### 目标
- 创建 UI 让用户可以创建、编辑、选择 AI 角色
- 角色设定保存到本地存储
- 可以创建多个角色并切换

---

## 实现方案

### 1. 创建角色编辑器 Screen

创建新文件 `app/src/main/java/com/example/chatpart/screens/CharacterEditorScreen.kt`：

```kotlin
@Composable
fun CharacterEditorScreen(
    isDarkMode: Boolean = false,
    onSave: (Profile) -> Unit,
    onCancel: () -> Unit
) {
    // 表单字段
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("女") }
    var relationship by remember { mutableStateOf("用户的朋友") }
    var background by remember { mutableStateOf("") }
    var personality by remember { mutableStateOf("") }
    var speakStyle by remember { mutableStateOf("") }
    var doRules by remember { mutableStateOf("") }
    var dontRules by remember { mutableStateOf("") }

    // 表单界面，包含：
    // - TextField for each field
    // - Save button
    // - Cancel button
}
```

### 2. 创建角色列表 Screen

创建新文件 `app/src/main/java/com/example/chatpart/screens/CharacterListScreen.kt`：

```kotlin
@Composable
fun CharacterListScreen(
    isDarkMode: Boolean = false,
    onSelectCharacter: (Profile) -> Unit,
    onCreateNew: () -> Unit,
    onEdit: (Profile) -> Unit,
    onDelete: (Profile) -> Unit
) {
    // 显示已创建的角色列表
    // 每个角色显示：名称、简介
    // 支持：选择、编辑、删除
    // 创建新角色按钮
}
```

### 3. 修改 MainActivity

添加导航到角色管理：
- 从 Settings 页面可以进入"角色管理"
- 或在 Chat 页面添加角色切换按钮

### 4. 本地存储

使用 SharedPreferences 或 DataStore 保存角色列表：

```kotlin
class CharacterStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("characters", Context.MODE_PRIVATE)

    fun saveCharacters(characters: List<Profile>) {
        // JSON 序列化保存
    }

    fun loadCharacters(): List<Profile> {
        // JSON 反序列化读取
    }

    fun saveSelectedCharacterId(id: String) {
        prefs.edit().putString("selected_character_id", id).apply()
    }

    fun getSelectedCharacterId(): String? {
        return prefs.getString("selected_character_id", null)
    }
}
```

---

## UI 设计要求

### 角色列表页面
- 显示已创建的角色卡片
- 每个卡片显示：角色名称、性别、简短描述
- 卡片点击选择角色
- 长按或编辑按钮进入编辑
- FloatingActionButton 创建新角色

### 角色编辑器页面
- 滚动表单，包含：
  - 名称（必填）
  - 性别（选择器：男/女/其他）
  - 与用户关系（文本输入）
  - 背景故事（多行文本输入）
  - 性格描述（多行文本输入）
  - 说话风格（标签输入或列表）
  - 行为规则（多行文本）
  - 禁止规则（多行文本）
- 保存按钮
- 取消按钮

---

## 交付要求

1. **必须验证**: `./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`
2. 角色列表页面显示已创建的角色
3. 可以创建新角色
4. 可以编辑现有角色
5. 可以删除角色
6. 选择角色后，Chat 页面使用该角色的设定
7. 角色数据持久化保存

---

## 注意事项

- 复用现有的 `Profile` 数据类
- 复用现有的 `ChatHistoryManager` 类似的存储模式
- 保持深色模式适配
- 不要修改 `AITest.kt`（保留原有功能）
- UI 风格保持与现有界面一致