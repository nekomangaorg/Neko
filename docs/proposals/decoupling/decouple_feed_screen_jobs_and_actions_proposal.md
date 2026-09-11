# Technical Proposal: Decoupling FeedScreen from Background Job Orchestration, Duplicate Rules & Flow Propagation

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Compose & Domain Decoupling  
**Implementation State:** 🟡 Coupled Baseline (WorkManager lifecycle management, duplicate business rules, and StateFlow prop-drilling)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`FeedScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/FeedScreen.kt#L144-L198), UI composables manage background services, duplicate domain validations, and pass raw `StateFlow` instances:
> - **Lines 144–156 (`downloadClick`)**:
>   Duplicates domain business logic and hardcoded toast messages across screens:
>   ```kotlin
>   if (MdConstants.UnsupportedOfficialGroupList.contains(chapterItem.chapter.scanlator)) {
>       context.toast("${chapterItem.chapter.scanlator} not supported, try WebView")
>   } else if (chapterItem.chapter.isUnavailable) {
>       context.toast("Chapter is not available")
>   } else {
>       feedViewModel.downloadChapter(chapterItem, feedManga, downloadAction)
>   }
>   ```
> - **Lines 158–164 (`updateLibrary`)**:
>   Directly queries and manipulates `LibraryUpdateJob` state inside the Composable action block:
>   ```kotlin
>   if (LibraryUpdateJob.isRunning(context) && !start) {
>       LibraryUpdateJob.stop(context)
>   } else if (!LibraryUpdateJob.isRunning(context) && start) {
>       LibraryUpdateJob.startNow(context)
>   }
>   ```
> - **Lines 182–198 (`FeedWrapper`)**:
>   Drills 4 raw `StateFlow<T>` objects (`feedScreenFlow`, `updateScreenFlow`, `historyScreenFlow`, `summaryScreenFlow`) into child composables and collects them using `collectAsState()` without lifecycle boundaries (`collectAsStateWithLifecycle()`).
>
> **What This Proposal Solves:**
> Hoists library job start/stop triggers to `FeedViewModel`, centralizes download eligibility validation in a shared domain UseCase, and refactors `FeedWrapper` to accept evaluated, immutable UI state models collected safely at the screen boundary.

---

## 1. Executive Summary & Vision

Propagating `StateFlow` objects down into nested composables prevents UI reusability, forces subcomposables to know about reactive streams, and risks collecting flows when the screen is in the background (unless lifecycle-aware). Additionally, copying and pasting validation checks for unsupported scanlator groups between `MangaScreen` and `FeedScreen` leads to bug drift.

### The Objective
Decouple [`FeedScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/FeedScreen.kt) by:
1. Delegating library update execution (`startNow`/`stop`) to `FeedViewModel`.
2. Reusing domain chapter download validation across screens.
3. Hoisting all `StateFlow` collection to the top-level `FeedScreen` using `collectAsStateWithLifecycle()`.
4. Passing evaluated immutable data models (`feedState`, `updatesState`, `historyState`, `summaryState`) into `FeedWrapper`.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Architecture
        UI["FeedWrapper Composable"] -->|Takes StateFlow<T>| CF["collectAsState()"]
        UI -->|Direct Call| LUJ["LibraryUpdateJob.startNow() / stop()"]
        UI -->|Scanlator Rule & Toasts| Dupe["Duplicated Blacklist Check"]
    end

    subgraph Proposed Architecture
        VM["FeedViewModel"] -->|Controls| LUJ2["LibraryUpdateJob"]
        VM -->|Validates via| DUC["ValidateChapterDownloadUseCase"]
        Screen["FeedScreen (Top-Level)"] -->|collectAsStateWithLifecycle()| VM
        Screen -->|Evaluated Immutable States| Wrapper["Stateless FeedWrapper\n(state, onAction)"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Shared Domain `ValidateChapterDownloadUseCase`

```kotlin
class ValidateChapterDownloadUseCase {
    sealed interface DownloadEligibility {
        object Eligible : DownloadEligibility
        data class UnsupportedGroup(val groupName: String) : DownloadEligibility
        object Unavailable : DownloadEligibility
    }

    operator fun invoke(scanlator: String?, isUnavailable: Boolean): DownloadEligibility {
        if (scanlator != null && MdConstants.UnsupportedOfficialGroupList.contains(scanlator)) {
            return DownloadEligibility.UnsupportedGroup(scanlator)
        }
        if (isUnavailable) {
            return DownloadEligibility.Unavailable
        }
        return DownloadEligibility.Eligible
    }
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Hoisting State Collection in `FeedScreen`

```kotlin
@Composable
fun FeedScreen(
    navigationRail: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    feedViewModel: FeedViewModel,
    mainDropdown: AppBar.MainDropdown,
    mainDropdownShowing: Boolean,
    openManga: (Long) -> Unit,
    windowSizeClass: WindowSizeClass,
) {
    // Collect once at screen root with lifecycle awareness
    val feedState by feedViewModel.feedScreenState.collectAsStateWithLifecycle()
    val updatesState by feedViewModel.updatesScreenPagingState.collectAsStateWithLifecycle()
    val historyState by feedViewModel.historyScreenPagingState.collectAsStateWithLifecycle()
    val summaryState by feedViewModel.summaryScreenPagingState.collectAsStateWithLifecycle()

    FeedWrapper(
        feedScreenState = feedState,
        updatesScreenState = updatesState,
        historyScreenState = historyState,
        summaryScreenState = summaryState,
        onDownloadChapter = feedViewModel::downloadChapter,
        onToggleLibraryUpdate = feedViewModel::toggleLibraryUpdate,
        ...
    )
}
```

### 4.2 Decoupling `FeedWrapper`

```kotlin
@Composable
private fun FeedWrapper(
    feedScreenState: FeedScreenState,
    updatesScreenState: UpdatesScreenPagingState,
    historyScreenState: HistoryScreenPagingState,
    summaryScreenState: SummaryScreenPagingState,
    onDownloadChapter: (ChapterItem, FeedManga, MangaConstants.DownloadAction) -> Unit,
    onToggleLibraryUpdate: (Boolean) -> Unit,
    ...
)
```

No `StateFlow` types are passed into `FeedWrapper`. The child composable becomes purely stateless and previewable.

---

## 5. Technical Footprint & Integration

1. **[`FeedScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/FeedScreen.kt)**: Replace `collectAsState()` with `collectAsStateWithLifecycle()`; remove `LibraryUpdateJob` imports and download validation branch.
2. **[`FeedViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/feed/FeedViewModel.kt)**: Implement `toggleLibraryUpdate(start: Boolean)` and inject `ValidateChapterDownloadUseCase`.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Implement `ValidateChapterDownloadUseCase` and write unit tests.
- [ ] **Step 2**: Add `toggleLibraryUpdate()` to `FeedViewModel`.
- [ ] **Step 3**: Update `FeedViewModel.downloadChapter()` to validate eligibility and emit feedback.
- [ ] **Step 4**: Refactor `FeedScreen.kt` to hoist all `collectAsStateWithLifecycle()` calls and pass pure models to `FeedWrapper`.
- [ ] **Step 5**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
