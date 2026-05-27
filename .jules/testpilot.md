## 2026-03-21 - StorageManager Testing Insights
**Learning:** `ModifyMangaUseCase` relies on global injection of `StorageManager` via `Injekt`. Furthermore, `DownloadProvider` internally relies on injected dependencies (`StorageManager` and `SourceManager`) which must be registered with `Injekt` before tests instantiate or mock `DownloadProvider`.
**Action:** When testing classes that interact with download or storage subsystems, ensure `Injekt.addSingleton(...)` is configured for any globally required dependencies (like `StorageManager` or `SourceManager`) even if they are mocked and passed directly via constructors.
## 2026-03-28 - Testing ChapterItemSort getNextUnreadChapter
**Learning:** Injekt framework requires explicit mocking of internal dependencies. The preferences structure `MangaDetailsPreferences` is highly nested and requires chained mocking for values like `sortDescending().get()` and `sortChapterOrder().get()`. `MockK` matches explicit property calls well.
**Action:** When testing components relying on nested preferences, ensure to mock the entire call chain (`preference.method().get()`) to avoid `NullPointerException` during test execution.
## 2026-04-04 - Testing MarkChaptersRemote skipSync edge case
**Learning:** MangaDexPreferences nested method mocking requires explicit returns. When using MockK to mock a use case that might skip execution conditionally, using `coVerify(exactly = 0)` cleanly asserts that external dependencies (like `StatusHandler`) are bypassed correctly.
**Action:** When testing conditional skips, mock all preferences normally and use exactly = 0 for the verification step on the external calls.
## 2026-04-27 - Testing ModifyCategoryUseCase Preferences
**Learning:** When writing unit tests involving nested preference structures like `LibraryPreferences` in Nekomanga, setting values requires explicit MockK chains (e.g., `every { libraryPreferences.sortAscending().set(any()) } just runs`) because methods like `sortAscending()` return a generic `Preference` object instead of a direct primitive value.
**Action:** Always mock the intermediate `Preference` object and its `.set()` or `.get()` methods individually when testing domain logic that alters App preferences to prevent runtime test crashes.
## 2026-02-23 - [UpdateTrackChapterTest] **Learning:** Mocking simple data classes (`mockk<TrackItem>(relaxed = true)`) in Kotlin can lead to test fragility and code smells. **Action:** Instantiate real instances of simple data classes in tests instead of mocking them.

## 2026-05-18 - DeepLinkViewModel Testing DI **Learning:** When writing tests for classes in Nekomanga that use Injekt (like ViewModels), `InjektRegistrar` cannot be instantiated directly as it is an interface. Use `Injekt.addSingleton()` directly. **Action:** Next time setting up tests involving `Injekt`, just call `Injekt.addSingleton(mock)` directly instead of trying to override the `Injekt` singleton instance with `InjektRegistrar`.
