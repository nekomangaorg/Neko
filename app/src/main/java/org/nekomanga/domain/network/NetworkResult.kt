package org.nekomanga.domain.network

import androidx.annotation.StringRes

/**
 * Wrapper for Error Results
 */

sealed interface ResultError {
    data class Generic(val errorString: String = "", @StringRes val errorRes: Int = -1) : ResultError
    data class HttpError(val httpCode: Int, val message: String) : ResultError
}



