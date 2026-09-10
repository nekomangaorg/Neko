# Technical Proposal: Decoupling ReaderSettingsSheet from Service Locators, Preference Mutations & Domain Flags

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Injekt calls, direct preference mutations, and domain flag parsing in Compose)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`ReaderSettingsSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderSettingsSheet.kt#L75-L250), the settings bottom sheet couples directly to the DI container, domain model flags, and SharedPreferences:
> - **Line 75**: Injects preferences via Service Locator inside the Composable:
>   `val readerPreferences: ReaderPreferences = remember { Injekt.get() }`.
> - **Lines 79–88**: Parses domain flags and bitmasks in Compose:
>   ```kotlin
>   val currentReadingMode = remember(manga?.readingModeType, manga?.viewerFlags, defaultReadingMode) {
>       if (manga == null) defaultReadingMode
>       else {
>           val viewer = if (manga.readingModeType == 0) defaultReadingMode else manga.readingModeType
>           if (manga.isLongStrip()) ReadingModeType.WEBTOON.flagValue else viewer
>       }
>   }
>   ```
> - **Lines 244, 250+**: Mutates SharedPreferences directly from UI event listeners:
>   `onSelected = { index -> readerPreferences.readerTheme().set(index) }`.
> - Passes `readerPreferences` down through multiple tabs (`GeneralSettingsTab`, `LayoutSettingsTab`, `FilterSettingsTab`).
>
> **What This Proposal Solves:**
> Removes all `Injekt.get()` calls, preference mutations, and domain flag parsing from `ReaderSettingsSheet`. Replaces them with an immutable `ReaderSettingsUiState` and a unified `ReaderSettingsAction` callback, enabling `@Preview` support and testing without Android dependencies.

---

## 1. Executive Summary & Vision

A settings sheet in Jetpack Compose should be a purely declarative UI component displaying options and forwarding user selections to a ViewModel. Injecting `ReaderPreferences` and mutating disk preferences directly from Composable lambda callbacks violates Unidirectional Data Flow (UDF), bypasses ViewModel lifecycle validation, and makes the component impossible to preview in Android Studio.

### The Objective
Decouple [`ReaderSettingsSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderSettingsSheet.kt) by:
1. Removing `Injekt.get<ReaderPreferences>()` from the Composable function.
2. Hoisting all preference mutations (`.set(...)`) up to `ReaderViewModel`.
3. Pre-calculating effective reading mode and orientation options before passing them to the sheet.
4. Defining an immutable `ReaderSettingsUiState` and a sealed `ReaderSettingsAction` interface.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Flow
        Sheet["ReaderSettingsSheet Composable"] -->|Injekt.get()| Prefs["ReaderPreferences Store"]
        Sheet -->|Reads & Parses| Manga["manga.isLongStrip() & bitmasks"]
        Sheet -->|Direct Mutation| Set["readerPreferences.readerTheme().set(index)"]
    end

    subgraph Proposed Decoupled Architecture
        RVM["ReaderViewModel"] -->|Collects & Maps Preferences| State["ReaderSettingsUiState (Immutable)"]
        State -->|Renders UI| Sheet2["Stateless ReaderSettingsSheet"]
        Sheet2 -->|onAction(ReaderSettingsAction)| RVM
        RVM -->|Updates Preferences| Prefs2["ReaderPreferences Store"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable Settings UI State

```kotlin
@Immutable
data class ReaderSettingsUiState(
    val selectedTab: Int = 0,
    val isWebtoon: Boolean = false,
    val readingModeIndex: Int = 0,
    val orientationIndex: Int = 0,
    val themeIndex: Int = 0,
    val sliderPositionIndex: Int = 0,
    val showPageNumber: Boolean = true,
    val keepScreenOn: Boolean = true,
    val cropBorders: Boolean = false,
    val grayscale: Boolean = false,
    val invertedColors: Boolean = false,
    val webtoonSidePadding: Int = 0,
    val webtoonDisableGaps: Boolean = false,
)
```

### 3.2 Sealed Settings Actions

```kotlin
sealed interface ReaderSettingsAction {
    data class SetReadingMode(val type: ReadingModeType) : ReaderSettingsAction
    data class SetOrientation(val type: OrientationType) : ReaderSettingsAction
    data class SetTheme(val themeIndex: Int) : ReaderSettingsAction
    data class SetSliderPosition(val positionIndex: Int) : ReaderSettingsAction
    data class ToggleShowPageNumber(val show: Boolean) : ReaderSettingsAction
    data class ToggleKeepScreenOn(val keep: Boolean) : ReaderSettingsAction
    data class ToggleCropBorders(val crop: Boolean) : ReaderSettingsAction
    data class ToggleGrayscale(val grayscale: Boolean) : ReaderSettingsAction
    data class ToggleInvertedColors(val inverted: Boolean) : ReaderSettingsAction
    data class SetWebtoonSidePadding(val padding: Int) : ReaderSettingsAction
    data class ToggleWebtoonDisableGaps(val disabled: Boolean) : ReaderSettingsAction
    object OpenFullSettings : ReaderSettingsAction
    object Dismiss : ReaderSettingsAction
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless Composable Signature

```kotlin
@Composable
fun ReaderSettingsSheet(
    uiState: ReaderSettingsUiState,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    onAction: (ReaderSettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSheet(themeColor = themeColorState) {
        // Render tabs purely based on uiState values
        // Emit onAction(ReaderSettingsAction.SetTheme(newIndex)) on click
    }
}
```

### 4.2 Removing Preference Injections in Tabs

`GeneralSettingsTab`, `LayoutSettingsTab`, and `FilterSettingsTab` no longer receive `ReaderPreferences`. They accept plain primitive values and action lambdas:
```kotlin
@Composable
private fun GeneralSettingsTab(
    uiState: ReaderSettingsUiState,
    onAction: (ReaderSettingsAction) -> Unit,
)
```

---

## 5. Technical Footprint & Integration

1. **[`ReaderSettingsSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderSettingsSheet.kt)**: Remove lines 75–88 and all `readerPreferences.*.set()` calls.
2. **[`ReaderViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt)**: Expose `readerSettingsUiState: StateFlow<ReaderSettingsUiState>` and handle `ReaderSettingsAction`.
3. **New Previews**: Add `@Preview` for each settings tab (General, Layout, Filter) with mock UI states.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `ReaderSettingsUiState` and `ReaderSettingsAction`.
- [ ] **Step 2**: Add preference flow aggregation and action handler in `ReaderViewModel`.
- [ ] **Step 3**: Refactor `ReaderSettingsSheet.kt` to be 100% stateless.
- [ ] **Step 4**: Add `@Preview` annotations for `ReaderSettingsSheet`.
- [ ] **Step 5**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
