package org.nekomanga.usecases.chapters

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.util.system.executeOnIO
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MarkChapterRead(private val db: DatabaseHelper = Injekt.get()) {
    suspend operator fun invoke(markAction: ChapterMarkActions, chapterItem: ChapterItem) {
        val updatedChapter =
            when (markAction) {
                is ChapterMarkActions.Read -> {
                    chapterItem.chapter.copy(read = true)
                }
                is ChapterMarkActions.Unread -> {
                    chapterItem.chapter.copy(
                        read = false,
                        lastPageRead = markAction.lastRead ?: 0,
                        pagesLeft = markAction.pagesLeft ?: 0,
                    )
                }
                else -> chapterItem.chapter
            }.toDbChapter()
        db.updateChaptersProgress(listOf(updatedChapter)).executeOnIO()
    }
}
