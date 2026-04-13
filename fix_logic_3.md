Wait, let's look at `SettingsScreen.kt` again.
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

Wait, if we do:
```kotlin
private fun reset(
    backstack: NavBackStack<NavKey>,
    wasDeepLink: Boolean,
    onBackPressed: () -> Unit,
) {
    if (wasDeepLink) {
        if (backstack.size > 1) {
            backstack.removeLastOrNull()
        } else {
            onBackPressed()
        }
    } else {
        if (backstack.size > 1) {
            backstack.clear()
            backstack.add(Screens.Settings.Main())
        } else {
            onBackPressed()
        }
    }
}
```
Is this exactly what we want?
If we are not deep-linked (we are coming from MainScreen, so `Screens.Settings.Main()` is the first item in the `backstack`).
If `backstack.size == 1` (we are on `Main`), and we press the Back arrow on `Main`, `reset` is called.
Since `backstack.size == 1`, it goes to the `else` block: `onBackPressed()`.
Wait, originally `reset` would do:
```kotlin
    } else {
        backstack.clear()
        backstack.add(Screens.Settings.Main())
    }
```
If `backstack.size == 1`, doing `clear()` and `add(Main)` doesn't actually change the stack size, it just re-adds `Main`.
But wait! Does `reset` ever get called when `backstack.size == 1` and `wasDeepLink` is false?
Let's check where `reset` is called from `NavDisplay`'s `onBack`:
```kotlin
            onBack = {
                reset(
                    backstack = backStack,
                    wasDeepLink = deepLink != null,
                    onBackPressed = onBackPressed,
                )
            },
```
`NavDisplay` only calls `onBack` if `backStack.size > 1`.
So `reset` is only called from `NavDisplay` when `backstack.size > 1`.
What about `onNavigationIconClick`?
In `SettingsMainScreen` (the first screen if not deep-linked):
```kotlin
                onNavigationIconClick = onBackPressed,
```
Wait, `SettingsMainScreen` directly calls `onBackPressed`! It DOES NOT call `reset`!
Ah! So `reset` is only called from `onNavigationIconClick` of sub-screens!
And sub-screens are only active when `backstack.size > 1` (or `backstack.size == 1` but `wasDeepLink == true`).
If `wasDeepLink == true`, we could be on a sub-screen but `backstack.size == 1`.
If `backstack.size == 1` and `wasDeepLink == true`, `reset` is called.
In the original code:
```kotlin
    if (wasDeepLink) {
        onBackPressed()
    }
```
This would call `onBackPressed()` which exits. This is correct!
If `wasDeepLink == true` and the user navigated deeper, `backstack.size == 2`.
In the original code:
```kotlin
    if (wasDeepLink) {
        onBackPressed()
    }
```
This calls `onBackPressed()`. `onBackPressed()` triggers hardware back. `NavDisplay` intercepts hardware back because `backstack.size == 2 > 1`.
`NavDisplay` calls `onBack`, which calls `reset`.
`reset` calls `onBackPressed()`. Loop!

So if we change `reset` to:
```kotlin
private fun reset(
    backstack: NavBackStack<NavKey>,
    wasDeepLink: Boolean,
    onBackPressed: () -> Unit,
) {
    if (backstack.size > 1) {
        if (wasDeepLink) {
            backstack.removeLastOrNull()
        } else {
            backstack.clear()
            backstack.add(Screens.Settings.Main())
        }
    } else {
        onBackPressed()
    }
}
```
Does this fix it and preserve intended behavior?
If `wasDeepLink == true` and `backstack.size == 1`, it goes to `else`, calls `onBackPressed()`. Correct, exits.
If `wasDeepLink == true` and `backstack.size == 2`, it calls `backstack.removeLastOrNull()`. Correct, goes back to the deep-linked screen without infinite loop!
If `wasDeepLink == false` and `backstack.size == 2`, it calls `backstack.clear(); backstack.add(Screens.Settings.Main())`. Correct, jumps back to Main settings.
Wait, if `wasDeepLink == false` and `backstack.size == 1`, it calls `onBackPressed()`. Which goes back to the app's `MainScreen`.
But wait! If `wasDeepLink == false`, we are in `Screens.Settings.Main()`. The `onNavigationIconClick` of `Main` already calls `onBackPressed` directly!
So `reset` won't even be called when `wasDeepLink == false` and `backstack.size == 1` (from the UI). But if it were called, it would correctly just call `onBackPressed()`.

This logic perfectly matches the required behavior and fixes the bug!
