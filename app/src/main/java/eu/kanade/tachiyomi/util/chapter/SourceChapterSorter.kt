package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isMergedChapter

/** This attempts to create a smart source order used when a manga is merged */
fun reorderChapters(sourceChapters: List<Chapter>): List<Chapter> {
    // Mangadex and other sources tend to not always include a volume number, so we'll try to
    // interpolate
    // We can assume the null volume is a positive value, so Vol.0 always come before null volume
    // chapters
    var (zeroVolume, nonZeroVolume) = sourceChapters.partition { getVolumeNum(it) == 0 }
    zeroVolume =
        zeroVolume.sortedWith(
            compareByDescending<Chapter> { getChapterNum(it) == null }
                .thenByDescending { getChapterNum(it) }
        )

    var (nullVolume, withVolume) = nonZeroVolume.partition { getVolumeNum(it) == null }
    nullVolume =
        nullVolume.sortedWith(
            compareByDescending<Chapter> { getChapterNum(it) == null }
                .thenByDescending { getChapterNum(it) }
        )
    withVolume =
        withVolume.sortedWith(
            compareByDescending<Chapter> { getVolumeNum(it) }
                .thenByDescending { getChapterNum(it) == null }
                .thenByDescending { getChapterNum(it) }
        )

    val comp = compareBy<Chapter> { getChapterNum(it) == null }.thenBy { getChapterNum(it) }
    return listOf(nullVolume, withVolume).mergeSorted(comp) + zeroVolume
}

// Adapted from https://stackoverflow.com/a/69041133
fun <T> List<List<T>>.mergeSorted(comparator: Comparator<T>): List<T> {
    val iteratorToCurrentValues =
        map { it.reversed().iterator() }
            .filter { it.hasNext() }
            .associateWith { it.next() }
            .toMutableMap()

    val c: Comparator<Map.Entry<Iterator<T>, T>> = Comparator.comparing({ it.value }, comparator)

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

// fun getChapterNumInt(chapter: SChapter): Int? {
//    val float = getChapterNum(chapter)
//    return when (float != null) {
//        true -> floor(float).toInt()
//        else -> null
//    }
// }

fun getVolumeNum(chapter: SChapter): Int? {
    return chapter.vol.toIntOrNull()
}
