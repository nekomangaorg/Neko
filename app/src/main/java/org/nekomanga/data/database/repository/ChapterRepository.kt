package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MangaChapter
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {

    /**
     * Observes a paginated list of recent chapters with their associated manga.
     * * @param search The user's search query to filter manga titles.
     *
     * @param limit The number of items to load (for pagination).
     * @param offset The starting point (for pagination).
     * @param sortByFetched If true, sorts by when the app fetched the chapter. If false, sorts by
     *   when the scanlator uploaded it.
     */
    fun getRecentChapters(
        search: String = "",
        limit: Int,
        offset: Int,
        sortByFetched: Boolean,
    ): Flow<List<MangaChapter>>

    // Future methods you will add later:
    // suspend fun getChapterById(id: Long): Chapter?
    // suspend fun updateChapter(chapter: Chapter)
    // suspend fun deleteChapters(chapters: List<Chapter>)
}
