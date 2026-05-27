1. **Update `DeepLinkViewModelTest.kt`**:
   - Change the import to `uy.kohesive.injekt.registry.default.DefaultRegistrar`
   - In `@Before fun setup()`, add `Injekt = DefaultRegistrar()` before calling `Injekt.addSingleton()`.
   - Remove the reflection code in `@After fun tearDown()`.
2. **Update `.jules/testpilot.md`**:
   - Replace the `2026-05-18 - DeepLinkViewModel Testing DI` entry with the updated text specifying the use of `DefaultRegistrar()`.
3. **Format and Verify**: Run `./gradlew ktfmtFormat` and `./gradlew app:testDebugUnitTest --tests "org.nekomanga.presentation.screens.deepLink.DeepLinkViewModelTest"` and then `./gradlew testDebugUnitTest`.
4. **Reply to comments**: Call `reply_to_pr_comments` with the necessary acknowledgments.
5. **Pre commit checks**: Complete pre commit steps to ensure proper testing, verification, review, and reflection are done.
6. **Submit PR**: Call `submit` to push changes to the pull request.
