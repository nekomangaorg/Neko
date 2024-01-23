package eu.kanade.tachiyomi.ui.reader.model

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.source.model.Page
import java.io.InputStream

open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    mangaDexChapterId: String = "",
    var stream: (() -> InputStream)? = null,
    var bg: Drawable? = null,
    var bgType: Int? = null,
) : Page(index, url, imageUrl, mangaDexChapterId, null) {

    /** Value to check if this page is used to as if it was too wide */
    var shiftedPage: Boolean = false

    /**
     * Value to check if a page is can be doubled up, but can't because the next page is too wide
     */
    var isolatedPage: Boolean = false
    var firstHalf: Boolean? = null
    var longPage: Boolean? = null
    var endPageConfidence: Int? = null
    var startPageConfidence: Int? = null
    open lateinit var chapter: ReaderChapter

    /** Value to check if a page is too wide to be doubled up */
    var fullPage: Boolean? = null
        set(value) {
            field = value
            longPage = value
            if (value == true) shiftedPage = false
        }

    val alonePage: Boolean
        get() = fullPage == true || isolatedPage

    val isEndPage
        get() = endPageConfidence?.let { it > 0 && it > (startPageConfidence ?: 0) }

    val isStartPage
        get() = startPageConfidence?.let { it > 0 && it > (endPageConfidence ?: 0) }

    fun isFromSamePage(page: ReaderPage): Boolean =
        index == page.index && chapter.chapter.id == page.chapter.chapter.id
}
