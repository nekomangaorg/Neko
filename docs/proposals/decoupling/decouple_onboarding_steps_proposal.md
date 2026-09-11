# Technical Proposal: Decoupling Onboarding Steps from Service Locators, Direct Preferences & Activity Lifecycle

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Architectural Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Service locator calls, internal mutable state, direct preference writes, and Activity recreation inside UI step objects)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`StorageStep.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/onboarding/StorageStep.kt#L31-L98) and [`ThemeStep.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/onboarding/ThemeStep.kt#L24-L97):
> - **Service Locator Inside UI Components**:
>   - In [`StorageStep.kt:L33`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/onboarding/StorageStep.kt#L33):  
>     `private val storagePref = Injekt.get<StoragePreferences>().baseStorageDirectory()`
>   - In [`ThemeStep.kt:L27`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/onboarding/ThemeStep.kt#L27):  
>     `private val preferences: PreferencesHelper = Injekt.get()`
> - **Internal Mutable State & File Picker Launching**:
>   - `StorageStep` maintains mutable state inside the class instance (`private var _isComplete by mutableStateOf(false)`) and executes background collection directly via `LaunchedEffect`:
>     ```kotlin
>     LaunchedEffect(Unit) {
>         storagePref.changes().collectLatest { _isComplete = storagePref.isSet() }
>     }
>     ```
> - **Direct Preferences Mutation & Activity Recreation**:
>   - In [`ThemeStep.kt:L78-L96`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/onboarding/ThemeStep.kt#L78-L96), UI button clicks directly mutate `preferences.darkTheme().set(theme)`, `preferences.nightMode().set(nightMode)`, and invoke Activity recreation directly:
>     ```kotlin
>     (context as? Activity)?.let { activity -> ActivityCompat.recreate(activity) }
>     ```
>
> **What This Proposal Solves:**
> Removes all `Injekt.get()` calls, internal mutable states, direct preference mutations, and inline `ActivityCompat.recreate()` calls from `StorageStep` and `ThemeStep`. Consolidates state and actions into an `OnboardingViewModel`, enabling stateless `@Preview`s and robust unit testing.

---

## 1. Executive Summary & Vision

Onboarding steps are user-facing presentation screens that guide new users through setup. Coupling individual step classes directly to `Injekt`, `PreferencesHelper`, `StoragePreferences`, and `ActivityCompat` breaks separation of concerns and prevents step previews in isolation.

### The Objective
Decouple [`StorageStep.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/onboarding/StorageStep.kt) and [`ThemeStep.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/onboarding/ThemeStep.kt) by:
1. Removing all `Injekt.get()` dependencies from step classes.
2. Hoisting storage validation and completion status to `OnboardingViewModel`.
3. Hoisting theme selection mutations and activity recreation triggers to the ViewModel / host container.
4. Defining immutable UI models (`StorageStepUiState`, `ThemeStepUiState`) and action callbacks.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Flow
        Step1["StorageStep"] -->|Injekt.get()| SP["StoragePreferences"]
        Step1 -->|Internal state| MutState["_isComplete mutableStateOf"]
        Step2["ThemeStep"] -->|Injekt.get()| Prefs["PreferencesHelper"]
        Step2 -->|Mutates directly| MutPref["preferences.nightMode().set()"]
        Step2 -->|Direct Activity call| Recreate["ActivityCompat.recreate(activity)"]
    end

    subgraph Proposed Decoupled Architecture
        OVM["OnboardingViewModel"] -->|Collects Prefs| State["OnboardingUiState (Storage & Theme States)"]
        State --> Host["OnboardingScreen (Host)"]
        Host -->|Passes StorageStepUiState| SStep["Stateless StorageStep"]
        Host -->|Passes ThemeStepUiState| TStep["Stateless ThemeStep"]
        SStep -->|onSelectStorageClicked| Host
        TStep -->|onThemeSelected(theme, isDark)| OVM
        OVM -->|Dispatches Single Event| RecreateEvent["SingleLiveEvent.RecreateActivity"]
        RecreateEvent --> Host
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable Step UI States

```kotlin
@Immutable
data class StorageStepUiState(
    val currentStoragePath: String,
    val isStorageConfigured: Boolean,
)

@Immutable
data class ThemeStepUiState(
    val lightTheme: Themes,
    val darkTheme: Themes,
    val nightMode: Int,
    val followSystemTheme: Boolean,
)
```

### 3.2 Sealed Onboarding Actions

```kotlin
sealed interface OnboardingAction {
    data object RequestStoragePicker : OnboardingAction
    data class SelectTheme(val theme: Themes, val isDark: Boolean) : OnboardingAction
    data class SetNightMode(val nightMode: Int) : OnboardingAction
    data class SetFollowSystemTheme(val follow: Boolean) : OnboardingAction
    data object OpenStorageHelpLink : OnboardingAction
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless StorageStep Composable

```kotlin
@Composable
fun StorageStepContent(
    uiState: StorageStepUiState,
    onSelectStorage: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Size.medium).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Size.small),
    ) {
        Text(
            text = stringResource(
                R.string.onboarding_storage_info,
                stringResource(R.string.app_name),
                uiState.currentStoragePath,
            )
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSelectStorage,
        ) {
            Text(stringResource(R.string.onboarding_storage_action_select))
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = Size.small),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        Text(
            text = stringResource(
                R.string.onboarding_storage_help_info,
                stringResource(R.string.app_name),
            )
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenHelp,
        ) {
            Text(stringResource(R.string.onboarding_storage_help_action))
        }
    }
}
```

### 4.2 Stateless ThemeStep Composable

```kotlin
@Composable
fun ThemeStepContent(
    uiState: ThemeStepUiState,
    onThemeSelected: (Themes, Boolean) -> Unit,
    onFollowSystemToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(Size.medium)) {
        ThemeSelector(
            currentTheme = if (uiState.followSystemTheme) uiState.lightTheme else uiState.lightTheme,
            onThemeChange = { onThemeSelected(it, false) },
            darkThemeSelector = false,
        )
        Gap(Size.small)
        ThemeSelector(
            currentTheme = uiState.darkTheme,
            onThemeChange = { onThemeSelected(it, true) },
            darkThemeSelector = true,
        )
        ThemeFollowSystemSwitch(
            modifier = Modifier.padding(vertical = Size.small),
            nightMode = uiState.nightMode,
            onToggle = onFollowSystemToggle,
        )
    }
}
```

---

## 5. Technical Footprint & Integration

1. **[`StorageStep.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/onboarding/StorageStep.kt)**:
   - Remove `Injekt.get<StoragePreferences>()` and `_isComplete by mutableStateOf(false)`.
   - Extract UI into stateless `StorageStepContent`.
2. **[`ThemeStep.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/onboarding/ThemeStep.kt)**:
   - Remove `Injekt.get<PreferencesHelper>()`.
   - Remove `ActivityCompat.recreate(activity)` from UI code; emit recreation event through ViewModel channel.
   - Extract UI into stateless `ThemeStepContent`.
3. **`OnboardingViewModel.kt`**:
   - Manage `StorageStepUiState` and `ThemeStepUiState` flows.
   - Coordinate storage permission / picker callbacks and theme update persistence.
4. **New Previews**: Add `@Preview` for `StorageStepContent` and `ThemeStepContent` with mock light/dark themes.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `StorageStepUiState` and `ThemeStepUiState` models.
- [ ] **Step 2**: Add state flows and theme update handlers to `OnboardingViewModel`.
- [ ] **Step 3**: Refactor `StorageStep.kt` to be stateless and remove `Injekt.get()`.
- [ ] **Step 4**: Refactor `ThemeStep.kt` to be stateless, removing `Injekt` and direct `ActivityCompat.recreate()`.
- [ ] **Step 5**: Add Compose previews for each onboarding step.
- [ ] **Step 6**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
