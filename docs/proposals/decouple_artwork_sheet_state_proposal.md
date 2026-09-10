# Technical Proposal: Decoupling ArtworkSheet from Internal Selection State, Inline Coil Builders & Domain Entities

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x UI Decoupling  
**Implementation State:** 🟡 Coupled Baseline (Internal mutable state for selection, inline Coil `ImageRequest.Builder` calls, and direct domain entity coupling)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> In [`ArtworkSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/ArtworkSheet.kt#L62-L156):
> - **Internal Mutable Selection State**:
>   ```kotlin
>   // ArtworkSheet.kt: Lines 76–81
>   var currentImage by
>       remember(alternativeArtwork) {
>           mutableStateOf(
>               alternativeArtwork.firstOrNull { it.active } ?: alternativeArtwork.firstOrNull()
>           )
>       }
>   ```
>   The sheet tracks the selected cover internally via `mutableStateOf`. The parent (`MangaScreen` / `MangaViewModel`) has no visibility into which cover is actively being previewed until the user commits an action.
> - **Inline Coil ImageRequest Creation**:
>   ```kotlin
>   // ArtworkSheet.kt: Lines 107–110
>   val mainImageRequest =
>       remember(currentImage) {
>           ImageRequest.Builder(context).data(currentImage).build()
>       }
>   ```
>   The Composable directly manages image request lifecycle and context binding inline instead of receiving a plain image URL/model.
> - **Unhoisted Action Callbacks & Domain Entity Leaks**:
>   - Takes 4 separate lambdas typed directly with domain `Artwork`: `saveClick: (Artwork) -> Unit`, `setClick: (Artwork) -> Unit`, `shareClick: (Artwork) -> Unit`, `resetClick: () -> Unit`.
>
> **What This Proposal Solves:**
> Hoists selection state to the ViewModel, introduces an immutable `ArtworkSheetUiModel`, models user intents with a sealed `ArtworkSheetAction` interface, and removes inline Coil request builders from Compose.

---

## 1. Executive Summary & Vision

A bottom sheet in Jetpack Compose should be a purely declarative presentation component that receives its full display state from above and emits user events. Managing selection state internally in `ArtworkSheet` prevents state persistence across process recreation, inhibits external synchronization, and precludes previews in Android Studio.

### The Objective
Decouple [`ArtworkSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/ArtworkSheet.kt) by:
1. Hoisting the selected artwork state up to the ViewModel / caller screen.
2. Creating an immutable `ArtworkSheetUiModel` and `ArtworkThumbnailUiModel`.
3. Consolidating all user actions into a single sealed `ArtworkSheetAction` interface.
4. Passing plain image data to `AsyncImage` rather than building `ImageRequest` objects inline.
5. Providing full `@Preview` coverage for single-cover, multi-cover, and empty states.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph Current Coupled Flow
        Parent["MangaScreen"] --> Sheet["ArtworkSheet Composable"]
        Sheet -->|Internal mutableStateOf| Current["currentImage state"]
        Sheet -->|Builds inline| Coil["ImageRequest.Builder(context)"]
        Sheet -->|Calls with domain entity| Lambda["setClick(Artwork), saveClick(Artwork)"]
    end

    subgraph Proposed Decoupled Flow
        VM["MangaViewModel / ArtworkController"] -->|selectedArtworkId Flow| State["ArtworkSheetUiModel (Immutable)"]
        State --> Sheet2["Stateless ArtworkSheet"]
        Sheet2 -->|Direct URL/Data to AsyncImage| Coil2["Coil Image Pipeline"]
        Sheet2 -->|onAction(ArtworkSheetAction)| VM
    end
```

---

## 3. Proposed Domain & UI Models

### 3.1 Immutable Artwork UI State Models

```kotlin
@Immutable
data class ArtworkSheetUiModel(
    val selectedArtwork: ArtworkItemUiModel?,
    val artworks: List<ArtworkItemUiModel> = emptyList(),
    val isInLibrary: Boolean = false,
    val isLoading: Boolean = false,
)

@Immutable
data class ArtworkItemUiModel(
    val id: Long,
    val imageUrl: String,
    val description: String,
    val isActive: Boolean,
)
```

### 3.2 Sealed Artwork Sheet Actions

```kotlin
sealed interface ArtworkSheetAction {
    data class SelectArtwork(val artworkId: Long) : ArtworkSheetAction
    data class SaveArtwork(val artworkId: Long) : ArtworkSheetAction
    data class SetAsCover(val artworkId: Long) : ArtworkSheetAction
    data class ShareArtwork(val artworkId: Long) : ArtworkSheetAction
    data object ResetCover : ArtworkSheetAction
    data object Dismiss : ArtworkSheetAction
}
```

---

## 4. UI / Compose Layer Refactoring

### 4.1 Stateless ArtworkSheet Signature

```kotlin
@Composable
fun ArtworkSheet(
    uiModel: ArtworkSheetUiModel,
    themeColorState: ThemeColorState = defaultThemeColorState(),
    onAction: (ArtworkSheetAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseSheet(themeColor = themeColorState, maxSheetHeightPercentage = .9f) {
        if (uiModel.artworks.isEmpty() || uiModel.selectedArtwork == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_artwork_found),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            ArtworkSheetContent(
                selectedArtwork = uiModel.selectedArtwork,
                artworks = uiModel.artworks,
                inLibrary = uiModel.isInLibrary,
                themeColorState = themeColorState,
                onAction = onAction,
            )
        }
    }
}
```

### 4.2 Stateless ArtworkSheetContent

```kotlin
@Composable
private fun ArtworkSheetContent(
    selectedArtwork: ArtworkItemUiModel,
    artworks: List<ArtworkItemUiModel>,
    inLibrary: Boolean,
    themeColorState: ThemeColorState,
    onAction: (ArtworkSheetAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = selectedArtwork.description,
            modifier = Modifier.padding(horizontal = Size.small),
            style = MaterialTheme.typography.labelLarge,
        )

        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.small),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = selectedArtwork.imageUrl,
                contentDescription = stringResource(R.string.artwork),
                contentScale = ContentScale.Fit,
            )
        }

        ArtworkActionButtons(
            inLibrary = inLibrary,
            themeColorState = themeColorState,
            onSave = { onAction(ArtworkSheetAction.SaveArtwork(selectedArtwork.id)) },
            onSet = { onAction(ArtworkSheetAction.SetAsCover(selectedArtwork.id)) },
            onReset = { onAction(ArtworkSheetAction.ResetCover) },
            onShare = { onAction(ArtworkSheetAction.ShareArtwork(selectedArtwork.id)) },
        )

        if (artworks.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Size.tiny)) {
                items(artworks, key = { it.id }) { artwork ->
                    ArtworkThumbnail(
                        model = artwork,
                        isSelected = artwork.id == selectedArtwork.id,
                        themeColorState = themeColorState,
                        onClick = { onAction(ArtworkSheetAction.SelectArtwork(artwork.id)) },
                    )
                }
            }
        }
    }
}
```

---

## 5. Technical Footprint & Integration

1. **[`ArtworkSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/components/sheets/ArtworkSheet.kt)**:
   - Remove lines 76–81 (`var currentImage by remember(alternativeArtwork) { mutableStateOf(...) }`).
   - Remove lines 107–110 (`ImageRequest.Builder(context)` inline).
   - Replace 4 domain-typed lambdas with single `onAction: (ArtworkSheetAction) -> Unit`.
2. **[`MangaViewModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/manga/MangaViewModel.kt)**:
   - Expose `selectedArtworkId: StateFlow<Long?>` and handle `ArtworkSheetAction`.
3. **New Previews**: Add `@Preview` for `ArtworkSheetContent` showing selected active cover and thumbnail list.

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Define `ArtworkSheetUiModel`, `ArtworkItemUiModel`, and `ArtworkSheetAction`.
- [ ] **Step 2**: Hoist active cover selection to `MangaViewModel` (or `MangaArtworkController`).
- [ ] **Step 3**: Refactor `ArtworkSheet.kt` to be completely stateless.
- [ ] **Step 4**: Pass URL directly to `AsyncImage` model.
- [ ] **Step 5**: Add Compose previews for `ArtworkSheet`.
- [ ] **Step 6**: Run `./gradlew ktfmtFormat` and `./gradlew testDebugUnitTest`.
