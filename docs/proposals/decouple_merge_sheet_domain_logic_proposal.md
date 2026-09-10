# Technical Proposal: Decoupling MergeSheet from Domain Source Resolvers & Business Rules

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Compose & Domain Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Injekt calls and source resolution logic present inside Composables)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`MergeSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/MergeSheet.kt), the sheet composable directly accesses the dependency injection container:
> - Line 140: `MergeType.getSource(mergedManga.mergeType, Injekt.get())` in `MergedItem` `onOpenWebView`.
> - Line 207: `MergeType.getSource(validMergeType, Injekt.get())` in `MergeLogo` `onLongClick`.
> - Line 247: `MergeType.getSource(selectedMergeType, Injekt.get())` in `SuccessResults` `mergeMangaLongClick`.
>
> Furthermore, [`MergeSelectionSheet`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/MergeSheet.kt#L159-L230) performs domain business logic directly in Compose:
> - Computes active merge IDs and filters available merge types based on the `multiMerge` business rule:
>   ```kotlin
>   val mergedTypeIds = isMergedManga.mergedMangaList.map { it.mergeType.id }.toSet()
>   validMergeTypes.filter { mergeType -> mergeType.multiMerge || !mergedTypeIds.contains(mergeType.id) }
>   ```
> - Filters existing URLs from `isMergedManga.mergedMangaList` to pass into search queries.
>
> **What This Proposal Solves:**
> Removes all Service Locator (`Injekt.get()`) invocations, URL resolution logic, and merge eligibility filtering from the UI composables. The sheet becomes 100% stateless and accepts pre-computed UI models, delegating all domain logic and source resolution to a dedicated UseCase and ViewModel.

---

## 1. Executive Summary & Vision

Jetpack Compose composables should be pure projections of UI state and should never depend on platform service locators or perform business filtering. When a Composable invokes `Injekt.get<SourceManager>()`, it breaks previewability in Android Studio (`@Preview`), hampers unit testability, and tightly couples the visual representation to specific data source implementations.

### The Objective
Decouple [`MergeSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/MergeSheet.kt) from domain dependencies by:
1. Eliminating all direct `Injekt.get()` calls inside Compose lambdas.
2. Hoisting merge availability filtering (`multiMerge` rules) into a pure UseCase.
3. Supplying pre-resolved URLs or high-level event callbacks (`onOpenSourceUrl(url, title)`) rather than resolving source instances on the UI thread.
4. Enabling standalone `@Preview` support for merge selection states.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Architecture [Current Tightly Coupled Flow]
        UI1["MergeSheet Composable"] -->|Injekt.get()| SM["SourceManager / Injekt"]
        UI1 -->|MergeType.getSource()| SR["MergedServerSource"]
        UI1 -->|Filters multiMerge| BL["In-Composable Business Rules"]
    end

    subgraph Proposed Decoupled Architecture [Clean Unidirectional Data Flow]
        VM["MangaViewModel / MergeViewModel"] -->|Invokes| UC["GetAvailableMergeSourcesUseCase"]
        UC -->|Returns| MS["Immutable MergeSheetUiState"]
        MS -->|Emits State| UI2["Stateless MergeSheet Composable"]
        UI2 -->|onSelectSource(type)| VM
        UI2 -->|onOpenWebView(url, title)| Nav["Navigation / Custom Tabs"]
    end
```

---

## 3. Proposed UI Models & Domain Contracts

### 3.1 Pure UI State Models

```kotlin
@Immutable
data class MergeSheetUiState(
    val isMerged: Boolean = false,
    val mergedItems: List<MergedItemUiModel> = emptyList(),
    val availableSources: List<MergeSourceUiModel> = emptyList(),
    val searchResults: MergeSearchResult = MergeSearchResult.Initial,
    val selectedSource: MergeSourceUiModel? = null,
)

@Immutable
data class MergedItemUiModel(
    val id: Long,
    val title: String,
    val sourceName: String,
    val coverUrl: String,
    val webUrl: String,
    val mergeType: MergeType,
)

@Immutable
data class MergeSourceUiModel(
    val mergeType: MergeType,
    val name: String,
    val iconRes: Int,
    val homeUrl: String,
    val isAvailable: Boolean = true,
)
```

### 3.2 Extracted UseCase

```kotlin
class GetAvailableMergeSourcesUseCase(
    private val sourceManager: SourceManager,
) {
    operator fun invoke(
        validMergeTypes: List<MergeType>,
        currentlyMergedTypes: Set<Int>,
    ): List<MergeSourceUiModel> {
        return validMergeTypes.map { mergeType ->
            val source = MergeType.getSource(mergeType, sourceManager)
            val homeUrl = if (mergeType.baseUrl.isNotEmpty()) {
                mergeType.baseUrl
            } else if (source is MergedServerSource) {
                source.hostUrl()
            } else {
                ""
            }
            MergeSourceUiModel(
                mergeType = mergeType,
                name = mergeType.name,
                iconRes = mergeType.toDrawableRes(),
                homeUrl = homeUrl,
                isAvailable = mergeType.multiMerge || !currentlyMergedTypes.contains(mergeType.id),
            )
        }
    }
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless Composable Signature

Refactor [`MergeSheet`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/MergeSheet.kt) to accept resolved models and plain event callbacks:

```kotlin
@Composable
fun MergeSheet(
    themeColorState: ThemeColorState,
    uiState: MergeSheetUiState,
    onSelectSource: (MergeType) -> Unit,
    onRemoveMerge: (MergedItemUiModel) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectMergeCandidate: (SourceMergeManga) -> Unit,
    onOpenUrl: (url: String, title: String) -> Unit,
    onBack: () -> Unit,
)
```

### 4.2 Removing UI Calculations
- In `MergedItem`, the `onOpenWebView` lambda receives the pre-calculated `mergedItem.webUrl` directly:
  ```kotlin
  onOpenWebView = { onOpenUrl(mergedManga.webUrl, mergedManga.title) }
  ```
- In `MergeLogo`, `onLongClick` accesses `source.homeUrl` directly from `MergeSourceUiModel` without inspecting or casting `MergedServerSource`.

---

## 5. Technical Footprint & Integration

1. **[`MergeSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/MergeSheet.kt)**: Remove all `Injekt.get()` and business filtering.
2. **[`MangaConstants.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaConstants.kt)**: Update `MangaScreenMergeState` to wrap pre-computed UI models.
3. **[`MangaViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt)**: Inject `GetAvailableMergeSourcesUseCase` and compute `MergeSheetUiState`.
4. **New Preview**: Add `MergeSheetPreview` providing mock states for zero, single, and multiple merged sources.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `MergeSheetUiState`, `MergedItemUiModel`, and `MergeSourceUiModel` in `org.nekomanga.presentation.components.sheets`.
- [ ] **Step 2**: Create and unit-test `GetAvailableMergeSourcesUseCase`.
- [ ] **Step 3**: Update `MangaViewModel` to compute `MergeSourceUiModel` with resolved web URLs.
- [ ] **Step 4**: Refactor `MergeSheet.kt` and `MergeSelectionSheet` to consume UI state and remove `Injekt`.
- [ ] **Step 5**: Add `@Preview` annotations for `MergeSheet` variants.
- [ ] **Step 6**: Verify with `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
