# Technical Proposal: Decoupling DownloadScreen and DownloadChapterRow from Domain Grouping & Legacy Download States

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x UI Decoupling  
**Implementation State:** 🟡 Coupled Baseline (In-composable scanlator grouping, direct coupling to legacy `Download.State` enum, and inline dropdown allocations)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`DownloadScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/download/DownloadScreen.kt#L48-L65) and [`DownloadChapterRow.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/download/DownloadChapterRow.kt#L100-L180):
> - **In-Composable Grouping & Scanlator Parsing**:
>   ```kotlin
>   // DownloadScreen.kt: Lines 57–63
>   val downloadGroup =
>       remember(downloads) {
>           downloads.groupBy {
>               MergeType.getMergeTypeFromName(it.chapterItem.chapter.scanlator)?.scanlatorName
>                   ?: MdConstants.name
>           }
>       }
>   ```
>   The UI layer executes domain grouping rules and scanlator string extraction directly inside the Composable using `remember(downloads)`.
> - **Legacy State Inspection in Row**:
>   ```kotlin
>   // DownloadChapterRow.kt: Line 127
>   when (download.chapterItem.downloadState == Download.State.QUEUE) {
>       true -> LinearWavyProgressIndicator(...)
>       false -> { val animatedProgress by animateFloatAsState(...) ... }
>   }
>   ```
>   The row checks `Download.State.QUEUE` from the legacy `eu.kanade.tachiyomi.data.download.model.Download` package to decide whether to animate the progress indicator.
> - **Allocation of Dropdown Menus on Recomposition**:
>   In [`DownloadChapterRow.kt:L185-L250`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/download/DownloadChapterRow.kt#L185-L250), `getDropDownItems(...)` re-allocates a multi-level list of `SimpleDropDownItem.Parent` and `SimpleDropDownItem.Action` objects with nested closures on every recomposition.
>
> **What This Proposal Solves:**
> Moves scanlator grouping into `DownloadViewModel` (or a domain interactor), maps items into an immutable `DownloadScreenUiModel`, replaces legacy `Download.State` checks with an explicit `DownloadDisplayState` UI model, and avoids repeated menu allocations.

---

## 1. Executive Summary & Vision

Jetpack Compose screens should focus strictly on visual rendering and layout. When a screen performs collection grouping, scanlator parsing with `MergeType`, and inspects legacy background download engine states, it couples UI code to data layer internals and increases recomposition overhead.

### The Objective
Decouple [`DownloadScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/download/DownloadScreen.kt) and [`DownloadChapterRow.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/download/DownloadChapterRow.kt) by:
1. Hoisting the `groupBy` logic to `DownloadViewModel`, emitting pre-grouped section models.
2. Defining a dedicated, immutable `DownloadRowUiModel` containing pre-computed progress, formatted titles, and display states.
3. Decoupling the progress indicator from `Download.State` by introducing an explicit `DownloadDisplayState` (`Queued`, `Downloading(progress)`, `Error`).
4. Enabling standalone `@Preview`s for individual download rows and grouped sections without requiring active database models.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Flow
        RawDownloads["List<DownloadItem>"] --> Screen["DownloadScreen Composable"]
        Screen -->|remember(downloads)| Grouping["downloads.groupBy { MergeType.getMergeTypeFromName(...) }"]
        Grouping --> Row["DownloadChapterRow Composable"]
        Row -->|Inspects| LegacyEnum["Download.State.QUEUE"]
        Row -->|Allocates| InlineMenu["getDropDownItems() allocation on recompose"]
    end

    subgraph Proposed Decoupled Flow
        RawDownloads2["DownloadManager & Repositories"] --> VM["DownloadViewModel"]
        VM -->|Groups & Formats| UiModel["DownloadScreenUiModel (Pre-grouped Sections)"]
        UiModel --> Screen2["Stateless DownloadScreen"]
        Screen2 --> Row2["Stateless DownloadChapterRow"]
        Row2 -->|Renders| CleanState["DownloadRowUiModel with DownloadDisplayState"]
        Row2 -->|Emits| Actions["DownloadRowAction (MoveTop, MoveBottom, Cancel)"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable Download UI State Models

```kotlin
@Immutable
data class DownloadScreenUiModel(
    val sections: List<DownloadSectionUiModel> = emptyList(),
    val isDownloaderRunning: Boolean = false,
    val totalDownloadsCount: Int = 0,
)

@Immutable
data class DownloadSectionUiModel(
    val sourceName: String,
    val items: List<DownloadRowUiModel>,
)

@Immutable
data class DownloadRowUiModel(
    val downloadId: Long,
    val mangaTitle: String,
    val chapterName: String,
    val displayState: DownloadDisplayState,
    val isFirst: Boolean,
    val isLast: Boolean,
)

@Immutable
sealed interface DownloadDisplayState {
    data object Queued : DownloadDisplayState
    data class Downloading(val progress: Float) : DownloadDisplayState
    data object Paused : DownloadDisplayState
    data class Error(val message: String?) : DownloadDisplayState
}
```

### 3.2 Sealed Actions

```kotlin
sealed interface DownloadRowAction {
    data class MoveDownload(val downloadId: Long, val direction: MoveDownloadDirection) : DownloadRowAction
    data class MoveSeries(val downloadId: Long, val direction: MoveDownloadDirection) : DownloadRowAction
    data class CancelSeries(val downloadId: Long) : DownloadRowAction
    data class DeleteDownload(val downloadId: Long) : DownloadRowAction
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Pure Stateless DownloadScreen

```kotlin
@Composable
fun DownloadScreen(
    uiModel: DownloadScreenUiModel,
    contentPadding: PaddingValues,
    onRowAction: (DownloadRowAction) -> Unit,
    onClearSource: (sourceName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberLazyListState()

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = Size.small),
        state = scrollState,
        contentPadding = contentPadding,
    ) {
        uiModel.sections.forEach { section ->
            item(key = section.sourceName) {
                DownloadSourceHeader(
                    sourceName = section.sourceName,
                    itemCount = section.items.size,
                    onClear = { onClearSource(section.sourceName) },
                )
            }
            items(section.items, key = { it.downloadId }) { rowModel ->
                DownloadChapterRow(
                    model = rowModel,
                    onAction = onRowAction,
                )
            }
        }
    }
}
```

### 4.2 Streamlined DownloadChapterRow

```kotlin
@Composable
fun DownloadChapterRow(
    model: DownloadRowUiModel,
    onAction: (DownloadRowAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pure rendering based on model.displayState without querying Download.State.QUEUE
    val progress = when (val state = model.displayState) {
        is DownloadDisplayState.Queued -> 0f
        is DownloadDisplayState.Downloading -> state.progress
        else -> 0f
    }
    // ...
}
```

---

## 5. Technical Footprint & Integration

1. **[`DownloadScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/download/DownloadScreen.kt)**:
   - Remove lines 57–63 (`remember(downloads) { downloads.groupBy { ... } }`).
   - Accept `DownloadScreenUiModel` instead of raw `List<DownloadItem>`.
2. **[`DownloadChapterRow.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/download/DownloadChapterRow.kt)**:
   - Remove `import eu.kanade.tachiyomi.data.download.model.Download`.
   - Replace `download: DownloadItem` parameter with `DownloadRowUiModel`.
   - Hoist or memoize dropdown actions.
3. **[`FeedViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/feed/FeedViewModel.kt)** (or `DownloadViewModel`):
   - Perform the grouping transformation in `StateFlow` combining downloads and scanlator information.
4. **New Previews**: Add `@Preview` for `DownloadChapterRow` in Queued, Downloading, and Error states.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `DownloadScreenUiModel`, `DownloadRowUiModel`, and `DownloadDisplayState`.
- [ ] **Step 2**: Implement the mapping and grouping logic in the ViewModel layer.
- [ ] **Step 3**: Refactor `DownloadScreen.kt` to consume the pre-grouped UI model.
- [ ] **Step 4**: Refactor `DownloadChapterRow.kt` to consume `DownloadRowUiModel` and eliminate `Download.State` import.
- [ ] **Step 5**: Add `@Preview`s for download rows in various states.
- [ ] **Step 6**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
