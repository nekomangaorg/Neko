package org.nekomanga.presentation.components

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

sealed class UiText {
    data class String(val str: kotlin.String) : UiText()

    class PluralsResource(
        @param:PluralsRes val resourceId: Int,
        val count: Int,
        vararg val args: Any,
    ) : UiText() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PluralsResource
            return resourceId == other.resourceId &&
                count == other.count &&
                args.contentEquals(other.args)
        }

        override fun hashCode(): Int {
            var result = resourceId
            result = 31 * result + count
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    class StringResource(@param:StringRes val resourceId: Int, vararg val args: Any) : UiText() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as StringResource
            return resourceId == other.resourceId && args.contentEquals(other.args)
        }

        override fun hashCode(): Int {
            var result = resourceId
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

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
