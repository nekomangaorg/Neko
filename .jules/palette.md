## 2024-05-23 - [Accessibility in Dynamic Button Blocks]
**Learning:** In `ButtonBlock.kt`, buttons can have their text labels hidden via `hideText` or be initialized with empty text (like the Favorite button). This creates icon-only buttons that are inaccessible to screen readers.
**Action:** When designing data-driven button components, always include an optional `contentDescription` field in the data model (e.g., `ActionButtonData`) to provide a fallback accessible label when the visual text is hidden or empty.

## 2024-05-23 - [Accessibility in Icon-only Buttons]
**Learning:** In Jetpack Compose UI components, icon-only `IconButton`s often leave the `contentDescription` of the inner `Icon` as `null` (e.g., `Icons.Default.MoreVert`). This renders the buttons inaccessible to screen readers like TalkBack.
**Action:** When working on Compose UI, always ensure that `Icon` components inside `IconButton`s have a valid `contentDescription` using an appropriate string resource (e.g., `stringResource(id = R.string.options)`) instead of `null`.

## 2026-03-08 - [Missing ContentDescription on IconButtons]
**Learning:** Found an accessibility issue: missing `contentDescription` on some icon-only IconButtons in Jetpack Compose UI components. For instance, `org.nekomanga.presentation.screens.library.CategorySortButtons.kt` has multiple icon-only `IconButton`s containing `Icon` elements with `contentDescription = null`. This violates accessibility guidelines because screen readers will not announce anything useful when navigating to these buttons.
**Action:** Always provide a valid localized string resource for the `contentDescription` of `Icon` components when they act as the sole visual element inside an interactive component like an `IconButton`.
