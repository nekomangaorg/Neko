package org.nekomanga.usecases.chapters

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.util.system.executeOnIO
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions

class MarkChapterUseCase(private val db: DatabaseHelper) {
    suspend operator fun invoke(markAction: ChapterMarkActions, chapterItems: List<ChapterItem>) {

        val dbChapters =
            chapterItems.map { chapterItem ->
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

        db.updateChaptersProgress(dbChapters).executeOnIO()
    }
}
