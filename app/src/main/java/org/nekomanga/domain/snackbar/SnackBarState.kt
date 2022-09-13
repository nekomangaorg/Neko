package org.nekomanga.domain.snackbar

import android.content.Context
import androidx.annotation.StringRes

data class SnackbarState(
    val message: String? = null,
    @StringRes val messageRes: Int? = null,
    @StringRes val fieldRes: Int? = null,
    val actionLabel: String? = null,
    @StringRes val actionLabelRes: Int? = null,
    val action: (() -> Unit)? = null,
    val dismissAction: (() -> Unit)? = null,
) {
    fun getFormattedMessage(context: Context): String {
        return when {
            this.message != null && this.messageRes != null && this.fieldRes != null -> context.getString(this.messageRes, context.getString(this.fieldRes)) + this.message
            this.message != null && this.messageRes != null -> context.getString(this.messageRes, this.message)
            this.messageRes != null && this.fieldRes != null -> context.getString(this.messageRes, context.getString(this.fieldRes))
            this.message != null -> this.message
            this.messageRes != null -> context.getString(this.messageRes)
            else -> ""
        }
    }

    fun getFormattedActionLabel(context: Context): String? {
        return when {
            this.actionLabel != null -> this.actionLabel
            this.actionLabelRes != null -> context.getString(this.actionLabelRes)
            else -> null
        }
    }
}
