package eu.kanade.tachiyomi.ui.reader.domain

import eu.kanade.tachiyomi.ui.reader.model.ChapterNavTarget
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter

/**
 * Resolves the appropriate navigation target when moving between chapters in reading order. Ensures
 * forward continuous transitions land at the start (page 0), backward transitions land at the end,
 * and initial loads or explicit jumps restore reading progress.
 */
class ResolveChapterNavTargetUseCase {

    operator fun invoke(
        currentChapter: ReaderChapter?,
        selectedChapter: ReaderChapter,
        chapterList: List<ReaderChapter>,
        nextChapter: ReaderChapter? = null,
        prevChapter: ReaderChapter? = null,
    ): ChapterNavTarget {
        if (currentChapter == null || currentChapter.chapter.id == selectedChapter.chapter.id) {
            return ChapterNavTarget.Resume
        }

        val isForward =
            when {
                nextChapter != null && selectedChapter.chapter.id == nextChapter.chapter.id -> true
                prevChapter != null && selectedChapter.chapter.id == prevChapter.chapter.id -> false
                else -> {
                    val currentIndex = chapterList.indexOfFirst {
                        it.chapter.id == currentChapter.chapter.id
                    }
                    val newIndex = chapterList.indexOfFirst {
                        it.chapter.id == selectedChapter.chapter.id
                    }
                    if (currentIndex != -1 && newIndex != -1) {
                        newIndex > currentIndex
                    } else {
                        selectedChapter.chapter.chapter_number >
                            currentChapter.chapter.chapter_number
                    }
                }
            }

        return if (isForward) {
            ChapterNavTarget.Start
        } else {
            ChapterNavTarget.End
        }
    }
}
