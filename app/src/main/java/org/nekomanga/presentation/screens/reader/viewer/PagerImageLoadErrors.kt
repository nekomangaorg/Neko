package org.nekomanga.presentation.screens.reader.viewer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import org.nekomanga.logging.TimberKt

/**
 * Images of one pager item that Coil failed to load. A page that fails to decode stays READY, so
 * its status never shows the failure and the error overlay reads it from here.
 */
@Stable
class PagerImageLoadErrors {
    private val messages = mutableStateMapOf<ReaderPage, String>()

    val hasError: Boolean
        get() = messages.isNotEmpty()

    val message: String?
        get() = messages.values.firstOrNull()

    fun onError(page: ReaderPage, throwable: Throwable) {
        TimberKt.e(throwable) { "Failed to load pager image for page ${page.number}" }
        messages[page] = throwable.message ?: throwable.javaClass.simpleName
    }

    fun onSuccess(page: ReaderPage) {
        messages.remove(page)
    }

    fun clear() {
        messages.clear()
    }
}
