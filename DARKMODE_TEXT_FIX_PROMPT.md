# Bug Fix 任务：深色模式下的文字颜色修复

## 任务目标

1. 修复 Settings 页面在深色模式下文字看不见的问题
2. 在 Onboarding 之后引导用户创建第一个 AI 角色

---

## 问题 1：深色模式文字颜色问题

### 当前代码问题

在 `SettingsScreen.kt` 中：
- 使用了 `DarkText = Color(0xFF2D2D2D)` 作为文字颜色
- 在深色背景 (`Color(0xFF1E1E1E)`) 下，深色文字看不见
- `Color.Gray` 在深色背景下也不够明显

### 修复方案

在 `SettingsScreen.kt` 中添加深色模式下的文字颜色变量：

```kotlin
// Theme-aware colors (添加这些)
val textColor = if (isDarkMode) Color(0xFFE0E0E0) else DarkText
val subtitleColor = if (isDarkMode) Color(0xFFAAAAAA) else Color.Gray
val dividerColor = if (isDarkMode) Color(0xFF444444) else Color.LightGray.copy(alpha = 0.3f)
```

然后替换所有硬编码的颜色：

```kotlin
// 替换 DarkText -> textColor
// 替换 Color.Gray -> subtitleColor
// 替换 Color.LightGray.copy(alpha = 0.3f) -> dividerColor
```

需要修改的位置：
- 第 76 行: `color = DarkText` -> `color = textColor`
- 第 81 行: `containerColor = Color.Transparent`
- 第 135 行: `color = DarkText`
- 第 140 行: `color = Color.Gray`
- 第 151 行: `color = Color.Gray`
- 第 172 行: `color = Color.LightGray.copy(alpha = 0.3f)`
- 第 190 行: `color = Color.Gray`
- 第 209 行: `color = Color.LightGray.copy(alpha = 0.3f)`
- 第 219 行: `color = Color.LightGray.copy(alpha = 0.3f)`
- 第 242 行: `color = Color.Gray`
- 第 261 行: `color = Color.LightGray.copy(alpha = 0.3f)`
- 第 333 行: `color = DarkText`
- 第 335 行: `color = Color.Gray`
- 第 381 行: `color = DarkText`
- 第 383 行: `color = Color.Gray`
- 第 389 行: `tint = Color.LightGray`

---

## 问题 2：Onboarding 后引导创建角色

### 当前状态

用户完成 Onboarding 后直接进入主界面，没有引导创建角色。

### 修复方案

修改 `MainActivity.kt`：

1. 添加新页面常量（如需要）
2. 在 Onboarding 完成后，检查是否已有角色
3. 如果没有角色，显示角色创建引导

或者更简单的方案：
- 首次完成 Onboarding 后，直接跳转到 `CharacterListScreen` 让用户创建第一个角色
- 已创建过角色的用户直接进入主界面

```kotlin
// 在 MainActivity 的初始化逻辑中
val hasCharacters = characters.isNotEmpty()

// Onboarding 完成后
if (!hasCharacters) {
    // 引导创建第一个角色
    currentPage = PAGE_CHARACTER_LIST
} else {
    // 直接进入主界面
    currentPage = PAGE_MAIN
}
```

---

## 交付要求

1. **必须验证**: `./gradlew assembleDebug` 输出 `BUILD SUCCESSFUL`
2. 深色模式下 Settings 页面的文字清晰可见
3. 首次完成 Onboarding 后引导用户创建角色
4. 已创建过角色的用户直接进入主界面

---

## 注意事项

- 确保深色模式的所有文字颜色使用新变量
- 保持 UI 风格与现有界面一致
- 不要删除或修改 AITest.kt