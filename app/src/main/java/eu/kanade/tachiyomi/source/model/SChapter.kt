package eu.kanade.tachiyomi.source.model

import java.io.Serializable

interface SChapter : Serializable {

    var url: String

    var name: String

    var vol: String

    var chapter_txt: String

    var chapter_title: String

    var date_upload: Long

    var chapter_number: Float

    var scanlator: String?

    // chapter id from mangadex
    var mangadex_chapter_id: String

    fun copyFrom(other: SChapter) {
        name = other.name
        vol = other.vol
        chapter_title = other.chapter_title
        chapter_txt = other.chapter_txt
        url = other.url
        date_upload = other.date_upload
        chapter_number = other.chapter_number
        scanlator = other.scanlator
        mangadex_chapter_id = other.mangadex_chapter_id
    }

    companion object {
        fun create(): SChapter {
            return SChapterImpl()
        }
    }
}
