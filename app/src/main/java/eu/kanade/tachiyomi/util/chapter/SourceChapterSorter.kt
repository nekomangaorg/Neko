package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.utils.MdLang
import kotlin.math.floor

/** This attempts to create a smart source order used when a manga is merged */
fun reorderChapters(sourceChapters: List<SChapter>, manga: Manga): List<SChapter> {
    if (sourceChapters.all { !it.isMergedChapter() }) {
        return sourceChapters
    }

    // mangalife tends to not include a volume number for manga
    val sorter =
        if (manga.lang_flag != null && MdLang.fromIsoCode(manga.lang_flag!!) == MdLang.JAPANESE) {
            compareByDescending<SChapter> { getChapterNum(it) == null }
                .thenByDescending { getChapterNum(it) }
        } else {
            compareByDescending<SChapter> { getVolumeNum(it) == null }
                .thenByDescending { getVolumeNum(it) }
                .thenByDescending { getChapterNum(it) }
        }

    return sourceChapters.sortedWith(sorter)
}

fun getChapterNum(chapter: SChapter): Float? {
    return when (chapter.name.contains("oneshot", true) && !chapter.isMergedChapter()) {
        true -> 0f
        false -> {
            val txt = chapter.chapter_txt
            txt.subStringfloatOrNull("Ch.")
                ?: txt.subStringfloatOrNull("Chp.")
                ?: txt.subStringfloatOrNull("Chapter")
        }
    }
}

private fun String.subStringfloatOrNull(delimiter: String): Float? {
    return this.substringAfter(delimiter).toFloatOrNull()
}

fun getChapterNumInt(chapter: SChapter): Int? {
    val float = getChapterNum(chapter)
    return when (float != null) {
        true -> floor(float).toInt()
        else -> null
    }
}

fun getVolumeNum(chapter: SChapter): Int? {
    return chapter.vol.toIntOrNull()
}
