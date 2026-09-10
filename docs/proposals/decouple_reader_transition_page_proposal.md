# Technical Proposal: Decoupling ReaderTransitionPage from DownloadManager, Legacy Entity Conversions & Chapter Gap Math

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling  
**Implementation State:** 🟡 Coupled Baseline (DownloadManager injected into Composable, legacy DB conversions, and gap math in UI)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`ReaderTransitionPage.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ReaderTransitionPage.kt#L50-L350), the chapter transition interstitial page couples directly to the download subsystem, legacy database models, and chapter gap calculations:
> - **Line 53**: Directly accepts `downloadManager: DownloadManager` as a Composable parameter.
> - **Lines 149–161**: Collects download queues and performs chapter download verification inside the UI render pass:
>   ```kotlin
>   val queue by downloadManager.queueState.collectAsStateWithLifecycle()
>   val isPrevDownloaded = remember(prevChapter, manga, queue) {
>       manga?.let { downloadManager.isChapterDownloaded(prevChapter.chapter, it.toManga()) } ?: false
>   }
>   ```
> - **Lines 153 & 159**: Converts domain models to legacy database entities inside Compose: `it.toManga()`.
> - **Lines 306–318 (`MissingChapterWarningSection`)**: Executes domain gap arithmetic on the UI thread:
>   ```kotlin
>   val hasMissing = hasMissingChapters(transition.from, transition.to)
>   val diff = calculateChapterDifference(transition.from, transition.to)
>   ```
>
> **What This Proposal Solves:**
> Strips `DownloadManager`, `toManga()`, and gap arithmetic from `ReaderTransitionPage`. The component consumes a pure, immutable `ChapterTransitionUiModel` prepared by the domain layer and `ReaderViewModel`.

---

## 1. Executive Summary & Vision

A transition page in a reader displays contextual metadata when moving between chapters (e.g. "Previous: Ch. 12 -> Next: Ch. 13", missing chapter warnings, and download indicators). Inverting control so that the Composable directly inspects download queues and executes mathematical algorithms violates clean architecture and slows down page flip transitions.

### The Objective
Decouple [`ReaderTransitionPage.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ReaderTransitionPage.kt) by:
1. Removing `DownloadManager` from the Composable parameter list.
2. Eliminating legacy `it.toManga()` model conversions in presentation code.
3. Moving `hasMissingChapters` and `calculateChapterDifference` calculations to a domain UseCase.
4. Supplying a pre-calculated `ChapterTransitionUiModel` directly to `ReaderTransitionPage`.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Flow
        Page1["ReaderTransitionPage Composable"] -->|Collects Flow| DM["downloadManager.queueState"]
        Page1 -->|it.toManga()| DB["Legacy DB Manga"]
        Page1 -->|In-UI Math| Gap["hasMissingChapters() & calculateChapterDifference()"]
    end

    subgraph Proposed Decoupled Flow
        VM["ReaderViewModel"] --> UC["ResolveChapterTransitionUiModelUseCase"]
        UC -->|Queries Download Status & Gap Math| Model["ChapterTransitionUiModel (Pure Immutable Model)"]
        Model --> Page2["Stateless ReaderTransitionPage"]
        Page2 -->|onRetryChapter(chapterId)| VM
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable Transition UI Model

```kotlin
@Immutable
sealed interface ChapterTransitionUiModel {
    val fromChapterName: String
    val isFromDownloaded: Boolean

    @Immutable
    data class Prev(
        override val fromChapterName: String,
        override val isFromDownloaded: Boolean,
        val toChapter: TargetChapterInfo?,
        val missingChaptersCount: Int = 0,
    ) : ChapterTransitionUiModel

    @Immutable
    data class Next(
        override val fromChapterName: String,
        override val isFromDownloaded: Boolean,
        val toChapter: TargetChapterInfo?,
        val missingChaptersCount: Int = 0,
    ) : ChapterTransitionUiModel

    @Immutable
    data class TargetChapterInfo(
        val chapterId: Long,
        val name: String,
        val isDownloaded: Boolean,
        val preloadState: PreloadState = PreloadState.Ready,
    )
}

enum class PreloadState {
    Ready,
    Loading,
    Error,
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless `ReaderTransitionPage`

```kotlin
@Composable
fun ReaderTransitionPage(
    uiModel: ChapterTransitionUiModel,
    onRetry: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onTap: ((PointF) -> Unit)? = null,
) {
    // Pure declarative rendering based on uiModel properties:
    // - uiModel.toChapter?.name
    // - uiModel.isFromDownloaded vs uiModel.toChapter?.isDownloaded
    // - uiModel.missingChaptersCount (directly passes count into pluralStringResource)
}
```

No `DownloadManager`, no `queueState.collectAsState()`, and no `toManga()` calls exist in the composable.

---

## 5. Technical Footprint & Integration

1. **[`ReaderTransitionPage.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/ReaderTransitionPage.kt)**: Remove lines 50–57 and all `DownloadManager` / `toManga()` invocations.
2. **[`ReaderViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderViewModel.kt)**: Map `ChapterTransition` to `ChapterTransitionUiModel` when transitions are instantiated.
3. **New Previews**: Add `@Preview` for `PrevTransition`, `NextTransition`, and `MissingChaptersWarning`.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `ChapterTransitionUiModel`.
- [ ] **Step 2**: Create transition mapping logic in `ReaderViewModel` combining chapter gap math and download status.
- [ ] **Step 3**: Refactor `ReaderTransitionPage.kt` to consume `ChapterTransitionUiModel`.
- [ ] **Step 4**: Add `@Preview` annotations for all transition states.
- [ ] **Step 5**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
