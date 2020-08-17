package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isMergedChapter
import kotlin.math.floor

/**This method dedupes merged manga chapters.  If the manga is not merged then it just returns the direct Dex chapter list
 * The follow fringe cases won't be deduped
 * MangaDex manga that has chapter number reset every volume
 * Manhua/Manhwa that have multiple seasons on MangaDex but no seasons on MergedSource
 */
fun deduplicateChapters(dbChapters: List<SChapter>, sourceChapters: List<SChapter>, manga: Manga): List<SChapter> {
    if (manga.merge_manga_url == null) {
        return sourceChapters
    }
    val partition = sourceChapters.partition { !it.isMergedChapter() }
    val dexChapters = partition.first.toMutableList()
    val mergedChapters = partition.second

    val isManga = "jp" == manga.lang_flag

    var dexMap: Map<Int?, List<Float?>>? = null
    var only1VolNoVol: Boolean = false

    if (isManga.not()) {
        dexMap = dexChapters.groupBy(keySelector = { getVolumeNum(it) }, valueTransform = { getChapterNum(it) })
        only1VolNoVol = dexChapters.all { getVolumeNum(it) == 1 } && mergedChapters.all { getVolumeNum(it) == null }
    }

    var dexSet: HashSet<Float?>? = null
    if (isManga || only1VolNoVol) {
        dexSet = dexChapters.map { getChapterNum(it) }.toHashSet()
    }

    mergedChapters.forEach { sChapter ->
        getChapterNum(sChapter)?.let { chpNum ->
            if (isManga || only1VolNoVol) {
                if (!dexSet!!.contains(chpNum)) {
                    dexChapters.add(sChapter)
                } else {
                }
            } else {
                val volume = dexMap!![getVolumeNum(sChapter)]
                if (volume == null) {
                    dexChapters.add(sChapter)
                } else {
                    dexMap!![getVolumeNum(sChapter)]?.let {
                        if (it.contains(chpNum).not()) {
                            dexChapters.add(sChapter)
                        }
                    }
                }
            }
        }
    }
    val sorter = when (isManga || only1VolNoVol) {
        true -> compareByDescending { getChapterNum(it) }
        false -> compareByDescending<SChapter> { getVolumeNum(it) }.thenByDescending { getChapterNum(it) }
    }

    return dexChapters.sortedWith(sorter)
}

fun getChapterNum(chapter: SChapter): Float? {
    val float = if (chapter.isMergedChapter()) {
        chapter.chapter_txt.toFloatOrNull()
    } else {
        if (chapter.name.contains("oneshot", true)) {
            0f
        } else {
            chapter.chapter_txt.substringAfter("Ch.").toFloatOrNull()
        }
    }
    return float ?: return null
}

fun getChapterNumInt(chapter: SChapter): Int? {
    val float = getChapterNum(chapter)
    return when (float != null) {
        true -> floor(float).toInt()
        else -> null
    }
}

fun getVolumeNum(chapter: SChapter): Int? {
    return if (chapter.isMergedChapter()) {
        chapter.vol.toIntOrNull()
    } else {
        chapter.vol.substringAfter("Vol.").toIntOrNull()
    }
}
