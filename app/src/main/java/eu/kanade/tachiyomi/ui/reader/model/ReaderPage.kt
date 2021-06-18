package eu.kanade.tachiyomi.ui.reader.model

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.source.model.Page
import java.io.InputStream

class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    mangaDexChapterId: String = "",
    var stream: (() -> InputStream)? = null,
    var bg: Drawable? = null,
    var bgType: Int? = null,
    /** Value to check if this page is used to as if it was too wide */
    var shiftedPage: Boolean = false,
    /** Value to check if a page is can be doubled up, but can't because the next page is too wide */
    var isolatedPage: Boolean = false,
) : Page(index, url, imageUrl, mangaDexChapterId, uri = null) {

    lateinit var chapter: ReaderChapter

    /** Value to check if a page is too wide to be doubled up */
    var fullPage: Boolean = false
        set(value) {
            field = value
            if (value) shiftedPage = false
        }
}
