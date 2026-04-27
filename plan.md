1. Extract logic from `getFormattedBuildTime()` inside `AboutViewModel.kt` into `GetFormattedBuildTimeUseCase`.
2. Add `GetFormattedBuildTimeUseCase` to `AppModule.kt` DI container.
3. Update `AboutViewModel.kt` to inject and use `GetFormattedBuildTimeUseCase`.
4. Run tests and static analysis.
5. Create a dedicated test `GetFormattedBuildTimeUseCaseTest`.
