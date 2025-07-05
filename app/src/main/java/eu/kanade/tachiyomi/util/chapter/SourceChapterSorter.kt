package eu.kanade.tachiyomi.util.chapter

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.isLocalSource
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.source.online.utils.MdLang
import eu.kanade.tachiyomi.util.system.toInt
import kotlin.math.floor
import org.nekomanga.logging.TimberKt

/** This attempts to create a smart source order used when a manga is merged */
fun reorderChapters(sourceChapters: List<SChapter>, manga: Manga): List<SChapter> {
    if (sourceChapters.all { !it.isMergedChapter() && !it.isLocalSource() }) {
        return sourceChapters
    }

    var (withVolume, nullVolume) = sourceChapters.partition { getVolumeNum(it) == null }
    nullVolume = nullVolume.sortedWith(compareBy { getChapterNum(it) })

    // mangalife tends to not include a volume number for manga
    val sorter =
        if (manga.lang_flag != null && MdLang.fromIsoCode(manga.lang_flag!!) == MdLang.JAPANESE) {
            compareBy<SChapter> { getChapterNum(it) == null }.thenBy { getChapterNum(it) }
        } else {
            compareBy<SChapter> { getVolumeNum(it) }.thenBy { getChapterNum(it) }
        }
    withVolume = withVolume.sortedWith(sorter)

    return listOf(withVolume.asSequence(), nullVolume.asSequence())
        .mergeSorted(compareBy<SChapter> { getChapterNum(it) == null }.thenBy { getChapterNum(it) })
        .toList()
        .reversed()
}

fun <T> List<Sequence<T>>.mergeSorted(comparator: Comparator<T>): Sequence<T> {
    val iteratorToCurrentValues =
        map { it.iterator() }.filter { it.hasNext() }.associateWith { it.next() }.toMutableMap()

    val c: Comparator<Map.Entry<Iterator<T>, T>> = Comparator.comparing({ it.value }, comparator)

    return sequence {
        while (iteratorToCurrentValues.isNotEmpty()) {
            val smallestEntry = iteratorToCurrentValues.minWithOrNull(c)!!

            yield(smallestEntry.value)

            if (!smallestEntry.key.hasNext()) iteratorToCurrentValues.remove(smallestEntry.key)
            else iteratorToCurrentValues[smallestEntry.key] = smallestEntry.key.next()
        }
    }
}

fun getChapterNum(chapter: SChapter): Float? {
    TimberKt.d {
        "${getVolumeNum(chapter)} ${chapter.chapter_txt} ${
            chapter.isMergedChapter().toInt()
        } ${chapter.name} "
    }
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
