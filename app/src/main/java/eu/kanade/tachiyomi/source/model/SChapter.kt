package eu.kanade.tachiyomi.source.model

import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.merged.mangalife.MangaLife
import java.io.Serializable
import tachiyomi.source.model.ChapterInfo

interface SChapter : Serializable {

    var url: String

    var name: String

    var vol: String

    var chapter_txt: String

    var chapter_title: String

    var date_upload: Long

    var chapter_number: Float

    var scanlator: String?

    var language: String?

    // chapter id from mangadex
    var mangadex_chapter_id: String

    var old_mangadex_id: String?

    fun chapterLog(): String {
        return "$name - $scanlator"
    }

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
        language = other.language
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

fun SChapter.isLegacyMergedChapter() = this.scanlator?.equals(MangaLife.oldName) == true

fun SChapter.isMergedChapter() = MergeType.containsMergeSourceName(this.scanlator)

fun SChapter.isMergedChapterOfType(mergeType: MergeType) = MergeType.getMergeTypeName(mergeType) == this.scanlator

fun SChapter.getHttpSource(sourceManager: SourceManager): HttpSource {
    val mergeType = MergeType.getMergeTypeFromName(this.scanlator)
    return when (mergeType == null) {
        true -> sourceManager.mangaDex
        false -> MergeType.getSource(mergeType, sourceManager)
    }
}

fun SChapter.toChapterInfo(): ChapterInfo {
    return ChapterInfo(
        dateUpload = this.date_upload,
        key = this.url,
        name = this.name,
        number = this.chapter_number,
        scanlator = this.scanlator ?: "",
    )
}

fun ChapterInfo.toSChapter(): SChapter {
    val chapter = this
    return SChapter.create().apply {
        url = chapter.key
        name = chapter.name
        date_upload = chapter.dateUpload
        chapter_number = chapter.number
        scanlator = chapter.scanlator
    }
}
