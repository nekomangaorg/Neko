package org.nekomanga.domain.chapter

sealed class ChapterMarkActions {
    abstract val canUndo: Boolean

    data class Bookmark(override val canUndo: Boolean = false) : ChapterMarkActions()

    data class UnBookmark(override val canUndo: Boolean = false) : ChapterMarkActions()

    data class PreviousRead(override val canUndo: Boolean, val altChapters: List<ChapterItem>) :
        ChapterMarkActions()

    data class PreviousUnread(override val canUndo: Boolean, val altChapters: List<ChapterItem>) :
        ChapterMarkActions()

    data class Read(override val canUndo: Boolean = false) : ChapterMarkActions()

    data class Unread(
        override val canUndo: Boolean = false,
        val lastRead: Int? = null,
        val pagesLeft: Int? = null,
    ) : ChapterMarkActions()
}
