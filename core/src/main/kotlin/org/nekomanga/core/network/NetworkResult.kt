package org.nekomanga.domain.network

import android.content.Context
import androidx.annotation.StringRes

/**
 * Wrapper for Error Results
 */

sealed interface ResultError {
    data class Generic(val errorString: String = "", @StringRes val errorRes: Int = -1) : ResultError
    data class HttpError(val httpCode: Int, val message: String) : ResultError
}

fun ResultError.message(context: Context): String {
    return when (this) {
        is ResultError.HttpError -> this.message
        is ResultError.Generic -> when (errorRes == -1) {
            true -> errorString
            false -> context.getString(errorRes)
        }
    }
}

fun ResultError.message(): String {
    return when (this) {
        is ResultError.HttpError -> this.message
        is ResultError.Generic -> errorString
    }
}
