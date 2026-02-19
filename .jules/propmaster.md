## 2025-02-17 - NekoTheme Preview Wrapper
**Learning:** `NekoTheme` (in `org.nekomanga.presentation.theme`) requires `Injekt` (dependency injection) for preferences. To use it in `@Preview` without crashing, manually pass a `colorScheme` parameter (e.g., `NekoColorScheme.lightScheme`).
**Action:** Always wrap previews in `NekoTheme(colorScheme = ...)` or mock `Injekt`.
