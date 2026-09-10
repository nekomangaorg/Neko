# Technical Proposal: Decoupling ChapterRow & Chapter Actions from Domain Logic and Database Entities

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Compose & Domain Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Domain checks, DB entity conversions, and Intent building inside Compose)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`MangaScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/MangaScreen.kt#L298-L364), critical domain business rules and entity conversions are executed directly inside Composable callback lambdas:
> - **Lines 298–310 (`chapterActions.download`)**: Checks `MdConstants.UnsupportedOfficialGroupList.contains(chapter.scanlator)` and fires hardcoded toasts.
> - **Lines 314–351 (`openNext` and `open`)**:
>   1. Converts domain chapter to legacy database model: `simpleChapter.toDbChapter()`.
>   2. Queries domain manga: `mangaViewModel.getManga()`.
>   3. Evaluates scanlator blacklists: `MdConstants.UnsupportedOfficialGroupList.contains(...)`.
>   4. Checks download status via domain service: `chapter.isAvailable(mangaViewModel.downloadManager, manga)`.
>   5. Launches `ReaderActivity.newIntent(context, manga, chapter)` directly from the Composable lambda with hardcoded toast strings.
> - In [`ChapterRow.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/ChapterRow.kt#L172-L255), each list row:
>   - Builds contextual dropdown menus dynamically inside `remember` blocks.
>   - Executes date computations (`ChapterUtil.relativeDate`), plural string resolutions (`R.plurals.pages_left`), and business rules (`isMergedChapter()`, `isLocalSource()`) on the UI thread during list rendering.
>
> **What This Proposal Solves:**
> Decouples chapter opening, downloading, and row rendering. Moves all scanlator validation, availability checking, and Reader intent preparation into a domain UseCase (`OpenChapterUseCase`), while transforming `ChapterRow` into a lightweight, stateless component consuming an immutable `ChapterRowUiModel`.

---

## 1. Executive Summary & Vision

A chapter list item in a manga reader app is one of the most frequently rendered components. Performing database conversions (`toDbChapter()`), checking file availability against `DownloadManager`, and formatting plurals on every scroll or recomposition hurts frame rates and scatters domain rules across Compose callbacks.

### The Objective
Decouple [`ChapterRow.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/ChapterRow.kt) and [`MangaScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/MangaScreen.kt) by:
1. Moving chapter availability and scanlator validation out of Compose into a domain `OpenChapterUseCase`.
2. Eliminating legacy database conversions (`toDbChapter()`) in presentation layers.
3. Defining an immutable `ChapterRowUiModel` that contains pre-formatted strings, badges, and action capabilities.
4. Unifying chapter interactions behind a single sealed `ChapterUiAction` callback.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Flow
        Row1["ChapterRow (passes ChapterItem)"] --> Screen1["MangaScreen Composable"]
        Screen1 -->|toDbChapter()| DB["Legacy DB Models"]
        Screen1 -->|isAvailable(DownloadManager)| DM["DownloadManager Check in UI"]
        Screen1 -->|Checks Unsupported Groups| UG["Hardcoded Toasts in Compose"]
        Screen1 -->|ReaderActivity.newIntent| RA1["Direct Activity Launch"]
    end

    subgraph Proposed Clean Architecture
        Row2["Stateless ChapterRow"] -->|ChapterUiAction.Open(chapterId)| VM["MangaViewModel"]
        VM --> UC["OpenChapterUseCase"]
        UC -->|Validates Group & Availability| Rules{"Valid & Available?"}
        Rules -->|No| Err["Emit UiText Snackbar/Toast Event"]
        Rules -->|Yes| Intent["Emit LaunchReaderEvent(intentPayload)"]
        Intent --> Screen2["MangaScreen Activity Observer"]
        Screen2 --> RA2["ReaderActivity Launch"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable `ChapterRowUiModel`

```kotlin
@Immutable
data class ChapterRowUiModel(
    val id: Long,
    val name: String,
    val chapterNumber: Double,
    val formattedSubtitle: String,
    val pagesLeftText: String?,
    val isRead: Boolean,
    val isBookmarked: Boolean,
    val isDownloaded: Boolean,
    val isUnsupportedGroup: Boolean,
    val isUnavailable: Boolean,
    val downloadProgress: Float = 0f,
    val downloadState: ChapterDownloadState = ChapterDownloadState.None,
)

enum class ChapterDownloadState {
    None,
    Queued,
    Downloading,
    Downloaded,
    Error,
}
```

### 3.2 Sealed UI Actions

```kotlin
sealed interface ChapterUiAction {
    data class Open(val chapterId: Long) : ChapterUiAction
    data class Bookmark(val chapterId: Long, val bookmarked: Boolean) : ChapterUiAction
    data class MarkRead(val chapterId: Long, val read: Boolean) : ChapterUiAction
    data class Download(val chapterId: Long, val action: MangaConstants.DownloadAction) : ChapterUiAction
    data class OpenInBrowser(val chapterId: Long) : ChapterUiAction
    data class OpenComment(val chapterId: Long) : ChapterUiAction
    data class BlockScanlator(val type: MangaConstants.BlockType, val scanlator: String) : ChapterUiAction
    data class MarkPrevious(val chapterId: Long, val read: Boolean) : ChapterUiAction
}
```

### 3.3 Extracted `OpenChapterUseCase`

```kotlin
class OpenChapterUseCase(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    private val downloadManager: DownloadManager,
) {
    sealed interface OpenResult {
        data class Success(val manga: Manga, val chapter: Chapter) : OpenResult
        data class UnsupportedScanlator(val scanlator: String) : OpenResult
        object ChapterUnavailable : OpenResult
    }

    suspend operator fun invoke(mangaId: Long, chapterId: Long): OpenResult = withContext(Dispatchers.Default) {
        val manga = mangaRepository.getManga(mangaId) ?: return@withContext OpenResult.ChapterUnavailable
        val chapter = chapterRepository.getChapter(chapterId) ?: return@withContext OpenResult.ChapterUnavailable
        
        if (chapter.scanlator != null && MdConstants.UnsupportedOfficialGroupList.contains(chapter.scanlator)) {
            return@withContext OpenResult.UnsupportedScanlator(chapter.scanlator)
        }
        
        if (!chapter.isAvailable(downloadManager, manga)) {
            return@withContext OpenResult.ChapterUnavailable
        }
        
        OpenResult.Success(manga, chapter)
    }
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless `ChapterRow`

```kotlin
@Composable
fun ChapterRow(
    model: ChapterRowUiModel,
    themeColor: ThemeColorState,
    shouldHideChapterTitles: Boolean,
    onAction: (ChapterUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pure rendering without ChapterUtil.relativeDate or plural calculations
}
```

### 4.2 Streamlined `MangaScreen.kt`

All complex checks and DB mappings are removed from `MangaScreen.kt`:
```kotlin
chapterActions = ChapterActions(
    open = { chapter -> mangaViewModel.openChapter(chapter.id) },
    openNext = { mangaViewModel.openNextUnreadChapter() },
    download = { chapters, action -> mangaViewModel.downloadChapters(chapters.map { it.id }, action) },
    ...
)
```

The screen observer receives a single navigation event:
```kotlin
when (event) {
    is MangaUiEvent.OpenReader -> {
        context.startActivity(ReaderActivity.newIntent(context, event.manga, event.chapter))
    }
    is MangaUiEvent.ShowNotice -> {
        context.toast(event.message.asString(context))
    }
}
```

---

## 5. Technical Footprint & Integration

1. **[`ChapterRow.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/ChapterRow.kt)**: Replace 8 lambda callbacks with single `(ChapterUiAction) -> Unit`; accept `ChapterRowUiModel`.
2. **[`MangaScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/MangaScreen.kt)**: Remove lines 298–364; route chapter opening and downloads to `MangaViewModel`.
3. **[`MangaViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt)**: Inject `OpenChapterUseCase` and emit `OpenReader` events.
4. **New UseCase**: Implement `OpenChapterUseCaseTest` testing all availability and official group branches.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Implement `OpenChapterUseCase` and write unit tests covering supported and unsupported scanlators.
- [ ] **Step 2**: Create `ChapterRowUiModel` and `ChapterUiAction`.
- [ ] **Step 3**: Move relative date, status text, and plural resolution to a background mapper in `MangaViewModel`.
- [ ] **Step 4**: Refactor `ChapterRow.kt` to be purely stateless and add `@Preview`.
- [ ] **Step 5**: Update `MangaScreen.kt` to delegate chapter opening to `MangaViewModel`.
- [ ] **Step 6**: Validate with `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
