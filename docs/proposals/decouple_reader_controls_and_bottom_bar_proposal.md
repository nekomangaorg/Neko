# Technical Proposal: Decoupling ReaderControls and Bottom Action Bar from Parameter Overload & State Sprawl

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling  
**Implementation State:** 🟡 Coupled Baseline (41 separate parameters in Composable signature, state sprawl)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`ReaderControls.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderControls.kt#L120-L270), `ReaderBottomControls` represents one of the largest parameter-bloated composables in the repository:
> - **Lines 121–162**: Takes **41 separate parameters**:
>   - 11 boolean visibility flags: `pageNumberVisible`, `isChaptersVisible`, `isCommentsVisible`, `isWebViewVisible`, `isReadingModeVisible`, `isRotationVisible`, `isCropBordersVisible`, `isGrayscaleVisible`, `isDoublePageVisible`, `isShiftPageVisible`, `isSettingsVisible`.
>   - 4 icon resource IDs: `readingModeIconRes`, `rotationIconRes`, `doublePageIconRes`, `shiftPageIconRes`.
>   - 2 toggle states: `cropBorders`, `grayscale`.
>   - 6 pagination properties: `currentPageText`, `totalPagesText`, `currentPageIndex`, `totalPages`, `isRtl`, `isVertical`.
>   - 10 individual click lambdas: `onChaptersClick`, `onCommentsClick`, `onWebviewClick`, `onReadingModeClick`, `onRotationClick`, `onCropBordersClick`, `onGrayscaleClick`, `onDoublePageClick`, `onShiftPageClick`, `onSettingsClick`.
> - The composable directly branches layout trees and animations based on raw enum comparisons:
>   `val showVerticalSlider = isVertical && sliderPosition != ReaderSliderPosition.HORIZONTAL`.
>
> **What This Proposal Solves:**
> Collapses the 41 parameters into a unified, immutable `ReaderBottomControlsUiState` and a sealed `ReaderBottomBarAction` event interface. Decouples slider presentation from action bar rendering and creates previewable, modular composables.

---

## 1. Executive Summary & Vision

A 41-parameter Composable signature is an extreme code smell in Jetpack Compose. It leads to accidental parameter swapping bugs, excessive boilerplate across call sites in `ReaderActivity`, and makes maintaining, previewing, or animating toolbar controls tedious and error-prone.

### The Objective
Decouple [`ReaderControls.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderControls.kt) by:
1. Encapsulating all bottom control states into an immutable `ReaderBottomControlsUiState`.
2. Grouping all toolbar buttons into a polymorphic list of `ReaderToolbarButtonUiModel`.
3. Routing all user interactions through a sealed `ReaderBottomBarAction` callback.
4. Isolating slider logic into an independent `ReaderSliderUiState`.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Architecture
        RVM1["ReaderViewModel / ReaderActivity"] -->|41 Individual Arguments| RBC1["ReaderBottomControls\n(11 Booleans, 4 Icons, 10 Lambdas)"]
        RBC1 --> Slid1["Horizontal / Vertical Floating Slider"]
        RBC1 --> Act1["BottomActionSheet (10 Buttons)"]
    end

    subgraph Proposed Decoupled Flow
        RVM2["ReaderViewModel"] --> State["ReaderBottomControlsUiState\n(sliderState, toolbarButtons, isVisible)"]
        State --> RBC2["Stateless ReaderBottomControls\n(uiState, onAction)"]
        RBC2 --> Slid2["Stateless FloatingSlider"]
        RBC2 --> Act2["Stateless ToolbarActionRow"]
        RBC2 -->|onAction(ReaderBottomBarAction)| RVM2
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable Controls State Model

```kotlin
@Immutable
data class ReaderBottomControlsUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val sliderState: ReaderSliderUiState,
    val buttons: List<ReaderToolbarButtonUiModel>,
)

@Immutable
data class ReaderSliderUiState(
    val currentPageText: String,
    val totalPagesText: String,
    val currentPageIndex: Int,
    val totalPages: Int,
    val isRtl: Boolean,
    val position: SliderOrientation,
)

enum class SliderOrientation {
    Horizontal,
    VerticalLeft,
    VerticalRight,
}

@Immutable
data class ReaderToolbarButtonUiModel(
    val id: ReaderBottomActionId,
    val iconRes: Int,
    val tooltipRes: Int,
    val isToggled: Boolean = false,
    val isVisible: Boolean = true,
)

enum class ReaderBottomActionId {
    Chapters,
    Comments,
    WebView,
    ReadingMode,
    Rotation,
    CropBorders,
    Grayscale,
    DoublePage,
    ShiftPage,
    Settings,
}
```

### 3.2 Sealed Bottom Bar Actions

```kotlin
sealed interface ReaderBottomBarAction {
    data class PageChanged(val pageIndex: Int) : ReaderBottomBarAction
    object SkipPrevious : ReaderBottomBarAction
    object SkipNext : ReaderBottomBarAction
    data class ButtonClicked(val actionId: ReaderBottomActionId) : ReaderBottomBarAction
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless Composable Signature

Refactor [`ReaderBottomControls`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderControls.kt) to accept a single UI model and an action callback:

```kotlin
@Composable
fun ReaderBottomControls(
    uiState: ReaderBottomControlsUiState,
    onAction: (ReaderBottomBarAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (uiState.sliderState.position) {
            SliderOrientation.VerticalLeft, SliderOrientation.VerticalRight -> {
                VerticalFloatingSlider(
                    state = uiState.sliderState,
                    isLoading = uiState.isLoading,
                    onPageChange = { onAction(ReaderBottomBarAction.PageChanged(it)) },
                    onSkipPrevious = { onAction(ReaderBottomBarAction.SkipPrevious) },
                    onSkipNext = { onAction(ReaderBottomBarAction.SkipNext) },
                )
            }
            SliderOrientation.Horizontal -> {
                HorizontalFloatingSlider(
                    state = uiState.sliderState,
                    isLoading = uiState.isLoading,
                    onPageChange = { onAction(ReaderBottomBarAction.PageChanged(it)) },
                    onSkipPrevious = { onAction(ReaderBottomBarAction.SkipPrevious) },
                    onSkipNext = { onAction(ReaderBottomBarAction.SkipNext) },
                )
            }
        }

        BottomActionSheet(
            buttons = uiState.buttons,
            onButtonClick = { onAction(ReaderBottomBarAction.ButtonClicked(it)) },
        )
    }
}
```

---

## 5. Technical Footprint & Integration

1. **[`ReaderControls.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderControls.kt)**: Replace lines 120–270 with concise, decoupled components.
2. **[`ReaderActivity.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt)**: Simplify bottom control binding to pass single `uiState` and receive `onAction`.
3. **New Previews**: Add `@Preview` for `HorizontalSlider`, `VerticalSlider`, and `BottomActionSheet`.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `ReaderBottomControlsUiState`, `ReaderSliderUiState`, and `ReaderBottomBarAction`.
- [ ] **Step 2**: Refactor `ReaderBottomControls` to consume the new state model.
- [ ] **Step 3**: Simplify `BottomActionSheet` into a generic button row iterating over `buttons`.
- [ ] **Step 4**: Update `ReaderActivity.kt` to bind the unified state flow.
- [ ] **Step 5**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
