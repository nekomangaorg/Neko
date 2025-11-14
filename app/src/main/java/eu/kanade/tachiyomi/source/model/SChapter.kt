package eu.kanade.tachiyomi.source.model

import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import java.io.Serializable
import org.nekomanga.constants.Constants

interface SChapter : Serializable {

    var url: String

    var name: String

    var vol: String

    var chapter_txt: String

    var chapter_title: String

    var date_upload: Long

    var chapter_number: Float

    var scanlator: String?

    var uploader: String?

    var language: String?

    var isUnavailable: Boolean

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
        uploader = other.uploader
        isUnavailable = other.isUnavailable
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
            uploader = this@SChapter.uploader
        }
    }

    companion object {
        fun create(): SChapter {
            return SChapterImpl()
        }
    }
}

fun SChapter.isLocalSource() =
    this.scanlator?.equals(Constants.LOCAL_SOURCE) == true && this.isUnavailable

fun SChapter.isMergedChapter() = MergeType.containsMergeSourceName(this.scanlator)

fun SChapter.isMergedChapterOfType(mergeType: MergeType) =
    this.scanlator?.contains(MergeType.getMergeTypeName(mergeType)) == true

fun SChapter.getHttpSource(sourceManager: SourceManager): HttpSource {
    val mergeType = MergeType.getMergeTypeFromName(this.scanlator)
    return when (mergeType == null) {
        true -> sourceManager.mangaDex
        false -> MergeType.getSource(mergeType, sourceManager)
    }
}
