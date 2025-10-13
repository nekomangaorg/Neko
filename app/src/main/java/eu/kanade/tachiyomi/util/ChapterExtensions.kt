package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.model.isLocalSource
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import kotlin.math.floor
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.chapter.ChapterItem

data class MissingChapterHolder(val count: String? = null, val estimatedChapters: String? = null)

fun List<ChapterItem>.getMissingChapters(): MissingChapterHolder {
    var count = 0
    val estimateChapters = mutableListOf<String>()

    if (this.isNotEmpty()) {
        val chapterNumberArray =
            this.asSequence()
                .filter { it.isAvailable() }
                .distinctBy {
                    if (it.chapter.chapterText.isNotEmpty()) {
                        it.chapter.volume + it.chapter.chapterText
                    } else {
                        it.chapter.name
                    }
                }
                .sortedBy { it.chapter.chapterNumber }
                .mapNotNull {
                    when (it.chapter.chapterText.isEmpty() && !it.chapter.isMergedChapter()) {
                        true -> null
                        false -> floor(it.chapter.chapterNumber).toInt()
                    }
                }
                .toList()
                .toIntArray()

        if (chapterNumberArray.isNotEmpty()) {
            if (chapterNumberArray.first() > 1) {
                while (count != (chapterNumberArray[0] - 1)) {
                    estimateChapters.add("Chp. $count")
                    count++

                    if (count > 5000) {
                        break
                    }
                }
            }

            chapterNumberArray.forEachIndexed { index, chpNum ->
                val lastIndex = index - 1
                if (
                    lastIndex >= 0 &&
                        (chpNum - 1) > chapterNumberArray[lastIndex] &&
                        chapterNumberArray[lastIndex] > 0
                ) {
                    count += (chpNum - chapterNumberArray[lastIndex]) - 1
                    val beginningChp = (chapterNumberArray[lastIndex] + 1)
                    val endChap = chpNum - 1
                    when (beginningChp == endChap) {
                        true -> estimateChapters.add("Ch.$beginningChp")
                        false ->
                            estimateChapters.add(
                                "Ch.$beginningChp ${Constants.RIGHT_ARROW_SEPARATOR} Ch.$endChap"
                            )
                    }
                }
            }
        }
    }

    val actualCount =
        if (count <= 0) {
            null
        } else {
            count.toString()
        }

    val estimateChapterString =
        when (estimateChapters.isEmpty()) {
            true -> null
            false -> estimateChapters.joinToString(Constants.SEPARATOR)
        }

    return MissingChapterHolder(count = actualCount, estimatedChapters = estimateChapterString)
}

/**
 * returns true for a list filter, if the source name exists in the filtered sources, and the
 * chapter has the scanlator
 */
fun Chapter.filteredBySource(sourceName: String, filteredSources: Set<String>): Boolean {
    if (filteredSources.isEmpty()) {
        return false
    }
    val shouldCheck = sourceName in filteredSources
    if (!shouldCheck) {
        return false
    }

    if (sourceName == MdConstants.name) {
        return !this.isMergedChapter()
    }

    return ChapterUtil.getScanlators(this.scanlator).any { group -> group == sourceName }
}

fun ChapterItem.isAvailable(): Boolean {
    return !this.chapter.isUnavailable || this.chapter.isLocalSource() || this.isDownloaded
}

fun Chapter.isAvailable(downloadManager: DownloadManager, manga: Manga): Boolean {
    return !this.isUnavailable ||
        this.isLocalSource() ||
        downloadManager.isChapterDownloaded(this, manga)
}

fun Chapter.isAvailable(isDownloaded: Boolean): Boolean {
    return isDownloaded || !this.isUnavailable || this.isLocalSource()
}
