package eu.kanade.tachiyomi.data.database.models

/**
 * Object containing manga, chapter and history
 *
 * @param manga object containing manga
 * @param chapter object containing chapter
 * @param history object containing history
 */
data class MangaChapterHistory(val manga: Manga, val chapter: Chapter, val history: History) {
    companion object {
        fun createBlank() = MangaChapterHistory(MangaImpl(), ChapterImpl(), HistoryImpl())
    }
}
