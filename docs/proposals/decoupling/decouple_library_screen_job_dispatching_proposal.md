# Technical Proposal: Decoupling LibraryScreen from Background Jobs, Database Entity Mapping & Share Logic

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Compose & Domain Decoupling  
**Implementation State:** 🟡 Coupled Baseline (WorkManager job launches, DB model mappings, and state queries in Compose)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`LibraryScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/LibraryScreen.kt#L95-L168), UI composables directly dispatch system background jobs, convert domain objects into legacy database entities, and query ViewModel state in action callbacks:
> - **Lines 95–99 (`updateLibrary`)**: Invokes system WorkManager job management directly in Compose:
>   ```kotlin
>   if (!LibraryUpdateJob.isRunning(context)) {
>       LibraryUpdateJob.startNow(context)
>   }
>   ```
> - **Lines 104–115 (`shareManga`)**: Queries ViewModel synchronously (`libraryViewModel.getSelectedMangaUrls()`), assembles Android `Intent(Intent.ACTION_SEND)`, and starts activity chooser directly inside the callback.
> - **Lines 152–168 (`categoryRefreshClick`)**:
>   1. Directly calls `LibraryUpdateJob.startNow(context, ...)`.
>   2. Maps domain model to legacy database model: `categoryItem.toDbCategory()`.
>   3. Directly reads `libraryViewModel.libraryScreenState.value.items` inside the callback to filter items and map them to legacy models: `it.toLibraryManga()`.
>
> **What This Proposal Solves:**
> Removes all `LibraryUpdateJob` invocations, legacy database model mappings (`toDbCategory()`, `toLibraryManga()`), and synchronous state-value querying from `LibraryScreen.kt`. The Composable emits pure events (`onUpdateLibrary`, `onRefreshCategory(id)`), while the ViewModel and UseCase manage scheduling and data conversion.

---

## 1. Executive Summary & Vision

Jetpack Compose screens should never instantiate or schedule background workers, touch Android `Context` to dispatch WorkManager tasks, or perform domain-to-database conversions. When a Composable queries `libraryViewModel.libraryScreenState.value` directly in a lambda, it breaks reactive patterns and creates race conditions if state updates concurrently.

### The Objective
Decouple [`LibraryScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/LibraryScreen.kt) by:
1. Extracting a domain `UpdateLibraryUseCase` to encapsulate `LibraryUpdateJob` triggering.
2. Hoisting category refresh logic into `LibraryViewModel.refreshCategory(categoryId: Int)`.
3. Eliminating `categoryItem.toDbCategory()` and `libraryItem.toLibraryManga()` mapping from presentation code.
4. Handling share intent generation through ViewModel one-off events.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Execution
        UI1["LibraryScreen Composable"] -->|toDbCategory()| DB1["Legacy DBCategory"]
        UI1 -->|toLibraryManga()| DB2["Legacy DBLibraryManga"]
        UI1 -->|Direct Call| LUJ1["LibraryUpdateJob.startNow(context)"]
        UI1 -->|Direct Query| ST["libraryViewModel.libraryScreenState.value"]
    end

    subgraph Proposed Decoupled Flow
        UI2["Stateless LibraryScreen"] -->|onRefreshCategory(categoryId)| VM["LibraryViewModel"]
        VM --> UC["UpdateLibraryUseCase"]
        UC --> LUJ2["LibraryUpdateJob (via Android Job Scheduler / WorkManager)"]
        VM -->> UI2["Emits Updated LibraryScreenState via StateFlow"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Domain `UpdateLibraryUseCase`

```kotlin
class UpdateLibraryUseCase(
    private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val mangaRepository: MangaRepository,
) {
    operator fun invoke(categoryId: Int? = null) {
        if (categoryId == null) {
            if (!LibraryUpdateJob.isRunning(context)) {
                LibraryUpdateJob.startNow(context)
            }
            return
        }

        val category = categoryRepository.getCategory(categoryId) ?: return
        val dbCategory = category.toDbCategory()

        if (category.isDynamic) {
            val mangaToUpdate = mangaRepository.getMangaForCategory(categoryId).map { it.toLibraryManga() }
            LibraryUpdateJob.startNow(context, dbCategory, mangaToUpdate)
        } else {
            LibraryUpdateJob.startNow(context, dbCategory)
        }
    }
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Cleaning up `LibraryScreenActions` in `LibraryScreen.kt`

```kotlin
// Before:
categoryRefreshClick = { categoryItem ->
    LibraryUpdateJob.startNow(
        context = context,
        categoryItem.toDbCategory(),
        mangaToUse = if (categoryItem.isDynamic) {
            libraryViewModel.libraryScreenState.value.items...map { it.toLibraryManga() }
        } else null
    )
}

// After:
categoryRefreshClick = { categoryItem ->
    libraryViewModel.refreshCategory(categoryItem.id)
}
```

```kotlin
// Before:
updateLibrary = {
    if (!LibraryUpdateJob.isRunning(context)) {
        LibraryUpdateJob.startNow(context)
    }
}

// After:
updateLibrary = libraryViewModel::updateLibrary
```

```kotlin
// Before:
shareManga = {
    val urls = libraryViewModel.getSelectedMangaUrls()
    val intent = Intent(Intent.ACTION_SEND)...
    context.startActivity(...)
}

// After:
shareManga = libraryViewModel::shareSelectedManga
```

The share intent is received via an event collector in the host Activity/Screen container.

---

## 5. Technical Footprint & Integration

1. **[`LibraryScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/LibraryScreen.kt)**: Remove imports of `LibraryUpdateJob`, `toDbCategory`, and `toLibraryManga`.
2. **[`LibraryViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/library/LibraryViewModel.kt)**: Add `refreshCategory(categoryId: Int)` and `updateLibrary()`.
3. **Domain Layer**: Introduce `UpdateLibraryUseCase` with appropriate dependency injection bindings.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Implement `UpdateLibraryUseCase` and unit tests.
- [ ] **Step 2**: Add `refreshCategory(categoryId)` and `updateLibrary()` to `LibraryViewModel`.
- [ ] **Step 3**: Move share intent creation into `LibraryViewModel` event pipeline.
- [ ] **Step 4**: Remove database model mapping and `LibraryUpdateJob` calls from `LibraryScreen.kt`.
- [ ] **Step 5**: Verify with `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
