package eu.kanade.tachiyomi.ui.reader.chapter

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga

data class ReaderChapterItem(
    val chapter: Chapter,
    val manga: Manga,
    val isCurrent: Boolean,
) : Chapter by chapter
