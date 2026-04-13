So how do we fix this?
In `SettingsScreen.kt`, `reset`:

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
        backstack.clear()
        backstack.add(Screens.Settings.Main())
    }
}
```

Wait, if we are in a deep link, do we want to support nested navigation, and popping one by one?
Wait, if `wasDeepLink` is true, and the user navigates, they probably expect to go back to the deep-linked screen, and then exit.
Wait! In `reset`'s original logic:
```kotlin
    if (wasDeepLink) {
        onBackPressed()
    } else {
...
```
This implies the intention was: "If it's a deep link, just exit entirely".
BUT if it exits entirely, it MUST stop intercepting the back press!
But how can it stop intercepting? By clearing the backstack.
Wait, if it clears the backstack, `NavDisplay` might crash, because it usually expects `size > 0`.
Let's verify if `NavDisplay` can have an empty back stack.
Wait, `SettingsScreen`'s back stack is completely destroyed if `MainScreen` pops it.
But if `MainScreen` calls `onBackPressedDispatcher?.onBackPressed()`, the activity finishes.
The problem is that calling `onBackPressedDispatcher?.onBackPressed()` RE-EVALUATES the currently active back handlers!
And since `backStack.size > 1` (or even if it's 1? Wait!), wait!

Does `NavDisplay` intercept when `backStack.size == 1`?
Let's check where `backstack` size comes from.
```
       at androidx.compose.runtime.snapshots.SnapshotStateList.size(SnapshotStateList.android.kt:36)
       at androidx.navigation3.runtime.NavBackStack.getSize(NavBackStack.java:163)
       at androidx.navigation3.runtime.NavBackStack.size(NavBackStack.java:49)
       at org.nekomanga.presentation.screens.MainScreenKt.MainScreen$lambda$16$lambda$15(MainScreen.kt:83)
```
Wait! Look at the stack trace top lines!
```
       at androidx.navigation3.runtime.NavBackStack.size(NavBackStack.java:49)
       at org.nekomanga.presentation.screens.MainScreenKt.MainScreen$lambda$16$lambda$15(MainScreen.kt:83)
```
In `MainScreen.kt:83`:
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
Wait! It calls `onBackPressedDispatcher?.onBackPressed()` when `MainScreen`'s `backStack.size` == 1.
And then the dispatcher calls `NavDisplay`'s onBack again!
Wait. If `MainScreen`'s `backStack.size` is 1, and we press back, why does `SettingsScreen`'s `NavDisplay` intercept?
Wait, if `SettingsScreen`'s `backStack.size` is ALSO > 1!
Wait, but if it was a deep link, does `SettingsScreen`'s `backStack.size` start at 1? Yes.
If it starts at 1, `SettingsScreen`'s `NavDisplay` should NOT intercept!
BUT wait! In `SettingsScreen`, look at `NavDisplay` for `isTablet`:
```kotlin
                onBack = {
                    reset(
                        backstack = backStack,
                        wasDeepLink = deepLink != null,
                        onBackPressed = onBackPressed,
                    )
                },
```
Wait! In `navigation3`, `NavDisplay` MIGHT intercept the back press unconditionally if it thinks it should, OR `navigation3`'s `NavDisplay` intercepts back if `backstack.size > 1`.
Wait! If `NavDisplay` only intercepts if `backstack.size > 1`, then the user MUST have navigated to a sub-screen!
BUT wait, look at `SettingsScreen`'s `onNavigationIconClick`:
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
Wait, `onNavigationIconClick` is triggered when the user clicks the "Back" arrow in the top app bar!
Ah! The user clicked the back arrow!
When they click the back arrow, it calls `reset`!
```kotlin
                                    reset(backStack, wasDeepLink, onBackPressed)
```
If `wasDeepLink` is true, `reset` calls `onBackPressed()`.
`onBackPressed()` is `MainScreen`'s `goBack`.
`MainScreen`'s `goBack` checks `backStack.size > 1`. If `MainScreen`'s `backStack.size == 1` (it's the only screen because it was a deep link), it calls `onBackPressedDispatcher?.onBackPressed()`.
Then the dispatcher triggers the hardware back press!
The hardware back press is intercepted by `SettingsScreen`'s `NavDisplay` (if `SettingsScreen`'s `backStack.size > 1`), OR `MainScreen`'s `NavDisplay` (if `MainScreen`'s `backStack.size > 1`), OR `NavDisplay` handles back by calling `onBack`?
Wait! If the hardware back press is intercepted by `SettingsScreen`'s `NavDisplay`, it calls `onBack` in `SettingsScreen`!
Which calls `reset`!
Which calls `onBackPressed()`!
Which calls `goBack`!
Which calls `onBackPressedDispatcher?.onBackPressed()`!
Which is intercepted by `SettingsScreen`'s `NavDisplay`!
Infinite loop!
