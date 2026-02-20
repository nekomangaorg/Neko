## 2025-02-17 - NekoTheme Preview Wrapper
**Learning:** `NekoTheme` (in `org.nekomanga.presentation.theme`) requires `Injekt` (dependency injection) for preferences. To use it in `@Preview` without crashing, manually pass a `colorScheme` parameter (e.g., `NekoColorScheme.lightScheme`).
**Action:** Always wrap previews in `NekoTheme(colorScheme = ...)` or mock `Injekt`.

## 2025-05-15 - Preview Wrappers and Image Mocks
**Learning:** This app uses `ThemedPreviews` (composable) to render components across all themes. For Coil images in previews, use `AsyncImagePreviewHandler` provided via `LocalAsyncImagePreviewHandler`.
**Action:** Wrap previews in `ThemedPreviews { ... }` and provide `LocalAsyncImagePreviewHandler` with a dummy `ColorImage`.
