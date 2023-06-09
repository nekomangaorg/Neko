package eu.kanade.tachiyomi.ui.reader.model

import org.nekomanga.core.loggycat

data class ViewerChapters(
    val currChapter: ReaderChapter,
    val prevChapter: ReaderChapter?,
    val nextChapter: ReaderChapter?,
) {

    fun ref() {
        loggycat { "ref viewer chapters" }
        currChapter.ref()
        prevChapter?.ref()
        nextChapter?.ref()
    }

    fun unref() {
        loggycat { "unref viewer chapters" }
        currChapter.unref()
        prevChapter?.unref()
        nextChapter?.unref()
    }
}
