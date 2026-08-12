# Custom Rules

- **Do NOT auto commit anything**: Never automatically run `git commit`. Keep all modifications unstaged in the working tree so that the user can review them and commit them manually.
- **Always generate a commit message**: At the end of every task involving code modifications, always output a draft commit message matching conventional commit standards. The commit message header must not include a scope in parentheses (e.g., use `style: description` instead of `style(scope): description`).
- **Always import classes when possible**: Do not use fully qualified class names in type signatures or code references (e.g. use `viewModel: ReaderViewModel` instead of `viewModel: eu.kanade.tachiyomi.ui.reader.ReaderViewModel` by adding the corresponding `import` statement at the top of the file).
- **Always use Neko `Size` tokens**: In Jetpack Compose layouts, paddings, and dimension specifications, always use `org.nekomanga.presentation.theme.Size` tokens (e.g. `Size.small`, `Size.medium`, `Size.large`, `Size.extraLarge`) instead of arbitrary hardcoded `.dp` values whenever possible.
- **Always hoist Composable state**: Keep composables stateless and decoupled by hoisting state up to caller screens/containers, accepting plain data models / UI state and emitting events via lambda callbacks (e.g., `onClick`, `onValueChange`). Avoid coupling child composables directly to ViewModels.
- **Always use `collectAsStateWithLifecycle()`**: When collecting `StateFlow` or `Flow` streams inside Jetpack Compose, always use `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose` to ensure safe, lifecycle-bounded coroutine execution.
- **Always use `TimberKt` for logging**: Use Neko's structured `TimberKt` logging wrapper (`TimberKt.d { ... }`, `TimberKt.e(throwable) { ... }`) rather than `android.util.Log`, `println`, or raw `Timber`.
- **Never hardcode user-facing strings**: Always define user-facing text in `strings.xml` and reference them via `stringResource(R.string.xyz)` or `context.getString(R.string.xyz)`.
- **Always prefer Material 3 theme colors**: In Compose, prefer `MaterialTheme.colorScheme` tokens (e.g., `surface`, `primary`, `background`, `onSurfaceVariant`) over hardcoded color literals (`Color(0xFF...)` or `android.graphics.Color`).
- **Always enforce immutable UI state**: UI state models should be immutable `data class`es containing only `val` properties to ensure predictable Compose recomposition and prevent state mutation bugs.
- **Always format Kotlin code with `ktfmt`**: After making any Kotlin code modifications, format files with `./gradlew ktfmtFormat` to maintain repo code style.

