# Technical Proposal: Decoupling ReaderChaptersSheet from Preferences, Context Color Resolvers & Action Bloat

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Injekt calls, 28 function parameters, Context-based color math in list items)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`ReaderChaptersSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderChaptersSheet.kt#L40-L260), the chapter drawer bottom sheet exhibits severe parameter bloat and domain coupling:
> - **Lines 40–70**: Accepts **28 separate parameters** (including 10 boolean visibility flags, 4 drawable resource IDs, and 11 distinct lambda callbacks).
> - **Line 75**: Injects preferences via Service Locator inside Compose:
>   `val mangaDetailsPreferences = remember { Injekt.get<MangaDetailsPreferences>() }`.
> - **Line 78**: Calculates title visibility rules from domain models and preferences in UI:
>   `chapters.firstOrNull()?.manga?.hideChapterTitle(mangaDetailsPreferences) ?: false`.
> - **Lines 237–244**: Executes synchronous Context-based color evaluations inside list item render loops:
>   ```kotlin
>   val chapterColor = remember(item.chapter, item.isCurrent) {
>       Color(ChapterUtil.chapterColor(context, item.chapter))
>   }
>   val bookmarkColor = remember(item.chapter) {
>       Color(ChapterUtil.bookmarkColor(context, item.chapter))
>   }
>   ```
> - Evaluates `DecimalFormat` number formatting directly during row composition.
>
> **What This Proposal Solves:**
> Decouples `ReaderChaptersSheet` by consolidating the 28 parameters into a single immutable `ReaderChaptersSheetUiState`, moving color resolution and title formatting to background mappers in `ReaderViewModel`, and eliminating all `Injekt.get()` calls.

---

## 1. Executive Summary & Vision

A chapter selection sheet should be a fast, responsive list that instantly renders when swiped up by the user. Calculating text colors through Android `Context` utility methods (`ChapterUtil.chapterColor`) and formatting decimals on every item composition degrades scroll performance on manga with hundreds of chapters.

### The Objective
Decouple [`ReaderChaptersSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderChaptersSheet.kt) by:
1. Eliminating `Injekt.get<MangaDetailsPreferences>()` from the sheet composable.
2. Replacing the 28 function parameters with an immutable `ReaderChaptersSheetUiState` and a sealed `ReaderChaptersAction`.
3. Creating a lightweight `ReaderChapterRowUiModel` containing pre-formatted display text and styling colors.
4. Enabling effortless `@Preview` support for fast local iteration.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Flow
        Sheet1["ReaderChaptersSheet (28 Parameters)"] -->|Injekt.get()| Prefs["MangaDetailsPreferences"]
        Sheet1 --> Items["LazyColumn of ReaderChapterItem"]
        Items -->|Per-Item on Main Thread| CU["ChapterUtil.chapterColor(context)\nDecimalFormat"]
    end

    subgraph Proposed Decoupled Flow
        VM["ReaderViewModel"] --> Mapper["Background Chapter Mapper (Dispatchers.Default)"]
        Mapper --> State["ReaderChaptersSheetUiState\n(Pre-formatted titles, colors, action bar state)"]
        State --> Sheet2["Stateless ReaderChaptersSheet\n(Single State + Single Action Lambda)"]
        Sheet2 -->|onAction(ReaderChaptersAction)| VM
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable UI State Models

```kotlin
@Immutable
data class ReaderChaptersSheetUiState(
    val chapters: List<ReaderChapterRowUiModel> = emptyList(),
    val quickActions: List<ReaderQuickActionUiModel> = emptyList(),
    val currentChapterIndex: Int = -1,
    val isLoading: Boolean = false,
)

@Immutable
data class ReaderChapterRowUiModel(
    val id: Long,
    val formattedTitle: String,
    val formattedSubtitle: String?,
    val isCurrent: Boolean,
    val isRead: Boolean,
    val isBookmarked: Boolean,
    val textColor: Color,
    val bookmarkColor: Color,
)

@Immutable
data class ReaderQuickActionUiModel(
    val id: ReaderQuickActionId,
    val iconRes: Int,
    val tooltipRes: Int,
    val isEnabled: Boolean = true,
    val isToggled: Boolean = false,
)

enum class ReaderQuickActionId {
    Comments,
    WebView,
    ReadingMode,
    Rotation,
    CropBorders,
    Grayscale,
    DoublePage,
    ShiftPage,
    DisplayOptions,
}
```

### 3.2 Sealed Actions

```kotlin
sealed interface ReaderChaptersAction {
    data class SelectChapter(val chapterId: Long) : ReaderChaptersAction
    data class ToggleBookmark(val chapterId: Long) : ReaderChaptersAction
    data class QuickActionClick(val actionId: ReaderQuickActionId) : ReaderChaptersAction
    object Dismiss : ReaderChaptersAction
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless Composable Signature

Refactor [`ReaderChaptersSheet`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderChaptersSheet.kt) to accept a single UI state and action callback:

```kotlin
@Composable
fun ReaderChaptersSheet(
    uiState: ReaderChaptersSheetUiState,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    onAction: (ReaderChaptersAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSheet(themeColor = themeColorState) {
        // Render quick action bar from uiState.quickActions
        // Render LazyColumn using pre-computed uiState.chapters
    }
}
```

### 4.2 Streamlined Item Composable

`ChapterListItem` becomes a dumb layout without `Context` or `ChapterUtil`:
```kotlin
@Composable
private fun ChapterListItem(
    model: ReaderChapterRowUiModel,
    isLoading: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text(
            text = model.formattedTitle,
            color = model.textColor,
            fontWeight = if (model.isCurrent) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
```

---

## 5. Technical Footprint & Integration

1. **[`ReaderChaptersSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderChaptersSheet.kt)**: Replace lines 40–70 with clean 4-parameter signature; remove all `Injekt.get()` and `ChapterUtil` references.
2. **[`ReaderViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt)**: Pre-map chapters to `ReaderChapterRowUiModel` on background dispatchers.
3. **New Previews**: Add `@Preview` covering regular chapters, unread chapters, and current reading chapter states.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `ReaderChaptersSheetUiState`, `ReaderChapterRowUiModel`, and `ReaderChaptersAction`.
- [ ] **Step 2**: Implement background mapper in `ReaderViewModel`.
- [ ] **Step 3**: Refactor `ReaderChaptersSheet.kt` to eliminate the 28 parameters and `Injekt.get()`.
- [ ] **Step 4**: Add `@Preview` annotations for `ReaderChaptersSheet`.
- [ ] **Step 5**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
