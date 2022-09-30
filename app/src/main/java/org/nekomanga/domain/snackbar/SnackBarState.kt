package org.nekomanga.domain.snackbar

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration

data class SnackbarState(
    val message: String? = null,
    @StringRes val messageRes: Int? = null,
    @StringRes val fieldRes: Int? = null,
    @StringRes val prefixRes: Int? = null,
    val actionLabel: String? = null,
    val snackbarDuration: SnackbarDuration = SnackbarDuration.Short,
    @StringRes val actionLabelRes: Int? = null,
    val action: (() -> Unit)? = null,
    val dismissAction: (() -> Unit)? = null,
) {
    fun getFormattedMessage(context: Context): String {
        val prefix = when (prefixRes == null) {
            true -> ""
            false -> context.getString(prefixRes)
        }

        val message = when {
            this.message != null && this.messageRes != null && this.fieldRes != null -> context.getString(this.messageRes, context.getString(this.fieldRes)) + this.message
            this.message != null && this.messageRes != null -> context.getString(this.messageRes, this.message)
            this.messageRes != null && this.fieldRes != null -> context.getString(this.messageRes, context.getString(this.fieldRes))
            this.message != null -> this.message
            this.messageRes != null -> context.getString(this.messageRes)
            else -> ""
        }

        return prefix + message
    }

    fun getFormattedActionLabel(context: Context): String? {
        return when {
            this.actionLabel != null -> this.actionLabel
            this.actionLabelRes != null -> context.getString(this.actionLabelRes)
            else -> null
        }
    }
}
