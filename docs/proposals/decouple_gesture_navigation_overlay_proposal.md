# Technical Proposal: Decoupling GestureNavigationOverlay from ViewerNavigation & Geometry Inversion Math

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Legacy ViewerNavigation classes, invert math, and ContextCompat color resolution in Compose)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`GestureNavigationOverlay.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/GestureNavigationOverlay.kt#L32-L100), the tap gesture navigation hint overlay couples directly to legacy viewer navigation controllers and performs coordinate geometry math inside Composable loops:
> - **Line 33**: Accepts legacy controller: `navigation: ViewerNavigation?`.
> - **Line 38**: Accepts controller enum: `invertMode: ViewerNavigation.TappingInvertMode`.
> - **Lines 67–72**: Iterates and calculates coordinate transformations, directional region lookups, and Android `Context` color queries during composition:
>   ```kotlin
>   val region = regionItem.invert(invertMode)
>   val rect = region.rectF
>   val directionalRegion = region.type.directionalRegion(isLtr)
>   val color = Color(ContextCompat.getColor(context, directionalRegion.colorRes))
>   ```
> - Multiplies screen bounds (`width * rect.left`, `height * rect.top`) per region item on each frame.
>
> **What This Proposal Solves:**
> Decouples `GestureNavigationOverlay` from `ViewerNavigation` by extracting a pure `NavigationRegionUiModel`. The Composable receives pre-calculated relative bounds, colors, and localized region labels, eliminating all controller references and Android Context lookups.

---

## 1. Executive Summary & Vision

The gesture navigation overlay is an educational visual overlay displaying touch target zones (e.g. "Next Page", "Previous Page", "Menu") when configuring reader navigation styles. Coupling it directly to `ViewerNavigation` forces Compose to understand tap invert rules, LTR/RTL transformations, and color resource mapping, preventing fast previewing and unit testing.

### The Objective
Decouple [`GestureNavigationOverlay.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/GestureNavigationOverlay.kt) by:
1. Creating an immutable `NavigationRegionUiModel`.
2. Offloading region inversion, directional mapping, and color assignments to `ReaderViewModel` or a pure UseCase.
3. Making `GestureNavigationOverlay` a dumb rendering overlay that receives `List<NavigationRegionUiModel>` and emits `onDismiss`.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Architecture
        Overlay1["GestureNavigationOverlay Composable"] -->|Accepts| VN["ViewerNavigation & TappingInvertMode"]
        Overlay1 -->|Invert Math & Directional Lookup| Math["region.invert() & directionalRegion(isLtr)"]
        Overlay1 -->|Color Lookup| CC["ContextCompat.getColor(context, colorRes)"]
    end

    subgraph Proposed Decoupled Architecture
        VN2["ViewerNavigation Rules"] --> UC["GetNavigationRegionsUseCase"]
        UC --> Regions["List<NavigationRegionUiModel>\n(relativeBounds, color, labelText)"]
        Regions --> Overlay2["Stateless GestureNavigationOverlay\n(regions, onDismiss)"]
        Overlay2 -->|onDismiss()| Host["ReaderScreen"]
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable Navigation Region UI Model

```kotlin
@Immutable
data class NavigationRegionUiModel(
    val id: String,
    val leftFraction: Float,
    val topFraction: Float,
    val rightFraction: Float,
    val bottomFraction: Float,
    val label: UiText,
    val color: Color,
)
```

### 3.2 Extracted UseCase

```kotlin
class GetNavigationRegionsUseCase {
    operator fun invoke(
        navigation: ViewerNavigation,
        invertMode: ViewerNavigation.TappingInvertMode,
        isLtr: Boolean,
        colorProvider: (Int) -> Color,
    ): List<NavigationRegionUiModel> {
        return navigation.regions.map { regionItem ->
            val region = regionItem.invert(invertMode)
            val rect = region.rectF
            val directional = region.type.directionalRegion(isLtr)
            NavigationRegionUiModel(
                id = "${region.type.name}_${rect.left}_${rect.top}",
                leftFraction = rect.left,
                topFraction = rect.top,
                rightFraction = rect.right,
                bottomFraction = rect.bottom,
                label = UiText.StringResource(directional.nameRes),
                color = colorProvider(directional.colorRes),
            )
        }
    }
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless `GestureNavigationOverlay`

```kotlin
@Composable
fun GestureNavigationOverlay(
    regions: List<NavigationRegionUiModel>,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (regions.isEmpty()) return

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().pointerInput(Unit) { ... }) {
            regions.forEach { region ->
                val left = maxWidth * region.leftFraction
                val top = maxHeight * region.topFraction
                val width = maxWidth * (region.rightFraction - region.leftFraction)
                val height = maxHeight * (region.bottomFraction - region.topFraction)

                Box(
                    modifier = Modifier.offset(x = left, y = top).size(width, height).background(region.color)
                ) {
                    Text(text = region.label.asString(), ...)
                }
            }
        }
    }
}
```

---

## 5. Technical Footprint & Integration

1. **[`GestureNavigationOverlay.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/GestureNavigationOverlay.kt)**: Simplify to accept `regions: List<NavigationRegionUiModel>`.
2. **[`ReaderActivity.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt)**: Map active navigation regions into UI models before displaying overlay.
3. **New Previews**: Add `@Preview` for `KindlishNavigation`, `LNavigation`, and `RightAndLeftNavigation` configurations.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `NavigationRegionUiModel`.
- [ ] **Step 2**: Implement `GetNavigationRegionsUseCase` with unit tests for LTR/RTL and invert modes.
- [ ] **Step 3**: Refactor `GestureNavigationOverlay.kt` to be 100% stateless and previewable.
- [ ] **Step 4**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
