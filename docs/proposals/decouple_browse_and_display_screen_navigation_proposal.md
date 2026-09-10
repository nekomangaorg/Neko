# Technical Proposal: Decoupling Browse & Display Screens from Database Entities and In-UI Navigation Resolvers

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Compose & Domain Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Database models in UI state, in-composable navigation logic, and StateFlow prop-drilling)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`BrowseScreenState.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/browse/BrowseScreenState.kt) and [`DisplayScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/DisplayScreen.kt), database entities leak into Compose UI state, and navigation routing logic is executed directly inside composables:
> - In **`BrowseScreenState.kt` (lines 41 & 77)**:
>   - Directly imports and exposes Room database entity `BrowseFilterImpl`:
>     `val savedFilters: List<BrowseFilterImpl> = listOf()`, `val loadFilter: (BrowseFilterImpl) -> Unit`.
> - In **`DisplayScreen.kt` (lines 106–121)**:
>   - The composable inspects internal ViewModel properties and executes conditional type transformation logic to determine the next screen:
>     ```kotlin
>     resultItemClick = { uuid: String ->
>         val result =
>             if (viewModel.displayScreenType is DisplayScreenType.AuthorByName) {
>                 DisplayScreenType.AuthorWithUuid(title = viewModel.displayScreenType.title, uuid)
>             } else if (viewModel.displayScreenType is DisplayScreenType.GroupByName) {
>                 DisplayScreenType.GroupByUuid(title = viewModel.displayScreenType.title, uuid)
>             } else null
>         if (result != null) {
>             onNavigateTo(Screens.Display(result.toSerializable()))
>         }
>     }
>     ```
> - In **[`BrowseScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/BrowseScreen.kt#L141-L163)**:
>   - `BrowseWrapper` takes `browseScreenFlow: StateFlow<BrowseScreenState>` and calls `collectAsState()` without lifecycle boundaries.
>
> **What This Proposal Solves:**
> Decouples `BrowseScreenState` from database entities by mapping them to `SavedFilterUiModel`, removes ViewModel property inspection and navigation branching from `DisplayScreen.kt`, and hoists flow collection using `collectAsStateWithLifecycle()`.

---

## 1. Executive Summary & Vision

Leaking Room database entity implementations (such as `BrowseFilterImpl`) into Compose UI models breaks clean architectural boundaries and prevents database schema changes without breaking the UI. Similarly, having a Composable check `if (viewModel.displayScreenType is ...)` and manually instantiate new navigation arguments tightly binds the UI layout to the navigation routing logic.

### The Objective
Decouple Browse and Display screens by:
1. Replacing `BrowseFilterImpl` in `BrowseScreenState` with a clean `SavedFilterUiModel`.
2. Moving result item navigation resolution (`AuthorByName -> AuthorWithUuid`) into `DisplayViewModel`.
3. Emitting navigation events from `DisplayViewModel` via a one-off event channel.
4. Passing evaluated UI state to `BrowseWrapper` using `collectAsStateWithLifecycle()`.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Architecture
        State["BrowseScreenState"] -->|Exposes DB Entity| DB["BrowseFilterImpl (Room Entity)"]
        UI["DisplayScreen Composable"] -->|Inspects| Prop["viewModel.displayScreenType"]
        UI -->|Type Casting & Serialization| Nav["Screens.Display(toSerializable())"]
    end

    subgraph Proposed Decoupled Flow
        Repo["FilterRepository"] --> VM1["BrowseViewModel"]
        VM1 -->|Maps to| UIModel["SavedFilterUiModel (Pure Immutable Model)"]
        UIModel --> BScreen["Stateless BrowseScreen"]

        DScreen["DisplayScreen"] -->|resultItemClick(uuid)| VM2["DisplayViewModel"]
        VM2 -->|Resolves Navigation Route| Event["Emit NavigationEvent.ToDisplay(serializable)"]
        Event --> Host["Navigation Observer / Host"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Clean UI Model for Filters

```kotlin
@Immutable
data class SavedFilterUiModel(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
)
```

In `BrowseScreenState.kt`:
```kotlin
@Immutable
data class BrowseScreenState(
    ...
    val savedFilters: List<SavedFilterUiModel> = listOf(),
    ...
)

data class FilterActions(
    val filterClick: () -> Unit,
    val saveFilterClick: (String) -> Unit,
    val deleteFilterClick: (Long) -> Unit,
    val filterDefaultClick: (Long, Boolean) -> Unit,
    val loadFilter: (Long) -> Unit,
    val resetClick: () -> Unit,
    val filterChanged: (Filter) -> Unit,
)
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Cleaning up `DisplayScreen.kt`

The Composable no longer checks `viewModel.displayScreenType`:

```kotlin
// Before:
resultItemClick = { uuid ->
    val result = if (viewModel.displayScreenType is DisplayScreenType.AuthorByName) ...
    if (result != null) onNavigateTo(Screens.Display(result.toSerializable()))
}

// After:
resultItemClick = viewModel::onResultItemClick
```

In `DisplayViewModel`:
```kotlin
fun onResultItemClick(uuid: String) {
    val nextScreen = when (val type = displayScreenType) {
        is DisplayScreenType.AuthorByName -> DisplayScreenType.AuthorWithUuid(type.title, uuid)
        is DisplayScreenType.GroupByName -> DisplayScreenType.GroupByUuid(type.title, uuid)
        else -> null
    }
    if (nextScreen != null) {
        viewModelScope.launch {
            _navigationEvent.emit(DisplayNavigationEvent.NavigateToDisplay(nextScreen.toSerializable()))
        }
    }
}
```

The Composable collects this event via `ObserveAsEvents` or `LaunchedEffect`:
```kotlin
ObserveAsEvents(viewModel.navigationEvent) { event ->
    when (event) {
        is DisplayNavigationEvent.NavigateToDisplay -> onNavigateTo(Screens.Display(event.serializable))
    }
}
```

### 4.2 Decoupling `BrowseWrapper`
- Collect `browseScreenState` at top-level `BrowseScreen` using `collectAsStateWithLifecycle()`.
- Pass evaluated `browseScreenState` into `BrowseWrapper`.

---

## 5. Technical Footprint & Integration

1. **[`BrowseScreenState.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/browse/BrowseScreenState.kt)**: Replace `BrowseFilterImpl` with `SavedFilterUiModel`.
2. **[`DisplayScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/DisplayScreen.kt)**: Remove lines 106–121; delegate to ViewModel event channel.
3. **[`DisplayViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/source/latest/DisplayViewModel.kt)**: Add `onResultItemClick(uuid: String)` and navigation event flow.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Create `SavedFilterUiModel` and update `BrowseScreenState` and `FilterActions`.
- [ ] **Step 2**: Update `BrowseViewModel` to map Room entities to `SavedFilterUiModel`.
- [ ] **Step 3**: Move navigation destination resolution from `DisplayScreen.kt` to `DisplayViewModel`.
- [ ] **Step 4**: Refactor `BrowseScreen.kt` to hoist state collection with `collectAsStateWithLifecycle()`.
- [ ] **Step 5**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
