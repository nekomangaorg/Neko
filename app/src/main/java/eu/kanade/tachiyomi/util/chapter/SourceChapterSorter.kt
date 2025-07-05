package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isLocalSource
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.util.system.toInt
import kotlin.math.floor

/** This attempts to create a smart source order used when a manga is merged */
fun reorderChapters(sourceChapters: List<SChapter>, manga: Manga): List<SChapter> {
    if (sourceChapters.all { !it.isMergedChapter() && !it.isLocalSource() }) {
        return sourceChapters
    }

    var (nullVolume, withVolume) = sourceChapters.partition { getVolumeNum(it) == null }
    nullVolume = nullVolume.sortedWith(compareByDescending { getChapterNum(it) })

    // mangalife tends to not include a volume number for manga
    val sorter =
        if (manga.lang_flag != null && MdLang.fromIsoCode(manga.lang_flag!!) == MdLang.JAPANESE) {
            compareByDescending<SChapter> { getChapterNum(it) == null }
                .thenByDescending { getChapterNum(it) }
        } else {
            compareByDescending<SChapter> { getVolumeNum(it) }
                .thenByDescending { getChapterNum(it) }
        }
    withVolume = withVolume.sortedWith(sorter)

    return listOf(withVolume, nullVolume).mergeSorted()
}

// Adapted from https://stackoverflow.com/a/69041133
private fun List<List<SChapter>>.mergeSorted(): List<SChapter> {
    val iteratorToCurrentValues =
        map { it.reversed().iterator() }
            .filter { it.hasNext() }
            .associateWith { it.next() }
            .toMutableMap()

    val c: Comparator<Map.Entry<Iterator<SChapter>, SChapter>> =
        Comparator.comparing(
            { it.value },
            compareBy<SChapter> { getChapterNum(it) == null }.thenBy { getChapterNum(it) },
        )

    return sequence {
            while (iteratorToCurrentValues.isNotEmpty()) {
                val smallestEntry = iteratorToCurrentValues.minWithOrNull(c)!!

                yield(smallestEntry.value)

                if (!smallestEntry.key.hasNext()) iteratorToCurrentValues.remove(smallestEntry.key)
                else iteratorToCurrentValues[smallestEntry.key] = smallestEntry.key.next()
            }
        }
        .toList()
        .reversed()
}

fun getChapterNum(chapter: SChapter): Float? {
    return when (chapter.name.contains("oneshot", true) && !chapter.isMergedChapter()) {
        true -> 0f
        false -> {
            val txt = chapter.chapter_txt
            txt.subStringFloatOrNull("Ch.")
                ?: txt.subStringFloatOrNull("Chp.")
                ?: txt.subStringFloatOrNull("Chapter")
        }
    }
}

private fun String.subStringFloatOrNull(delimiter: String): Float? {
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
