package org.nekomanga.presentation.extensions

import androidx.compose.ui.Modifier

/** Allows a conditional to be checked to apply a modifier */
fun Modifier.conditional(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        modifier.invoke(this)
    } else {
        this
    }
}
