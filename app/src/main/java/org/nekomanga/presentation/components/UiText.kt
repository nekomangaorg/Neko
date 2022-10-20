package org.nekomanga.presentation.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class UiText {

    data class String(val str: kotlin.String) : UiText()

    class StringResource(@StringRes val resourceId: Int, vararg val args: Any) : UiText()

    @Composable
    fun asString(): kotlin.String {
        return when (this) {
            is String -> str
            is StringResource -> stringResource(id = resourceId, formatArgs = args)
        }
    }
}
