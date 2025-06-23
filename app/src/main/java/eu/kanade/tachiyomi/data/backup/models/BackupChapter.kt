package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupChapter(
    // in 1.x some of these values have different names
    // url is called key in 1.x
    @ProtoNumber(1) var url: String,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var scanlator: String? = null,
    @ProtoNumber(4) var read: Boolean = false,
    @ProtoNumber(5) var bookmark: Boolean = false,
    // lastPageRead is called progress in 1.x
    @ProtoNumber(6) var lastPageRead: Int = 0,
    @ProtoNumber(7) var dateFetch: Long = 0,
    @ProtoNumber(8) var dateUpload: Long = 0,
    // chapterNumber is called number is 1.x
    @ProtoNumber(9) var chapterNumber: Float = 0F,
    @ProtoNumber(10) var sourceOrder: Int = 0,

    // J2K specific values
    @ProtoNumber(800) var pagesLeft: Int = 0,
    // Neko Values
    @ProtoNumber(905) var uploader: String? = null,
    @ProtoNumber(906) var isUnavailable: Boolean = false,
) {
    fun toChapterImpl(): ChapterImpl {
        return ChapterImpl().apply {
            url = this@BackupChapter.url
            name = this@BackupChapter.name
            chapter_number = this@BackupChapter.chapterNumber
            scanlator = this@BackupChapter.scanlator
            uploader = this@BackupChapter.uploader
            read = this@BackupChapter.read
            bookmark = this@BackupChapter.bookmark
            last_page_read = this@BackupChapter.lastPageRead
            date_fetch = this@BackupChapter.dateFetch
            date_upload = this@BackupChapter.dateUpload
            source_order = this@BackupChapter.sourceOrder
            pages_left = this@BackupChapter.pagesLeft
            isUnavailable = this@BackupChapter.isUnavailable
        }
    }

    companion object {
        fun copyFrom(chapter: Chapter): BackupChapter {
            return BackupChapter(
                url = chapter.url,
                name = chapter.name,
                chapterNumber = chapter.chapter_number,
                scanlator = chapter.scanlator,
                uploader = chapter.uploader,
                read = chapter.read,
                bookmark = chapter.bookmark,
                lastPageRead = chapter.last_page_read,
                dateFetch = chapter.date_fetch,
                dateUpload = chapter.date_upload,
                sourceOrder = chapter.source_order,
                pagesLeft = chapter.pages_left,
            )
        }
    }
}
