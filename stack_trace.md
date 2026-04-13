```
       at org.nekomanga.presentation.screens.SettingsScreenKt.reset(SettingsScreen.kt:534)
       at org.nekomanga.presentation.screens.SettingsScreenKt.SettingsScreen$lambda$91$lambda$90(SettingsScreen.kt:494)
       at androidx.navigation3.ui.NavDisplayKt__NavDisplayKt.NavDisplay$lambda$8$0$NavDisplayKt__NavDisplayKt(NavDisplayKt__NavDisplay.kt:562)
       at androidx.navigationevent.compose.ComposeNavigationEventHandler.onBackCompleted(NavigationEventHandler.kt:249)
       at androidx.navigationevent.NavigationEventHandler.doOnBackCompleted$navigationevent(NavigationEventHandler.kt:242)
       at androidx.navigationevent.NavigationEventProcessor.dispatchOnCompleted(NavigationEventProcessor.java:487)
       at androidx.navigationevent.NavigationEventDispatcher.dispatchOnCompleted$navigationevent(NavigationEventDispatcher.java:383)
       at androidx.navigationevent.NavigationEventInput.dispatchOnBackCompleted(NavigationEventInput.kt:227)
       at androidx.activity.OnBackPressedDispatcher$OnBackPressedEventInput.backCompleted(OnBackPressedDispatcher.java:303)
       at androidx.activity.OnBackPressedDispatcher.onBackPressed(OnBackPressedDispatcher.java:248)
       at org.nekomanga.presentation.screens.MainScreenKt.MainScreen$lambda$16$lambda$15(MainScreen.kt:86)
       at org.nekomanga.presentation.screens.SettingsScreenKt.reset(SettingsScreen.kt:534)
```

In `SettingsScreen.kt`:
Line 494 is `onBack` in `NavDisplay`:
```kotlin
            onBack = {
                reset(
                    backstack = backStack,
                    wasDeepLink = deepLink != null,
                    onBackPressed = onBackPressed,
                )
            },
```
So `NavDisplay` intercepts back and calls `onBack`.
This means `backStack.size > 1` inside `SettingsScreen`!

Wait, how could `backStack.size` be > 1 if it was a deep link?
If it's a deep link, the initial stack is `Screens.Settings.Advanced()`.
Maybe the user then navigates inside the deep link?
Like from `Advanced` to `Appearance`?
Wait, if they navigate, then `backstack.size` becomes 2.
Then they press back. `NavDisplay` intercepts and calls `onBack`.
`reset` is called with `wasDeepLink = true`.
`reset` calls `onBackPressed()`.
But `onBackPressed()` is passed from `MainScreen`, which is `goBack`:
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
If `MainScreen`'s `backStack.size` is 1, `goBack` calls `onBackPressedDispatcher?.onBackPressed()`.
But wait! `SettingsScreen`'s `backstack.size` is STILL 2!
Because `reset` DID NOT pop the `SettingsScreen`'s `backStack`!
Since `SettingsScreen`'s `backStack.size` is still 2, `SettingsScreen`'s `NavDisplay` STILL intercepts the back press!
So `onBackPressedDispatcher?.onBackPressed()` delegates to `SettingsScreen`'s `NavDisplay` again!
And it calls `onBack` again. Which calls `reset` again. Which calls `onBackPressed()` again. Which calls `onBackPressedDispatcher?.onBackPressed()` again!
Infinite loop!
