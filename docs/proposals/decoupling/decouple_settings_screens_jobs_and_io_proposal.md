# Technical Proposal: Decoupling Settings Screens from CoroutineScopes, Background Jobs & Disk I/O

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Compose & Domain Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Passing ViewModel CoroutineScopes, Injekt calls, and Disk I/O inside Settings UI)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In Settings screens, several architectural boundary violations couple Compose UI components with platform coroutines, disk I/O, and background worker jobs:
> - In **[`LibrarySettingsScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/settings/screens/LibrarySettingsScreen.kt)**:
>   - Line 43: Passes a ViewModel's `CoroutineScope` directly into the UI class constructor: `val viewModelScope: CoroutineScope`.
>   - Lines 210–232: Launches non-cancellable coroutines and orchestrates WorkManager tasks inside Composable preference change callbacks:
>     ```kotlin
>     viewModelScope.launchNonCancellable {
>         val interval = libraryPreferences.updateInterval().get()
>         LibraryUpdateJob.setupTask(context, interval)
>     }
>     ```
> - In **[`DataStorageSettingsScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/settings/screens/DataStorageSettingsScreen.kt)**:
>   - Line 162: Calls Service Locator directly inside a Composable: `val getDateFormatUseCase = remember { Injekt.get<GetDateFormatUseCase>() }`.
>   - Lines 90–120: Executes synchronous storage disk scans and storage space formatting via `DiskUtil.getExternalStorages(context)` and `DiskUtil.getAvailableStorageSpace(file)` on the main UI thread.
>   - Lines 187 & 218: Dispatches `BackupCreatorJob.startNow` and `BackupRestoreJob.start` directly from UI result launchers.
> - In **[`AdvancedSettingsScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/settings/screens/AdvancedSettingsScreen.kt)**:
>   - Invokes `CrashLogUtil(context).dumpLogs()` and Firebase Crashlytics platform singletons directly in UI preference listeners.
>
> **What This Proposal Solves:**
> Strips `viewModelScope`, `Injekt.get()`, and `DiskUtil` synchronous I/O from Settings composables. All background scheduling, disk calculations, and backup job dispatches are moved to their respective ViewModels.

---

## 1. Executive Summary & Vision

UI screens in Jetpack Compose should be declarative descriptions of settings items, values, and action events. Passing a `CoroutineScope` into a UI class violates inversion of control and allows the UI to launch unconstrained background work. Similarly, querying disk metrics or running `Injekt.get()` inside Compose impairs previewability and blocks the main thread.

### The Objective
Decouple Settings screens by:
1. Eliminating `viewModelScope: CoroutineScope` constructor injection in [`LibrarySettingsScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/settings/screens/LibrarySettingsScreen.kt).
2. Moving `LibraryUpdateJob.setupTask()` calls into `LibrarySettingsViewModel`.
3. Offloading `DiskUtil` storage space computations to `DataStorageSettingsViewModel` using `Dispatchers.IO`.
4. Removing `Injekt.get<GetDateFormatUseCase>()` from Composable functions and passing formatted timestamps via UI state.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Architecture
        UI1["LibrarySettingsScreen"] -->|Holds Reference to| CS["viewModelScope: CoroutineScope"]
        UI1 -->|Executes in UI| LUJ["LibraryUpdateJob.setupTask()"]
        UI2["DataStorageSettingsScreen"] -->|Injekt.get()| DI["GetDateFormatUseCase"]
        UI2 -->|Main Thread I/O| DU["DiskUtil.getAvailableStorageSpace()"]
    end

    subgraph Proposed Decoupled Architecture
        UI3["Stateless LibrarySettingsScreen"] -->|onUpdateIntervalChanged(interval)| LVM["LibrarySettingsViewModel"]
        LVM -->|Internal Coroutine| LUJ2["LibraryUpdateJob.setupTask()"]

        DVM["DataStorageSettingsViewModel"] -->|Dispatchers.IO| DU2["DiskUtil Scan"]
        DVM -->|Emits StateFlow| UI4["Stateless DataStorageSettingsScreen\n(storageUsage, onBackup, onRestore)"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Storage Usage UI Model

```kotlin
@Immutable
data class StorageUsageUiModel(
    val path: String,
    val availableSpaceText: String,
    val totalSpaceText: String,
    val usedRatio: Float,
)

@Immutable
data class DataStorageUiState(
    val storages: List<StorageUsageUiModel> = emptyList(),
    val lastBackupText: String = "",
    val isBackupRunning: Boolean = false,
)
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Cleaning `LibrarySettingsScreen.kt`

Remove `viewModelScope` entirely from the constructor:

```kotlin
// Before:
internal class LibrarySettingsScreen(
    incognitoMode: Boolean,
    val libraryPreferences: LibraryPreferences,
    onNavigationIconClick: (() -> Unit)?,
    val categories: List<CategoryItem>,
    val viewModelScope: CoroutineScope,
    val onAddEditCategoryClick: () -> Unit,
)

// After:
internal class LibrarySettingsScreen(
    incognitoMode: Boolean,
    val libraryPreferences: LibraryPreferences,
    onNavigationIconClick: (() -> Unit)?,
    val categories: List<CategoryItem>,
    val onUpdateIntervalChanged: (Int) -> Unit,
    val onDeviceRestrictionsChanged: (Set<String>) -> Unit,
    val onAddEditCategoryClick: () -> Unit,
)
```

Inside preference change callbacks:
```kotlin
onValueChanged = { interval ->
    onUpdateIntervalChanged(interval)
    true
}
```

### 4.2 Cleaning `DataStorageSettingsScreen.kt`

Remove `Injekt.get()` and `DiskUtil` calls:
```kotlin
// In DataStorageSettingsViewModel:
val storageState: StateFlow<DataStorageUiState> = flow {
    emit(loadStorageMetrics())
}.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataStorageUiState())
```

The Composable simply renders `storageState.storages`.

---

## 5. Technical Footprint & Integration

1. **[`LibrarySettingsScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/settings/screens/LibrarySettingsScreen.kt)**: Remove `viewModelScope`; expose action callbacks.
2. **[`SettingsScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/SettingsScreen.kt)**: Update entry instantiation to wire callbacks to `vm::setUpdateInterval`, etc.
3. **[`DataStorageSettingsScreen.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/settings/screens/DataStorageSettingsScreen.kt)**: Remove `Injekt.get()` and disk scans.
4. **[`DataStorageSettingsViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/setting/DataStorageSettingsViewModel.kt)**: Compute storage metrics and manage backup job execution.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Remove `viewModelScope` parameter from `LibrarySettingsScreen` and route job setup to `LibrarySettingsViewModel`.
- [ ] **Step 2**: Add background disk metric calculations to `DataStorageSettingsViewModel`.
- [ ] **Step 3**: Remove `Injekt.get<GetDateFormatUseCase>()` from `DataStorageSettingsScreen.kt`.
- [ ] **Step 4**: Encapsulate `BackupCreatorJob` and `BackupRestoreJob` calls in `DataStorageSettingsViewModel`.
- [ ] **Step 5**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
