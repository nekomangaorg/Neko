package org.nekomanga.usecases.chapters

import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions

class MarkPreviousChaptersUseCase {

    data class Result(val chaptersToMark: List<ChapterItem>, val action: ChapterMarkActions)

    operator fun invoke(
        chapterItem: ChapterItem,
        chapterList: List<ChapterItem>,
        read: Boolean,
    ): Result? {
        val chapterIndex = chapterList.indexOf(chapterItem)
        if (chapterIndex == -1) return null

        val chaptersToMark = chapterList.subList(0, chapterIndex)
        val altChapters = chapterList.subList(chapterIndex + 1, chapterList.size)

        val action =
            if (read) ChapterMarkActions.PreviousRead(true, altChapters)
            else ChapterMarkActions.PreviousUnread(true, altChapters)

        return Result(chaptersToMark, action)
    }
}
