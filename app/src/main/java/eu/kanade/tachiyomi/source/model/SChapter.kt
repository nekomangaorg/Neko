package eu.kanade.tachiyomi.source.model

import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import java.io.Serializable

interface SChapter : Serializable {

    var url: String

    var name: String

    var date_upload: Long

    var chapter_number: Float

    var scanlator: String?

    fun copyFrom(other: SChapter) {
        name = other.name
        url = other.url
        date_upload = other.date_upload
        chapter_number = other.chapter_number
        scanlator = other.scanlator
    }

    fun toChapter(): ChapterImpl {
        return ChapterImpl().apply {
            name = this@SChapter.name
            url = this@SChapter.url
            date_upload = this@SChapter.date_upload
            chapter_number = this@SChapter.chapter_number
            scanlator = this@SChapter.scanlator
        }
    }

    companion object {
        fun create(): SChapter {
            return SChapterImpl()
        }
    }
}
