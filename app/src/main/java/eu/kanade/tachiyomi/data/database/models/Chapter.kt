package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.model.SChapter
import java.io.Serializable

interface Chapter : SChapter, Serializable {

    var id: Long?

    var manga_id: Long?

    var read: Boolean

    var bookmark: Boolean

    var last_page_read: Int

    var pages_left: Int

    var date_fetch: Long

    var source_order: Int

    val isRecognizedNumber: Boolean
        get() = chapter_number >= 0f

    val isHeader: Boolean
        get() = id == Long.MIN_VALUE

    companion object {

        fun create(): Chapter = ChapterImpl().apply {
            chapter_number = -1f
        }

        fun createH(): Chapter = ChapterImpl().apply {
            id = Long.MIN_VALUE
            manga_id = null
        }
    }
}
