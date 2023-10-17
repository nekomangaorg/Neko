package eu.kanade.tachiyomi.ui.reader.model

import org.nekomanga.logging.TimberKt

data class ViewerChapters(
    val currChapter: ReaderChapter,
    val prevChapter: ReaderChapter?,
    val nextChapter: ReaderChapter?,
) {

    fun ref() {
        TimberKt.d { "ref viewer chapters" }
        currChapter.ref()
        prevChapter?.ref()
        nextChapter?.ref()
    }

    fun unref() {
        TimberKt.d { "unref viewer chapters" }
        currChapter.unref()
        prevChapter?.unref()
        nextChapter?.unref()
    }
}
