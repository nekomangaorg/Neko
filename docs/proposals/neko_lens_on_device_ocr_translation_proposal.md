# Technical Proposal: Neko Lens (On-Device Manga OCR & Dictionary Assistant)

**Status:** Proposed / Under Review  
**Author:** Neko Development Team  
**Date:** August 2026  
**Target Milestone:** Neko 3.4 Reader Power-Tools  
**Implementation State:** 🔴 Completely New Feature (Not Present in Codebase)  

---

## 📌 Codebase Audit & Baseline Notes

> [!NOTE]
> **Current Codebase Baseline:**
> Neko's reader ([`ReaderActivity.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt), [`PagerViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/pager/PagerViewer.kt), [`WebtoonViewer.kt`](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/WebtoonViewer.kt)) renders pre-baked raster bitmap images (JPEG/PNG/WebP) directly on canvas surfaces. There is no optical character recognition (OCR) or text interaction layer.
>
> **What This Proposal Adds:**
> An on-device optical character recognition and text-selection overlay ("Neko Lens"). Using Google ML Kit on-device Text Recognition (supporting Japanese vertical/horizontal, Korean, Chinese, and Latin scripts), users can long-press dialogue bubbles to copy text, select words for instant dictionary lookup (e.g., Yomitan, Aedict, Pleco), or trigger quick translations without taking screenshots or switching apps.

---

## 1. Executive Summary & Vision

Many manga readers read series in their original Japanese, Korean, or Chinese languages for language learning or to follow untranslated raw releases. Others read localized scans but encounter stylized typography, kanji names, or untranslated sound effects (SFX) that they want to look up or copy.

### The Objective
This proposal introduces **Neko Lens**, a lightweight, on-device OCR and text selection layer integrated into the reader viewer. With a single tap or region-selection gesture, Neko detects speech bubbles and renders selectable, interactive text bounding boxes directly over the manga page.

### Key Highlights:
1. **100% On-Device ML Kit Integration**: Utilizes Google ML Kit's bundled on-device Japanese, Devanagari, Korean, and Latin Text Recognition modules (zero cloud latency, works completely offline, 100% private).
2. **Interactive Text Selection Bounding Boxes**: Tap a dialogue bubble to highlight the recognized sentence with native Android text selection handles (Copy, Share, Select All).
3. **Instant Dictionary Deep-Linking**: One-tap query forwarding to installed third-party dictionary apps (Yomitan, Aedict, Akebi, Pleco, Google Translate, DeepL).
4. **On-Demand Processing**: OCR only executes when triggered by the user (via long-press or lens button in reader controls), ensuring 0% battery/CPU overhead during standard reading.

---

## 2. Architectural Design

```mermaid
graph TD
    UserTrigger[User Long-Press or Taps Lens Button] --> ReaderUI[ReaderActivity / Compose Overlay]
    ReaderUI --> CaptureBitmap[Extract Visible Page Bitmap]
    
    subgraph On-Device OCR Pipeline
        CaptureBitmap --> MLKitInput[InputImage.fromBitmap]
        MLKitInput --> OCRRecognizers[ML Kit TextRecognizer\n(Japanese / Korean / Latin)]
        OCRRecognizers --> TextBlocks[Extracted TextBlocks & BoundingBoxes]
    end
    
    subgraph Interactive Layer
        TextBlocks --> LensOverlay[NekoLensCanvasOverlay]
        LensOverlay --> TapBubble[User Taps Dialogue Box]
        TapBubble --> ActionSheet[Text Action Modal Bottom Sheet]
        
        ActionSheet --> CopyClip[Copy to Clipboard]
        ActionSheet --> DictIntent[Send Intent to Yomitan / Aedict / Pleco]
        ActionSheet --> Translate[Quick On-Device Translation]
    end
```

---

## 3. Core Domain & Data Layer Changes

### 3.1 Domain Models

```kotlin
package org.nekomanga.domain.reader.lens

import android.graphics.Rect
import androidx.compose.runtime.Immutable

@Immutable
data class RecognizedTextBlock(
    val id: String,
    val rawText: String,
    val boundingBox: Rect,
    val lines: List<RecognizedTextLine>,
    val detectedLanguage: String?,
)

@Immutable
data class RecognizedTextLine(
    val text: String,
    val boundingBox: Rect,
)

@Immutable
data class NekoLensState(
    val isActive: Boolean = false,
    val isProcessing: Boolean = false,
    val detectedBlocks: List<RecognizedTextBlock> = emptyList(),
    val selectedBlock: RecognizedTextBlock? = null,
    val selectedText: String = "",
    val error: String? = null,
)
```

### 3.2 ML Kit OCR Engine Wrapper

```kotlin
package org.nekomanga.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.TextRecognition

class OnDeviceOcrEngine {
    private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    suspend fun recognizeTextOnPage(bitmap: Bitmap): Result<List<RecognizedTextBlock>> {
        // Process bitmap asynchronously and map Vision TextBlocks to domain models
    }
}
```

---

## 4. UI / UX Design Specifications

### 4.1 Lens Reader Overlay
- When Neko Lens is toggled:
  - The reader page dims by 15% with a subtle blueprint grid animation.
  - Recognized speech bubbles are highlighted with glowing semi-transparent rounded rectangles.
  - Tapping any bubble brings up the recognized text in a crisp, readable popup.

### 4.2 Text Action Bar
- Selected text presents quick action pills:
  - 📋 **Copy**: Copies clean text to Android clipboard.
  - 📖 **Dictionary**: Dispatches `ACTION_PROCESS_TEXT` or sends direct `Intent` to installed dictionary apps.
  - 🌐 **Translate**: Displays quick translation using ML Kit On-Device Translate.

---

## 5. Technical Footprint & Integration

1. **Gradle Dependencies**:
   - `com.google.mlkit:text-recognition-japanese:16.0.1`
   - `com.google.mlkit:text-recognition-korean:16.0.1`
   - `com.google.mlkit:text-recognition:16.0.1` (Latin)
2. **Reader Integration**:
   - Add `ReaderBottomButton.Lens` to [ReaderBottomButton.kt](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/eu/kanade/tachiyomi/ui/reader/settings/ReaderBottomButton.kt).
   - Inject `NekoLensOverlay.kt` into the reader Compose viewport.
3. **Preferences**:
   - Add OCR language preferences in [ReaderSettingsScreen.kt](file:///run/media/nonproto/WD4T/programming/workspace-android/Neko/app/src/main/java/org/nekomanga/presentation/screens/settings/screens/ReaderSettingsScreen.kt).

---

## 6. Implementation Plan & Milestones

- [ ] **Step 1**: Add ML Kit OCR dependencies and build `OnDeviceOcrEngine`.
- [ ] **Step 2**: Create Compose interactive canvas overlay mapping image coordinate space to screen viewport.
- [ ] **Step 3**: Implement text selection, clipboard copying, and dictionary intent forwarding.
- [ ] **Step 4**: Add OCR trigger button in Reader toolbar and configure long-press gesture bindings.
