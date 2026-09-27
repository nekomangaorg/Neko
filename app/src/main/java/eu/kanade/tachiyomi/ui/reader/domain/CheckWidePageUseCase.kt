package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.util.system.ImageUtil
import okio.buffer
import okio.source

/**
 * Pure domain interactor that inspects image dimensions and determines whether a page is a wide
 * two-page spread (width > height) for double-page isolation and split-page handling.
 */
class CheckWidePageUseCase {

    fun isWidePage(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        return width > height
    }

    operator fun invoke(page: ReaderPage, width: Int, height: Int): Boolean {
        val isWide = isWidePage(width, height)
        if (isWide) {
            page.fullPage = true
            page.longPage = true
        }
        return isWide
    }

    operator fun invoke(page: ReaderPage): Boolean {
        val streamFn = page.stream ?: return false
        return try {
            streamFn().source().buffer().use {
                val options = ImageUtil.extractImageOptions(it)
                invoke(page, options.outWidth, options.outHeight)
            }
        } catch (_: Exception) {
            false
        }
    }
}
