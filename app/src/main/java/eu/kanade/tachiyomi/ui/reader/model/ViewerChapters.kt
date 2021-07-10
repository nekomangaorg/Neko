package eu.kanade.tachiyomi.ui.reader.model

import com.elvishew.xlog.XLog

data class ViewerChapters(
    val currChapter: ReaderChapter,
    val prevChapter: ReaderChapter?,
    val nextChapter: ReaderChapter?,
) {

    fun ref() {
        XLog.d("ref viewer chapters")
        currChapter.ref()
        prevChapter?.ref()
        nextChapter?.ref()
    }

    fun unref() {
        XLog.d("unref viewer chapters")
        currChapter.unref()
        prevChapter?.unref()
        nextChapter?.unref()
    }
}
