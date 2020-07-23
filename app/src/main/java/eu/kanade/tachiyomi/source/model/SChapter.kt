package eu.kanade.tachiyomi.source.model

import eu.kanade.tachiyomi.source.online.MergeSource
import java.io.Serializable
import kotlin.math.floor

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

    companion object {
        fun create(): SChapter {
            return SChapterImpl()
        }
    }
}

fun SChapter.isMergedChapter() = this.scanlator?.equals(MergeSource.name) ?: false

fun SChapter.getChapterNum(): Int? {
    val float = if (this.isMergedChapter()) {
        chapter_txt.toFloatOrNull()
    } else {
        if (this.name.contains("oneshot", true)) {
            0f
        } else {
            chapter_txt.substringAfter("Ch.").toFloatOrNull()
        }
    }
    float ?: return null
    return floor(float).toInt()
}

fun SChapter.getVolumeNum(): Int? {
    return if (this.isMergedChapter()) {
        vol.toIntOrNull()
    } else {
        vol.substringAfter("Vol.").toIntOrNull()
    }
}


