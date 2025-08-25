package org.nekomanga.presentation.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent

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
