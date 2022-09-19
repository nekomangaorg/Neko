package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.source.model.SChapter
import kotlin.math.floor
import org.nekomanga.domain.chapter.ChapterItem

/**
 * Calculate the missing chapters for a given list of chapters. Return null if none are missing
 */
fun List<SChapter>.getMissingChapterCount(mangaStatus: Int): String? {
    // if (mangaStatus == SManga.COMPLETED) return null
    var count = 0
    if (this.isNotEmpty()) {
        val chapterNumberArray = this.asSequence().distinctBy {
            if (it.chapter_txt.isNotEmpty()) {
                it.vol + it.chapter_txt
            } else {
                it.name
            }
        }.sortedBy { it.chapter_number }
            .map { floor(it.chapter_number).toInt() }.toList().toIntArray()

        if (chapterNumberArray.isNotEmpty()) {
            if (chapterNumberArray.first() > 1) {
                while (count != (chapterNumberArray[0] - 1)) {
                    count++
                    if (count > 5000) {
                        break
                    }
                }
            }

            chapterNumberArray.forEachIndexed { index, chpNum ->
                val lastIndex = index - 1
                if (lastIndex >= 0 && (chpNum - 1) > chapterNumberArray[lastIndex]) {
                    count += (chpNum - chapterNumberArray[lastIndex]) - 1
                }
            }
        }
    }

    if (count <= 0) return null

    return count.toString()
}

/**
 * Calculate the missing chapters for a given list of chapters. Return null if none are missing
 */
fun List<ChapterItem>.getMissingCount(mangaStatus: Int): String? {
    // if (mangaStatus == SManga.COMPLETED) return null
    var count = 0
    if (this.isNotEmpty()) {
        val chapterNumberArray = this.asSequence().map { it.chapter }.distinctBy {
            if (it.chapterText.isNotEmpty()) {
                it.volume + it.chapterText
            } else {
                it.name
            }
        }.sortedBy { it.chapterNumber }
            .map { floor(it.chapterNumber).toInt() }.toList().toIntArray()

        if (chapterNumberArray.isNotEmpty()) {
            if (chapterNumberArray.first() > 1) {
                while (count != (chapterNumberArray[0] - 1)) {
                    count++
                    if (count > 5000) {
                        break
                    }
                }
            }

            chapterNumberArray.forEachIndexed { index, chpNum ->
                val lastIndex = index - 1
                if (lastIndex >= 0 && (chpNum - 1) > chapterNumberArray[lastIndex]) {
                    count += (chpNum - chapterNumberArray[lastIndex]) - 1
                }
            }
        }
    }

    if (count <= 0) return null

    return count.toString()
}
