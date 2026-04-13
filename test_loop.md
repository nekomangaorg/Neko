The issue is exactly this:
If `wasDeepLink` is true, `reset` calls `onBackPressed()` without popping the `backstack` of `SettingsScreen`.
If `SettingsScreen`'s `backstack.size > 1` (e.g. because of navigation inside Settings), `NavDisplay`'s back handler remains enabled.
When `onBackPressed()` triggers `MainScreen`'s `goBack()`, and `MainScreen`'s `backStack.size == 1` (which is true if Settings was deep-linked as the only screen), `goBack()` calls `onBackPressedDispatcher?.onBackPressed()`.
The dispatcher sees `SettingsScreen`'s back handler is still enabled, so it invokes it again.
This forms the infinite loop:
`NavDisplay.onBack` -> `reset()` -> `onBackPressed()` -> `MainScreen.goBack()` -> `onBackPressedDispatcher.onBackPressed()` -> `NavDisplay.onBack`

How to fix:
If `wasDeepLink` is true, what should happen on back?
If the user navigated deep inside Settings, shouldn't they go back one step at a time inside Settings?
Yes! They should pop the `SettingsScreen`'s backstack if its size > 1!
Wait, look at how `reset` is called from `NavDisplay` in phone mode:
```kotlin
            onBack = {
                reset(
                    backstack = backStack,
                    wasDeepLink = deepLink != null,
                    onBackPressed = onBackPressed,
                )
            },
```
Wait, if it's a `NavDisplay`, `onBack` is called when the user presses back.
For a normal screen, `NavDisplay` handles back by calling `onBack`. Usually `onBack = { backStack.removeLastOrNull() }`.
Why does `SettingsScreen` have this complex `reset` logic for `onBack`?
```kotlin
private fun reset(
    backstack: NavBackStack<NavKey>,
    wasDeepLink: Boolean,
    onBackPressed: () -> Unit,
) {
    if (wasDeepLink) {
        onBackPressed()
    } else {
        backstack.clear()
        backstack.add(Screens.Settings.Main())
    }
}
```
Wait, if `wasDeepLink` is true, the user is deep linked into some setting (e.g., `Screens.Settings.Advanced()`).
When they press back from `Advanced`, `SettingsScreen` wants to just exit the app (or go back to wherever the deep link came from).
BUT wait, if `backstack.size > 1` in `SettingsScreen` (e.g. they started at `Advanced`, then clicked another setting inside it?), what should happen?
Wait, if `wasDeepLink` is true, and they press back, it exits. BUT it fails to clear the backstack, so `NavDisplay` keeps intercepting.
If we want to exit, we shouldn't rely on `onBackPressedDispatcher?.onBackPressed()` if our back handler is still enabled.
Actually, if we want to exit, we should probably pop the `MainScreen`'s backstack or exit.
But to prevent the `NavDisplay` from intercepting again, we should clear the `SettingsScreen`'s `backStack` OR we shouldn't use `onBackPressedDispatcher?.onBackPressed()` to exit while inside the callback.
Actually, the standard way to fix this is to pop the `backStack` first, so the back handler becomes disabled, or we just handle popping correctly.
