Let's see what happens if `wasDeepLink` is true. We should clear the `backstack` before calling `onBackPressed()`.
```kotlin
private fun reset(
    backstack: NavBackStack<NavKey>,
    wasDeepLink: Boolean,
    onBackPressed: () -> Unit,
) {
    if (wasDeepLink) {
        backstack.clear()
        onBackPressed()
    } else {
        backstack.clear()
        backstack.add(Screens.Settings.Main())
    }
}
```
If we clear the backstack, `NavDisplay`'s size becomes 0, so it stops intercepting the back press. But wait, `NavDisplay` requires at least one element in the backstack!
Let's check `NavDisplay`'s source code or documentation to see if it allows an empty `backStack`.
If `backstack.clear()` is called, and `NavDisplay` re-composes, it might crash if the backstack is empty.
Wait, in `reset`'s else branch:
```kotlin
        backstack.clear()
        backstack.add(Screens.Settings.Main())
```
It clears and then adds. So it doesn't leave it empty.

If `wasDeepLink` is true, we want to go back from `SettingsScreen`.
But `SettingsScreen` is shown by `MainScreen`'s `NavDisplay`.
When `onBackPressed()` is called, `MainScreen`'s `goBack` executes:
```kotlin
    val goBack = {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            onBackPressedDispatcher?.onBackPressed()
        }
        Unit
    }
```
If `MainScreen`'s `backStack` has `Screens.Settings.Main()` (or something else), `removeLastOrNull()` removes it, and `MainScreen` shows whatever is underneath. When `MainScreen`'s backStack removes it, `SettingsScreen` is disposed, so its `NavDisplay` is gone.
But what if `MainScreen`'s `backStack.size == 1`?
Then `goBack` calls `onBackPressedDispatcher?.onBackPressed()`.
If `SettingsScreen`'s `NavDisplay` back stack size is > 1 (which intercepts), then the dispatcher will invoke the innermost back callback, which is `SettingsScreen`'s `NavDisplay`'s `onBack`! This is exactly the infinite loop.

Wait, why does `SettingsScreen`'s `NavDisplay` intercept?
Because `backstack.size > 1` inside `SettingsScreen`!
Wait, if `SettingsScreen` only has ONE item in `backStack` (e.g., deep link to `Advanced`), `NavDisplay` DOES NOT intercept the back press.
Let's check `androidx.navigation3.ui.NavDisplay` source code to see when it intercepts back presses.
If `androidx.navigation3.ui.NavDisplay` only intercepts when `backStack.size > 1`, then the issue is when `SettingsScreen`'s `backstack.size > 1`.
If `SettingsScreen`'s `backstack.size > 1`, it intercepts.
When `NavDisplay` handles back, it calls `onBack`.
`SettingsScreen`'s `onBack` calls `reset`:
```kotlin
            onBack = {
                reset(
                    backstack = backStack,
                    wasDeepLink = deepLink != null,
                    onBackPressed = onBackPressed,
                )
            },
```
Wait!
If `wasDeepLink` is false, `reset` does:
```kotlin
        backstack.clear()
        backstack.add(Screens.Settings.Main())
```
This is because if you are not in a deep link, pressing back in `SettingsScreen`'s `NavDisplay` (when `backstack.size > 1`) should reset the `SettingsScreen` backstack to just `Screens.Settings.Main()`, instead of popping one by one!
Wait, really?
If you are in `General`, then click `Advanced` inside `General`, you are in `Main -> General -> Advanced`.
When you press back, `onBack` is called. It clears and adds `Main`. So it goes back to `Main`. It doesn't pop one by one!
But wait, `SettingsScreen` doesn't have nested navigation like that. The `Main` settings screen has a list of sub-screens. You click one, it goes to that sub-screen. So the backstack is `Main -> General`.
If you are in `General` and press back, `backstack.size` is 2. `NavDisplay` calls `onBack`.
`reset()` clears and adds `Main`. So `backstack` becomes `Main`. `NavDisplay` re-composes, `backstack.size` becomes 1. Back interception is disabled.
This works perfectly!

BUT, what if `wasDeepLink` is true?
If `wasDeepLink` is true, the initial `SettingsScreen` backstack is just `Screens.Settings.Advanced()`. So `backstack.size` is 1!
Wait, if `backstack.size` is 1, `NavDisplay` DOES NOT intercept the back press!
If `NavDisplay` doesn't intercept, then `SettingsScreen`'s `NavDisplay` doesn't handle the back press.
Instead, `MainScreen`'s `NavDisplay` intercepts it, if `MainScreen`'s `backStack.size > 1`.
If `MainScreen`'s `backStack.size == 1` (e.g. `MainScreen` was deep linked directly to `Settings`), then `MainScreen`'s `NavDisplay` DOES NOT intercept either!
Then the system `OnBackPressedDispatcher` handles it. The default behavior is to exit the app.
Wait. If `backstack.size == 1`, `NavDisplay` does NOT intercept. So how can there be an infinite loop?
The infinite loop happens when `reset` is called.
Who calls `reset`?
Let's check the stack trace!
