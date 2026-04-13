## 2026-03-30 - Extracting Library Filtering Logic 
**Learning:** The project relies heavily on `mockk` for testing domain layers, and UI logic often tightly couples domain models with state objects before filtering. Use Cases are typically registered as singletons in `AppModule.kt` using `addSingleton()` and injected via `Injekt.get()`. 
**Action:** Always create a `mockk` test file matching the usecase name and update `AppModule.kt` to bind the new usecase for DI.
## 2026-04-13 - Extracting Chapter Filter Text Logic 
**Learning:** Use Cases shouldn't be singletons if they take `Context` or require it injected unless using Dagger/Hilt correctly; alternatively, simply instantiate them where needed like `GetChapterFilterText(context)` or pass `Context` as an argument if they are kept as lightweight helper functions. 
**Action:** If a Use Case needs Context and DI is complex, just instantiate it with the context where needed or use Hilt's `@ApplicationContext`.
## 2026-04-13 - Extracting MarkPreviousChaptersUseCase
**Learning:** When trying to instantiate data classes like `Chapter` or `ChapterItem` for unit testing, the implementation classes (`ChapterImpl`) can be cumbersome. The instruction "Use Mockk" implies that we should create mocks via MockK (`mockk<Chapter>()`) instead of manually instantiating or using `apply`.
**Action:** When asked to "use MockK" for unit tests on domain layer extractions, mock the entity directly using `mockk<Type>()` and stub the fields using `every { field } returns ...` rather than spending cycles trying to find the appropriate `Impl.create().apply {}` equivalent.
