Wait, why does `SettingsScreen` intercept hardware back press if `backStack.size == 1`?
If `SettingsScreen` has `backStack.size == 1` (because it's the deep linked screen, like `Screens.Settings.Advanced()`), DOES `NavDisplay` intercept?
Wait, if we look at `NavDisplay` source code in `androidx.navigation3`, maybe we can find it.
But wait, if we look at `SettingsScreen`'s `reset`:
```kotlin
private fun reset(
    backstack: NavBackStack<NavKey>,
    wasDeepLink: Boolean,
    onBackPressed: () -> Unit,
) {
    if (wasDeepLink) {
        onBackPressed()
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
If we change it to just pop the stack when `wasDeepLink` is false, instead of clearing?
Wait, `SettingsScreen` wants to go back to `Screens.Settings.Main()` if you are not deep linked.
Wait, if you ARE deep linked, AND you navigated to another screen (e.g. `backStack.size > 1`), what should happen?
If you navigate in a deep linked screen, pressing back should probably pop the inner screen.
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
Wait, if `SettingsScreen`'s `backstack.size > 1`, `NavDisplay` intercepts it.
If `wasDeepLink` is true, and `backstack.size` is 2, it pops to 1. `NavDisplay` stops intercepting. Next back press goes to `MainScreen`'s `NavDisplay` or system.
But if `wasDeepLink` is true and `backstack.size == 1`? `NavDisplay` shouldn't intercept it anyway!
So `NavDisplay`'s `onBack` is NEVER called when `backstack.size == 1`!
Wait. If `NavDisplay`'s `onBack` is never called when `backstack.size == 1`, how does the infinite loop happen?

Ah! The user clicked the back arrow in the TopAppBar!
```kotlin
                    onNavigationIconClick =
                        if (isTablet && detailBackStack.size == 1) null
                        else {
                            {
                                if (isTablet && detailBackStack.size > 1) {
                                    detailBackStack.removeLastOrNull()
                                } else {
                                    reset(backStack, wasDeepLink, onBackPressed)
                                }
                            }
                        },
```
If the user clicks the TopAppBar back arrow, `reset` is called.
`reset` calls `onBackPressed()`. Which is `MainScreen.goBack`.
`MainScreen.goBack` calls `onBackPressedDispatcher?.onBackPressed()`.
This triggers a HARDWARE back press.
If `SettingsScreen`'s `backstack.size > 1`, `NavDisplay` intercepts it!
So if `backstack.size > 1` in `SettingsScreen`, and the user clicks the TopAppBar back arrow, it triggers a hardware back press. `NavDisplay` intercepts it, calling `onBack`.
`onBack` calls `reset`. `reset` calls `onBackPressed()`. Which triggers another hardware back press.
Which `NavDisplay` intercepts again!
This perfectly explains the infinite loop!

So the fix is to make `reset` pop the `backStack` if we shouldn't exit, or if we want to exit, we need to handle it without triggering a hardware back press that we intercept again.
Wait, if `wasDeepLink` is true, what SHOULD happen when we click the TopAppBar back arrow and `backstack.size > 1`?
We should pop `SettingsScreen`'s `backstack`!
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
Let's see if this fixes the infinite loop.
If `backstack.size > 1`, and user clicks TopAppBar back arrow, `reset` is called.
`reset` sees `backstack.size > 1`.
If `wasDeepLink` is true, it calls `backstack.removeLastOrNull()`. This removes the top screen. `NavDisplay` updates. No hardware back press is triggered! No infinite loop!
If `wasDeepLink` is false, it resets to `Screens.Settings.Main()`. `NavDisplay` updates. No hardware back press! No infinite loop!

What if `backstack.size == 1`?
Then `reset` calls `onBackPressed()`.
`onBackPressed()` is `MainScreen.goBack`.
`MainScreen.goBack` sees `MainScreen.backStack.size`.
If it's 1, it calls `onBackPressedDispatcher?.onBackPressed()`.
This triggers a hardware back press.
Since `SettingsScreen.backStack.size == 1`, `SettingsScreen`'s `NavDisplay` DOES NOT intercept it!
So the hardware back press finishes the activity or goes to the system! No infinite loop!

This perfectly solves the issue!
