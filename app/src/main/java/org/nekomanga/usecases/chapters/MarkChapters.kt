package org.nekomanga.usecases.chapters

import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions

/**
 * Use Case to mark chapter read, unread, previously read, previously unread, bookmarked,
 * unbookmarked
 */
class MarkChapterUseCase(private val chapterRepository: ChapterRepository) {
    suspend operator fun invoke(markAction: ChapterMarkActions, chapterItems: List<ChapterItem>) {

        val dbChapters = chapterItems.map { chapterItem ->
            when (markAction) {
                is ChapterMarkActions.Bookmark -> chapterItem.chapter.copy(bookmark = true)
                is ChapterMarkActions.UnBookmark -> chapterItem.chapter.copy(bookmark = false)
                is ChapterMarkActions.Read,
                is ChapterMarkActions.PreviousRead -> chapterItem.chapter.copy(read = true)
                is ChapterMarkActions.Unread -> {
                    chapterItem.chapter.copy(
                        read = false,
                        lastPageRead = markAction.lastRead ?: 0,
                        pagesLeft = markAction.pagesLeft ?: 0,
                    )
                }
                is ChapterMarkActions.PreviousUnread -> {
                    chapterItem.chapter.copy(read = false, lastPageRead = 0, pagesLeft = 0)
                }
            }.toDbChapter()
        }

        chapterRepository.updateChaptersProgress(dbChapters)
    }
}
