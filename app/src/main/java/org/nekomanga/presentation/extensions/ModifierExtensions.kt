package org.nekomanga.presentation.extensions

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** Allows a conditional to be checked to apply a modifier */
fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        modifier.invoke(this)
    } else {
        this
    }
}

/**
 * For TextField, the provided [action] will be invoked when physical enter key is pressed.
 *
 * Naturally, the TextField should be set to single line only.
 */
fun Modifier.runOnEnterKeyPressed(action: () -> Unit): Modifier =
    this.onPreviewKeyEvent {
        when (it.key) {
            Key.Enter,
            Key.NumPadEnter -> {
                action()
                true
            }
            else -> false
        }
    }

/**
 * Clears focus and hides the software keyboard when a tap gesture occurs that is not consumed by
 * child composables.
 */
@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return this.pointerInput(focusManager, keyboardController) {
        detectTapGestures {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
}
