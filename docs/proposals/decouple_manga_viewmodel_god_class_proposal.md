# Technical Proposal: Deconstructing MangaViewModel God Class into Modular Domain Controllers

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Architecture Overhaul  
**Implementation State:** 🟡 Coupled Baseline (2,387-line God ViewModel with 24+ injected dependencies orchestrating 6 distinct subdomains)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`MangaViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt#L1-L175):
> - **Extreme Class Size & Dependency Fan-out**:
>   - Spans **2,387 lines** of code.
>   - Injects **24 separate dependencies** via Service Locator (`Injekt.get()`):
>     `PreferencesHelper`, `MangaDexPreferences`, `LibraryPreferences`, `SecurityPreferences`, `MangaDetailsPreferences`, `CoverCache`, `MangaRepository`, `MangaAggregateRepository`, `MergeMangaRepository`, `ArtworkRepository`, `ChapterRepository`, `HistoryRepository`, `TrackRepository`, `DownloadManager`, `AppSnackbarManager`, `ChapterItemFilter`, `SourceManager`, `MangaDexLoginHelper`, `StatusHandler`, `TrackManager`, `StorageManager`, `ChapterUseCases`, `TrackUseCases`, `CategoryUseCases`, `MangaUseCases`.
> - **Overlapping Subdomain Responsibilities**:
>   - **Tracking**: Manages tracking services (MAL, AniList, Kitsu, Shikimori, Bangumi), tracking searches, date updates, and score syncing.
>   - **Merging**: Queries secondary merge sources, links/unlinks merged manga, and manages merged chapter feeds.
>   - **Artwork & Covers**: Fetches remote covers, saves custom artwork to disk, purges cover cache, and generates share intents.
>   - **Chapters & Downloads**: Sorts, filters by scanlator, calculates missing chapter ranges, updates read states, and enqueues downloads.
>   - **Metadata & Bookmarks**: Syncs MangaDex attributes, manages categories, and updates reading statuses.
> - **Testability Barrier**:
>   - Writing a unit test for `MangaViewModel` requires mocking 24 distinct services, repositories, and preferences helpers, making comprehensive testing virtually impossible.
>
> **What This Proposal Solves:**
> Deconstructs `MangaViewModel` into 4 focused, independently testable feature controllers (`MangaChapterController`, `MangaTrackingController`, `MangaMergeController`, `MangaArtworkController`). `MangaViewModel` becomes a lightweight coordinator aggregating state flows from these controllers into `MangaScreenUiState`.

---

## 1. Executive Summary & Vision

A single ViewModel should not serve as the aggregate controller for an entire complex screen containing 6 distinct modal workflows. By decomposing `MangaViewModel` into dedicated feature controllers using class delegation or component composition, we drastically improve maintainability, reduce cognitive load, and enable isolated unit testing.

### The Objective
Refactor [`MangaViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt) by:
1. Extracting tracking business logic into `MangaTrackingController`.
2. Extracting merge manga operations into `MangaMergeController`.
3. Extracting artwork management into `MangaArtworkController`.
4. Extracting chapter filtering, sorting, and batch actions into `MangaChapterController`.
5. Reducing `MangaViewModel` footprint from 2,387 lines down to under 500 lines, serving purely as a state combiner.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Monolithic God Architecture (Current)
        MVM_Old["MangaViewModel (2,387 lines)"] -->|24 Injekt.get() calls| Deps["Repositories, Cache, Preferences, Network, Downloads, Trackers"]
    end

    subgraph Modular Feature Controller Architecture (Proposed)
        MVM["MangaViewModel (<500 lines Coordinator)"]
        
        MVM --> MCController["MangaChapterController"]
        MVM --> MTController["MangaTrackingController"]
        MVM --> MMController["MangaMergeController"]
        MVM --> MAController["MangaArtworkController"]
        
        MCController -->|Manages| ChpFlow["chaptersUiState: StateFlow<ChapterUiState>"]
        MTController -->|Manages| TrackFlow["trackUiState: StateFlow<TrackUiState>"]
        MMController -->|Manages| MergeFlow["mergeUiState: StateFlow<MergeUiState>"]
        MAController -->|Manages| ArtFlow["artworkUiState: StateFlow<ArtworkUiState>"]
        
        MVM -->|Combines via combine()| ScreenState["mangaScreenUiState: StateFlow<MangaScreenUiState>"]
    end
```

---

## 3. Proposed Controller Contracts

### 3.1 MangaChapterController

```kotlin
interface MangaChapterController {
    val chapterUiState: StateFlow<ChapterUiState>
    fun toggleSortOrder()
    fun setScanlatorFilter(scanlators: Set<String>)
    fun markChaptersRead(chapters: List<ChapterItem>, markRead: Boolean)
    fun downloadChapters(chapters: List<ChapterItem>, action: DownloadAction)
    fun deleteDownloadedChapters(chapters: List<ChapterItem>)
}
```

### 3.2 MangaTrackingController

```kotlin
interface MangaTrackingController {
    val trackUiState: StateFlow<TrackUiState>
    fun updateTrackingService(item: TrackItem, service: TrackServiceItem)
    fun searchTracking(serviceId: Long, query: String)
    fun registerTracking(serviceId: Long, searchResult: TrackSearchResult)
    fun removeTracking(serviceId: Long)
}
```

### 3.3 MangaMergeController

```kotlin
interface MangaMergeController {
    val mergeUiState: StateFlow<MergeUiState>
    fun searchMergeManga(mergeType: MergeType, query: String)
    fun attachMergeManga(mergeType: MergeType, mangaUrl: String)
    fun deleteMergeManga(mergeType: MergeType)
    fun refreshMergeChapters()
}
```

### 3.4 MangaArtworkController

```kotlin
interface MangaArtworkController {
    val artworkUiState: StateFlow<ArtworkUiState>
    fun fetchRemoteArtwork()
    fun selectActiveArtwork(artwork: Artwork)
    fun saveArtworkToGallery(artwork: Artwork)
    fun deleteCustomArtwork()
}
```

---

## 4. Lean MangaViewModel Coordinator

With the controllers extracted, `MangaViewModel` delegates feature operations and combines the states:

```kotlin
class MangaViewModel(
    val mangaId: Long,
    private val chapterController: MangaChapterController,
    private val trackingController: MangaTrackingController,
    private val mergeController: MangaMergeController,
    private val artworkController: MangaArtworkController,
    private val mangaRepository: MangaRepository = Injekt.get(),
) : ViewModel() {

    val uiState: StateFlow<MangaScreenUiState> = combine(
        mangaRepository.getMangaFlow(mangaId),
        chapterController.chapterUiState,
        trackingController.trackUiState,
        mergeController.mergeUiState,
        artworkController.artworkUiState,
    ) { manga, chapters, tracking, merge, artwork ->
        MangaScreenUiState(
            manga = manga,
            chapterState = chapters,
            trackState = tracking,
            mergeState = merge,
            artworkState = artwork,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MangaScreenUiState.Initial)

    // Pass-through delegation for UI events
    fun onChapterAction(action: ChapterAction) = chapterController.handleAction(action)
    fun onTrackAction(action: TrackAction) = trackingController.handleAction(action)
    fun onMergeAction(action: MergeAction) = mergeController.handleAction(action)
    fun onArtworkAction(action: ArtworkAction) = artworkController.handleAction(action)
}
```

---

## 5. Technical Footprint & Integration

1. **[`MangaViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt)**:
   - Extract tracking methods (lines 1400–1850) to `MangaTrackingControllerImpl`.
   - Extract merge methods (lines 1100–1399) to `MangaMergeControllerImpl`.
   - Extract artwork methods (lines 900–1099) to `MangaArtworkControllerImpl`.
   - Extract chapter filtering and sorting (lines 450–899) to `MangaChapterControllerImpl`.
2. **Factory & DI**:
   - Provide factory injection for the 4 controllers in `MangaViewModel.Factory`.
3. **Unit Tests**:
   - Create isolated unit tests for `MangaTrackingControllerTest`, `MangaChapterControllerTest`, etc., without needing the massive 24-mock setup.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define interfaces for `MangaTrackingController`, `MangaMergeController`, `MangaArtworkController`, and `MangaChapterController`.
- [ ] **Step 2**: Extract `MangaArtworkController` and verify cover caching / selection.
- [ ] **Step 3**: Extract `MangaTrackingController` and verify tracker sync.
- [ ] **Step 4**: Extract `MangaMergeController` and verify multi-source merging.
- [ ] **Step 5**: Extract `MangaChapterController` and verify filtering/downloading.
- [ ] **Step 6**: Refactor `MangaViewModel` into a lean coordinator combining the 4 flows.
- [ ] **Step 7**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
