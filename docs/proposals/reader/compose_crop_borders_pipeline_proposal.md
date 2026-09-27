# Technical Proposal: Native Compose Crop Borders Pipeline & Auto-Crop Image Transformation

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** September 2026  
**Target Milestone:** Neko 3.x Reader Decoupling & Rendering Pipeline  
**Execution Order:** Reader Track — Phase R3 (Advanced Rendering & Image Processing), Step R12 (Priority: Medium / Visual Polish & Image Processing)  
**Resolves Issue:** [GitHub Issue #3435](https://github.com/nekomangaorg/Neko/issues/3435) ("Regression: 'Crop borders' doesn't work in Paged mode")  
**Prerequisites:** Step R5 ([`rock_solid_paged_compose_viewer_proposal.md`](rock_solid_paged_compose_viewer_proposal.md))  
**Downstream Dependents:** Step R11 ([`native_compose_webtoon_subsampling_renderer_proposal.md`](native_compose_webtoon_subsampling_renderer_proposal.md))  
**Implementation Targets:** [`PagerViewerConfigUiModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerViewerConfigUiModel.kt), [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt), [`DoublePageLayout.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/DoublePageLayout.kt), [`CropBordersTransformation.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/domain/reader/image/CropBordersTransformation.kt)  

---

## 📌 Baseline Audit & Problem Statement

### 1.1 Context & Regression Background
In legacy Tachiyomi and earlier Neko releases (prior to Compose reader migration), "Crop borders" was powered by a low-level routine inside `SubsamplingScaleImageView.setCropBorders(boolean)`. During image decoding and tile rendering, the legacy view sampled edge pixel rows/columns, trimmed uniform solid-color margins (white scan borders or black digital margins), and adjusted the source rectangle accordingly.

During the migration to Jetpack Compose ([`0aa3d687e7`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/commit/0aa3d687e7)) and the adoption of Coil 3 with Telephoto ([`me.saket.telephoto:zoomable-image-coil3`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/gradle/libs.versions.toml)):
1. `SubsamplingScaleImageView` was eliminated in favor of Compose-native `ZoomableAsyncImage`.
2. The user preference `ReaderPreferences.cropBorders()` was preserved in the settings UI ([`ReaderSettingsSheet.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderSettingsSheet.kt) and [`ReaderControls.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/ReaderControls.kt)), but **its value was completely disconnected from the Compose image rendering pipeline**.
3. Neither [`PagerViewerConfigUiModel`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerViewerConfigUiModel.kt) nor the Coil `ImageRequest` instances inside [`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt#L425-L435) accept or apply any crop transformations.

### 1.2 User-Facing Consequence
When reading manga releases with generous margins or raw scan borders (such as scans with prominent white padding on the right/left edges), enabling "Crop borders" has no effect in Paged mode. Users see oversized blank gutters, reducing page legibility and preventing art from expanding across available screen space.

---

## 2. Architectural Design

```mermaid
flowchart TD
    subgraph UI ["Compose Presentation Layer"]
        Prefs["ReaderPreferences.cropBorders()"] --> RVM["ReaderViewModel / ReaderState"]
        RVM --> PagerConfig["PagerViewerConfigUiModel\n(cropBorders: Boolean)"]
        PagerConfig --> PPI["PagerPageItem.kt / DoublePageLayout.kt"]
    end

    subgraph CoilPipeline ["Coil 3 Image Pipeline"]
        PPI --> IR["ImageRequest.Builder(context)"]
        IR -->|transformations(CropBordersTransformation)| Engine["Coil ImageLoader"]
        Engine --> CBT["CropBordersTransformation"]
        CBT -->|Scan Margins & Calculate Crop Rect| CropAlgo["CropBordersEngine.calculateCrop(bitmap)"]
        CropAlgo -->|Trimmed Bitmap| MemoryCache["Coil MemoryCache\nKey: page_url_crop_true"]
    end

    subgraph TelephotoIntegration ["Telephoto Zoomable Layer"]
        MemoryCache --> State["ZoomableImageState"]
        State --> ZoomView["ZoomableAsyncImage"]
    end
```

---

## 3. Technical Specifications

### 3.1 Domain Transformation Engine: `CropBordersTransformation`

Coil 3 allows custom image manipulation via `coil3.transform.Transformation`. The transformation inspects pixel luminance along the four exterior edges:

```kotlin
package org.nekomanga.domain.reader.image

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import coil3.size.Size
import coil3.transform.Transformation
import kotlin.math.abs

/**
 * Trims solid black, white, or uniform monotone margins from decoded manga page bitmaps.
 */
class CropBordersTransformation(
    private val enabled: Boolean,
    private val tolerance: Int = 15,
) : Transformation() {

    override val cacheKey: String = "org.nekomanga.crop_borders_enabled_${enabled}_tol_${tolerance}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (!enabled) return input
        val cropRect = calculateCropBounds(input, tolerance) ?: return input
        
        // Return original if margins are negligible (e.g. < 2 pixels trimmed on all sides)
        if (cropRect.left <= 2 && cropRect.top <= 2 && 
            cropRect.right >= input.width - 2 && cropRect.bottom >= input.height - 2) {
            return input
        }

        return Bitmap.createBitmap(
            input,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height(),
        )
    }

    companion object {
        fun calculateCropBounds(bitmap: Bitmap, tolerance: Int): Rect? {
            val width = bitmap.width
            val height = bitmap.height
            if (width < 10 || height < 10) return null

            val pixels = IntArray(maxOf(width, height))

            // 1. Scan Top edge
            var top = 0
            for (y in 0 until height / 4) {
                bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
                if (!isUniformRow(pixels, width, tolerance)) {
                    top = y
                    break
                }
            }

            // 2. Scan Bottom edge
            var bottom = height
            for (y in height - 1 downTo height - (height / 4)) {
                bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
                if (!isUniformRow(pixels, width, tolerance)) {
                    bottom = y + 1
                    break
                }
            }

            // 3. Scan Left edge
            var left = 0
            for (x in 0 until width / 4) {
                bitmap.getPixels(pixels, 0, 1, x, 0, 1, height)
                if (!isUniformColumn(pixels, height, tolerance)) {
                    left = x
                    break
                }
            }

            // 4. Scan Right edge
            var right = width
            for (x in width - 1 downTo width - (width / 4)) {
                bitmap.getPixels(pixels, 0, 1, x, 0, 1, height)
                if (!isUniformColumn(pixels, height, tolerance)) {
                    right = x + 1
                    break
                }
            }

            if (left >= right || top >= bottom) return null
            return Rect(left, top, right, bottom)
        }

        private fun isUniformRow(pixels: IntArray, length: Int, tolerance: Int): Boolean {
            val first = pixels[0]
            val firstLuma = Color.luminance(first)
            // Only crop if border is black-ish or white-ish
            if (firstLuma > 0.05f && firstLuma < 0.95f) return false
            for (i in 1 until length) {
                if (abs(Color.luminance(pixels[i]) - firstLuma) > tolerance / 255f) {
                    return false
                }
            }
            return true
        }

        private fun isUniformColumn(pixels: IntArray, length: Int, tolerance: Int): Boolean {
            val first = pixels[0]
            val firstLuma = Color.luminance(first)
            if (firstLuma > 0.05f && firstLuma < 0.95f) return false
            for (i in 1 until length) {
                if (abs(Color.luminance(pixels[i]) - firstLuma) > tolerance / 255f) {
                    return false
                }
            }
            return true
        }
    }
}
```

### 3.2 Wire `cropBorders` Into UI Model & Composable Items

1. **[`PagerViewerConfigUiModel.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerViewerConfigUiModel.kt)**:
   ```kotlin
   @Immutable
   data class PagerViewerConfigUiModel(
       ...
       val cropBorders: Boolean = false,
       ...
   )
   ```

2. **[`PagerPageItem.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/PagerPageItem.kt)**:
   ```kotlin
   val model =
       remember(page, config.cropBorders) {
           ImageRequest.Builder(context)
               .data(page)
               .size(CoilSize.ORIGINAL)
               .maxBitmapSize(CoilSize(GLUtil.maxTextureSize, GLUtil.maxTextureSize))
               .precision(Precision.EXACT)
               .crossfade(true)
               .apply {
                   if (config.cropBorders) {
                       transformations(CropBordersTransformation(enabled = true))
                   }
               }
               .build()
       }
   ```

3. **[`DoublePageLayout.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/reader/viewer/DoublePageLayout.kt)**:
   Apply matching transformation to both `firstModel` and `secondModel`, ensuring dual-page spread calculations accurately measure cropped dimensions rather than uncropped margins.

---

## 4. Performance, Memory & Caching Considerations

1. **Cache Key Isolation**:
   - `CropBordersTransformation.cacheKey` isolates memory and disk cache results. Toggling "Crop borders" in reader settings immediately re-requests and displays the updated bitmap without displaying stale cached margins.
2. **Allocation Efficiency**:
   - The edge scanning scans up to 25% of image width/height from the edges inward (`0 until width / 4`). If a non-uniform pixel is encountered on line 0, scanning halts immediately with zero additional bitmap allocations.
   - If no cropping is warranted, the original `input` Bitmap reference is returned directly, bypassing `Bitmap.createBitmap`.
3. **Double-Page Geometry**:
   - In `DoublePageLayout`, trimming margins ensures that two facing pages join flush in the center when double-page mode is enabled without artificial white vertical seams.

---

## 5. Implementation Roadmap & Milestones

1. **Step 1**: Implement pure Kotlin `CropBordersTransformation` with unit tests for edge luminance scanning and bounding box detection.
2. **Step 2**: Add `cropBorders: Boolean` to `PagerViewerConfigUiModel`, populate it from `ReaderPreferences.cropBorders()` in `ReaderViewModel` / `ReaderScreen`.
3. **Step 3**: Connect the transformation to `ImageRequest.Builder` in `PagerPageItem.kt` and `DoublePageLayout.kt`.
4. **Step 4**: Format code with `./gradlew ktfmtFormat` and verify regression test suite.
