1. **Analyze Deprecation Warning in `DiskUtil.kt`**
   - File: `core/src/main/kotlin/tachiyomi/core/util/storage/DiskUtil.kt`
   - Issue: Uses `Intent.ACTION_MEDIA_SCANNER_SCAN_FILE` which is deprecated in Java (Android Q/API 29+).
   - Solution: The modern equivalent is `MediaScannerConnection.scanFile`.

2. **Replace Deprecated API**
   - In `scanMedia(context: Context, uri: Uri)` in `DiskUtil.kt`, remove the intent broadcast.
   - Use `MediaScannerConnection.scanFile(context, arrayOf(uri.path), null, null)` instead. Actually, since `uri.path` might not be the exact physical path if it's a content URI, and `scanFile` expects a path string, we need to handle it properly. The old code used `Intent.ACTION_MEDIA_SCANNER_SCAN_FILE` with a data `Uri`.
   - Wait, `MediaScannerConnection.scanFile` takes `String[] paths` and `String[] mimeTypes`.
   - Alternatively, use `MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)` in `scanMedia(context: Context, file: File)`.
   - Since `scanMedia` with `Uri` is only used from `scanMedia` with `File` and `createNoMediaFile` with `dir.uri`.
   - Let's investigate how to properly replace `Intent.ACTION_MEDIA_SCANNER_SCAN_FILE`.

3. **Modify Code**
   - Modify `DiskUtil.kt` to import `android.media.MediaScannerConnection`.
   - Change `scanMedia` methods to use `MediaScannerConnection`.

4. **Verify the change**
   - Build the project and run `./gradlew core:lintDebug` to ensure the specific deprecation warning is resolved.

5. **Pre-commit Checks**
   - Run pre-commit instructions.

6. **Submit PR**
   - Create branch, commit, and submit.
