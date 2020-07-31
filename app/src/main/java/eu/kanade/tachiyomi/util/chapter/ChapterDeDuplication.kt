package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.getChapterNum
import eu.kanade.tachiyomi.source.model.getVolumeNum
import eu.kanade.tachiyomi.source.model.isMergedChapter

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

    var dexMap: Map<Int?, List<Int?>>? = null
    var only1VolNoVol: Boolean = false

    if (isManga.not()) {
        dexMap = dexChapters.groupBy(keySelector = { it.getVolumeNum() }, valueTransform = { it.getChapterNum() })
        only1VolNoVol = dexChapters.all { it.getVolumeNum() == 1 } && mergedChapters.all { it.getVolumeNum() == null }
    }

    var dexSet: HashSet<Int?>? = null
    if (isManga || only1VolNoVol) {
        dexSet = dexChapters.map { it.getChapterNum() }.toHashSet()
    }

    mergedChapters.forEach { sChapter ->
        sChapter.getChapterNum()?.let { chpNum ->
            if (isManga || only1VolNoVol) {
                if (!dexSet!!.contains(chpNum)) {
                    dexChapters.add(sChapter)
                } else {
                }
            } else {
                val volume = dexMap!![sChapter.getVolumeNum()]
                if (volume == null) {
                    dexChapters.add(sChapter)
                } else {
                    dexMap!![sChapter.getVolumeNum()]?.let {
                        if (it.contains(chpNum).not()) {
                            dexChapters.add(sChapter)
                        }
                    }
                }
            }
        }
    }
    val sorter = when (isManga || only1VolNoVol) {
        true -> compareByDescending { it.getChapterNum() }
        false -> compareByDescending<SChapter> { it.getVolumeNum() }.thenByDescending { it.getChapterNum() }
    }

    return dexChapters.sortedWith(sorter)
}