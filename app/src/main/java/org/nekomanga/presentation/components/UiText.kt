package org.nekomanga.presentation.components

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

sealed class UiText {

    data class String(val str: kotlin.String) : UiText()

    class PluralsResource(@PluralsRes val resourceId: Int, val count: Int, vararg val args: Any) :
        UiText()

    class StringResource(@StringRes val resourceId: Int, vararg val args: Any) : UiText()

    @Composable
    fun asString(): kotlin.String {
        return when (this) {
            is String -> str
            is StringResource -> stringResource(id = resourceId, formatArgs = args)
            is PluralsResource ->
                pluralStringResource(id = resourceId, count = count, formatArgs = args)
        }
    }
}
