## 2026-06-01 - Identifying unused drawables in multi-flavored projects
**Learning:** In projects with multiple build flavors (like Nekomanga), running the generic `lintDebug` task on the `app` module might fail due to ambiguity (e.g., `Task lintDebug is ambiguous`).
**Action:** When using lint to verify unused resources before deletion, target a specific flavor variant instead, such as `./gradlew :app:lintStandardDebug`, and parse its specific `lint-results` file.
