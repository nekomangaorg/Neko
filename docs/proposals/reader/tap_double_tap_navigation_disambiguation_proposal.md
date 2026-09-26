# Technical Proposal: Tap vs Double-Tap Navigation Disambiguation in Compose Pager

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling & Gesture Stabilization  
**Execution Order:** Reader Track — Phase R4 (Gesture Engine & Pointer Interaction), Step R14 (Priority: High / Touch Navigation & Zoom Conflict)  
**Resolves Issue:** [GitHub Issue #3433](https://github.com/nekomangaorg/Neko/issues/3433) ("Gesture issue: double tap to zoom in navigation regions turns page on first tap")  
**Prerequisites:** Step R5 ([`rock_solid_paged_compose_viewer_proposal.md`](rock_solid_paged_compose_viewer_proposal.md))  
**Downstream Dependents:** None  
**Implementation Targets:** [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt)  

---

## 📌 Baseline Audit & Problem Statement

### 1.1 Context & Reproduction
In [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt), pages in `ComposePagerViewer` handle tap navigation (turning to next/previous pages) and double-tap zoom (scaling into page panels via Telephoto's `DoubleClickToZoomListener`).

### 1.2 Root Cause Analysis
In [`PagerPageItem.kt#L266-L325`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt#L266-L325):
```kotlin
val isDoubleTap =
    (upTime - lastTapTime < doubleTapTimeoutMs) &&
        (hypot(
            (upPos.x - lastTapOffset.x).toDouble(),
            (upPos.y - lastTapOffset.y).toDouble(),
        ) < doubleTapSlopPx) &&
        (currentConfig.doubleTapAnimDuration > 0)

if (isDoubleTap) {
    if (currentConfig.menuVisible) {
        currentConfig.onToggleMenu()
    }
    lastTapTime = 0L
    lastTapOffset = Offset.Zero
} else {
    lastTapTime = upTime
    lastTapOffset = upPos

    when (action) {
        ViewerNavigation.NavigationRegion.NEXT -> {
            up.consume()
            if (currentConfig.menuVisible) {
                currentConfig.onToggleMenu()
            }
            currentConfig.onNavigateAdjacent(true)
        }
        ViewerNavigation.NavigationRegion.PREV -> {
            up.consume()
            if (currentConfig.menuVisible) {
                currentConfig.onToggleMenu()
            }
            currentConfig.onNavigateAdjacent(false)
        }
        ...
```

**The Breakdown:**
1. When a user double-taps on the left or right third of the screen to zoom into art:
   - On **Tap 1**: `isDoubleTap` is `false` (since `lastTapTime` was 0).
   - The code enters the `else` branch, records `lastTapTime`, **consumes the pointer event immediately**, and directly fires `currentConfig.onNavigateAdjacent(...)`.
   - The Compose pager immediately begins animating/scrolling toward the adjacent page.
2. When **Tap 2** arrives within `doubleTapTimeoutMs` (typically 250–300ms):
   - The page is already actively animating and shifting out from under the pointer.
   - Telephoto's zoom listener receives conflicting touch inputs, or the active pager scroll animation fights with the zoom animation, causing the page to stutter, bounce back, or switch pages instead of zooming.
3. The center region (`MENU`) is unaffected because toggling the reader menu bar does not trigger a pager scroll animation.

---

## 2. Architectural Design: Deferred Navigation & Disambiguation

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant PointerInput as PagerPageItem PointerInput
    participant Debounce as Deferred Nav Job
    participant Pager as ComposePagerViewer
    participant Telephoto as Telephoto Zoomable

    alt User Performs Single Tap (Page Turn)
        User->>PointerInput: Tap 1 Up in NEXT region
        PointerInput->>Debounce: Launch Job: Delay 250ms
        Note over Debounce: Waiting for possible Tap 2...
        Debounce-->>Pager: Timeout elapsed: Fire onNavigateAdjacent(forward = true)
        Pager->>Pager: Animate to Next Page
    else User Performs Double Tap (Zoom In)
        User->>PointerInput: Tap 1 Up in NEXT region
        PointerInput->>Debounce: Launch Job: Delay 250ms
        User->>PointerInput: Tap 2 Up at same position (< 250ms)
        PointerInput->>Debounce: Cancel Job!
        PointerInput->>Telephoto: Confirm Double-Tap -> Zoom in/out
        Note over Pager: Page turn aborted; Pager remains stationary
    else User Has Disabled Double Tap Zoom (AnimDuration == 0)
        User->>PointerInput: Tap 1 Up in NEXT region
        PointerInput->>Pager: Fire onNavigateAdjacent(forward = true) immediately (0ms latency)
    end
```

---

## 3. Technical Specifications

### 3.1 Debounced Navigation State Machine

1. **Retain a Cancellable Coroutine Job**:
   Inside [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt), track a pending navigation job:
   ```kotlin
   val coroutineScope = rememberCoroutineScope()
   var pendingNavJob by remember { mutableStateOf<Job?>(null) }
   ```

2. **Pointer Event Processing**:
   ```kotlin
   if (isDoubleTap) {
       // Cancel any pending single-tap navigation immediately
       pendingNavJob?.cancel()
       pendingNavJob = null

       if (currentConfig.menuVisible) {
           currentConfig.onToggleMenu()
       }
       lastTapTime = 0L
       lastTapOffset = Offset.Zero
   } else {
       lastTapTime = upTime
       lastTapOffset = upPos

       val executeNav = {
           when (action) {
               ViewerNavigation.NavigationRegion.NEXT -> {
                   if (currentConfig.menuVisible) currentConfig.onToggleMenu()
                   currentConfig.onNavigateAdjacent(true)
               }
               ViewerNavigation.NavigationRegion.PREV -> {
                   if (currentConfig.menuVisible) currentConfig.onToggleMenu()
                   currentConfig.onNavigateAdjacent(false)
               }
               ViewerNavigation.NavigationRegion.RIGHT -> {
                   if (currentConfig.menuVisible) currentConfig.onToggleMenu()
                   currentConfig.onNavigateAdjacent(!currentConfig.isRtl)
               }
               ViewerNavigation.NavigationRegion.LEFT -> {
                   if (currentConfig.menuVisible) currentConfig.onToggleMenu()
                   currentConfig.onNavigateAdjacent(currentConfig.isRtl)
               }
               ViewerNavigation.NavigationRegion.MENU -> {
                   currentConfig.onToggleMenu()
               }
           }
       }

       if (currentConfig.doubleTapAnimDuration > 0 && action != ViewerNavigation.NavigationRegion.MENU) {
           // Defer single tap until double-tap timeout expires
           pendingNavJob?.cancel()
           pendingNavJob = coroutineScope.launch {
               delay(doubleTapTimeoutMs)
               executeNav()
               pendingNavJob = null
           }
       } else {
           // Immediate execution for MENU or when double-tap zoom is disabled
           executeNav()
       }
   }
   ```

3. **Event Consumption Decoupling**:
   Do not consume `up` on Tap 1 if `currentConfig.doubleTapAnimDuration > 0`. This ensures downstream pointer handlers (including Telephoto's `onDoubleClick`) receive both taps unencumbered.

---

## 4. Key Guarantees & Edge Cases

1. **Zero-Latency Navigation When Double Tap Disabled**:
   Users who disable double-tap zoom (`config.doubleTapAnimDuration == 0`) retain instant single-tap navigation with 0ms delay.
2. **Deterministic Zooming Without Page Flickering**:
   Because navigation is deferred during the double-tap evaluation window, the pager remains completely stationary while zooming into margins or double-page spreads.
3. **Clean Teardown on Dispose**:
   If the user navigates away or disposes the composable, `coroutineScope` automatically cancels any lingering pending navigation jobs, preventing memory leaks and orphaned scroll requests.

---

## 5. Test Specifications

1. **Double Tap Zoom In Navigation Region**:
   - Tap at $(0.15 \times W, 0.5 \times H)$ (PREV region).
   - Tap again at the same position after $100\text{ms}$.
   - Advance clock by $500\text{ms}$.
   - Assert `onNavigateAdjacent` was **never invoked**.
   - Assert Telephoto `zoomFactor > 1.0f`.
2. **Single Tap Navigation With Debounce**:
   - Tap at $(0.85 \times W, 0.5 \times H)$ (NEXT region).
   - Advance clock by $240\text{ms}$ -> assert `onNavigateAdjacent` has not yet fired.
   - Advance clock by $20\text{ms}$ (total $260\text{ms}$) -> assert `onNavigateAdjacent(true)` fired **exactly once**.
3. **Menu Region Instant Response**:
   - Tap at $(0.5 \times W, 0.5 \times H)$ (MENU region).
   - Assert `onToggleMenu` is called **immediately without 250ms debounce**.
