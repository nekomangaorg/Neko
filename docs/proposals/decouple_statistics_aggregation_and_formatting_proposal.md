# Technical Proposal: Decoupling Statistics Aggregation & Heavy Dataset Transformations from Jetpack Compose

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Compose & Domain Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Heavy list grouping, $O(M \times C)$ filtering, and number formatting executed on UI thread in Composables)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`SimpleStats.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/stats/SimpleStats.kt) and [`DetailedStats.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/stats/DetailedStats.kt), heavy statistical aggregations and number formatting are executed directly on the main UI thread during Compose recomposition:
> - In **`SimpleStats.kt` (lines 43–100)**:
>   - Executes `remember { ... }` with no keys, causing stale cache bugs if `statsState` changes.
>   - Runs `NumberFormat.getInstance()`, fallback checks (`na`, `never`), string concatenations (`it.first.scanlatorName + " merged"`), and creates a 16-element tuple list inside the Composable.
> - In **`DetailedStats.kt` (lines 215–350)**:
>   - **`CategoryView` (lines 294–318)**: Executes an $O(M \times C)$ nested quadratic scan:
>     ```kotlin
>     detailedStats.categories.associateWith { category ->
>         detailedStats.manga.filter { it.categories.contains(category) }
>     }
>     ```
>     followed by multiple `.sumOf { it.readDuration }` traversals inside `remember(sortType)`.
>   - **`ContentRatingView` (lines 255–266)**: Groups manga collections, calculates entry sums, duration sums, and builds Charty `PieData` inside Compose `remember` blocks.
>   - **`TagView` (lines 217–231)**: Re-sorts and aggregates large chapter and duration lists whenever sort chips are toggled.
>
> **What This Proposal Solves:**
> Offloads all statistical calculations, groupings, sum aggregations, and chart data formatting to background coroutine dispatchers (`Dispatchers.Default`) in `StatsViewModel`. Composables become pure renderers of pre-calculated `SimpleStatMetric` and `ChartUiModel` instances.

---

## 1. Executive Summary & Vision

Jetpack Compose recomposition should never execute heavy CPU-bound algorithms like filtering large collections of manga, grouping by metadata, or calculating durations. Doing so introduces noticeable frame drops and UI stutter, particularly on library collections exceeding thousands of titles.

### The Objective
Decouple [`SimpleStats.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/stats/SimpleStats.kt) and [`DetailedStats.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/stats/DetailedStats.kt) by:
1. Extracting data processing into domain UseCases executing on `Dispatchers.Default`.
2. Calculating all chart data (`PieData`, `LineData`, `StatCardUiModel`) before reaching the UI state.
3. Supplying pre-formatted strings and metrics directly in `SimpleState`.
4. Removing all collection sorting and grouping logic from Composable functions.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Architecture
        UI1["DetailedStats / SimpleStats Composable"] -->|UI Thread remember()| Calc["groupBy, sumOf, O(M*C) associateWith, NumberFormat"]
        Calc --> Render1["Draw Charts & Lists"]
    end

    subgraph Proposed Architecture
        Repo["Manga & History Repositories"] --> VM["StatsViewModel"]
        VM -->|Dispatchers.Default| UC["AggregateDetailedStatsUseCase"]
        UC -->|Computes PieData, LineData, Metrics| Out["Pre-calculated DetailedStatsUiModel"]
        Out -->|StateFlow| Screen["StatsScreen"]
        Screen --> UI2["Stateless DetailedStats\n(Zero aggregations, instant render)"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Pre-Computed UI Models

```kotlin
@Immutable
data class SimpleStatMetricUiModel(
    val title: UiText,
    val formattedValue: String,
)

@Immutable
data class DetailedDistributionUiModel(
    val categoryDistributions: List<ChartDistributionItem>,
    val contentRatingDistributions: List<ChartDistributionItem>,
    val tagDistributions: List<ChartDistributionItem>,
    val startYearLineData: List<LineData>,
)

@Immutable
data class ChartDistributionItem(
    val label: String,
    val count: Int,
    val formattedDuration: String,
    val pieSlice: PieData,
)
```

### 3.2 Extracted Aggregation UseCase

```kotlin
class AggregateDetailedStatsUseCase {
    suspend operator fun invoke(
        mangaList: List<MangaStatItem>,
        categories: List<String>,
        sortType: Sort,
    ): DetailedDistributionUiModel = withContext(Dispatchers.Default) {
        // Fast indexing: Map manga to categories using inverted index in O(M + C) instead of O(M * C)
        val categoryMap = mutableMapOf<String, MutableList<MangaStatItem>>()
        categories.forEach { categoryMap[it] = mutableListOf() }
        
        for (manga in mangaList) {
            for (cat in manga.categories) {
                categoryMap[cat]?.add(manga)
            }
        }
        
        // Assemble pre-sorted items and chart models off the main thread...
        ...
    }
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Cleaning up `SimpleStats.kt`

The Composable no longer creates or formats strings:

```kotlin
@Composable
fun SimpleStats(
    metrics: List<SimpleStatMetricUiModel>,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
) {
    LazyColumn(contentPadding = contentPadding) {
        items(metrics, key = { it.title.hashCode() }) { metric ->
            StatCard(title = metric.title.asString(), value = metric.formattedValue)
        }
    }
}
```

### 4.2 Cleaning up `DetailedStats.kt`

`CategoryView`, `ContentRatingView`, and `TagView` drop all `remember(sortType)` grouping and summing blocks. They directly feed pre-calculated `ChartDistributionItem` lists into chart composables.

---

## 5. Technical Footprint & Integration

1. **[`SimpleStats.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/stats/SimpleStats.kt)**: Remove lines 43–100; replace with plain metric model list.
2. **[`DetailedStats.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/stats/DetailedStats.kt)**: Remove lines 215–350; eliminate in-composable calculations.
3. **[`StatsViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/stats/StatsViewModel.kt)**: Inject `AggregateDetailedStatsUseCase` and perform transformations on `Dispatchers.Default`.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Implement `AggregateDetailedStatsUseCase` with optimized index mapping and unit tests.
- [ ] **Step 2**: Update `StatsViewModel` to output pre-computed `SimpleStatMetricUiModel` and `DetailedDistributionUiModel`.
- [ ] **Step 3**: Refactor `SimpleStats.kt` to consume the metric list and add `@Preview`.
- [ ] **Step 4**: Refactor `DetailedStats.kt` to eliminate UI thread aggregations.
- [ ] **Step 5**: Run `./gradlew ktfmtFormat` and verify silky-smooth 120Hz chart rendering.
