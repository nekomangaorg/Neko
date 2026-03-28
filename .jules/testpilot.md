## 2026-03-21 - StorageManager Testing Insights
**Learning:** `ModifyMangaUseCase` relies on global injection of `StorageManager` via `Injekt`. Furthermore, `DownloadProvider` internally relies on injected dependencies (`StorageManager` and `SourceManager`) which must be registered with `Injekt` before tests instantiate or mock `DownloadProvider`.
**Action:** When testing classes that interact with download or storage subsystems, ensure `Injekt.addSingleton(...)` is configured for any globally required dependencies (like `StorageManager` or `SourceManager`) even if they are mocked and passed directly via constructors.
## 2024-03-28 - Testing ChapterItemSort getNextUnreadChapter
**Learning:** Injekt framework requires explicit mocking of internal dependencies. The preferences structure `MangaDetailsPreferences` is highly nested and requires chained mocking for values like `sortDescending().get()` and `sortChapterOrder().get()`. `MockK` matches explicit property calls well.
**Action:** When testing components relying on nested preferences, ensure to mock the entire call chain (`preference.method().get()`) to avoid `NullPointerException` during test execution.
