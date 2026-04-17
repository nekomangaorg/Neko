package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.MangaChapter as LegacyMangaChapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.nekomanga.data.database.dao.ChapterDao
import org.nekomanga.data.database.mapper.toChapter
import org.nekomanga.data.database.mapper.toManga

class ChapterRepositoryImpl(
    private val chapterDao: ChapterDao,
    // Injecting the dispatcher allows for easy testing, defaulting to IO
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChapterRepository {

    override fun getRecentChapters(
        search: String,
        limit: Int,
        offset: Int,
        sortByFetched: Boolean,
    ): Flow<List<LegacyMangaChapter>> {

        // 1. Format parameters for SQLite
        // Wrap the search string in '%' for SQLite's LIKE clause
        val sqlSearch = "%${search.lowercase()}%"

        // Room doesn't natively map Booleans to the integers used in your
        // CASE WHEN statement, so we convert it manually.
        val sortInt = if (sortByFetched) 1 else 0

        // 2. Fetch the Flow from Room
        return chapterDao
            .observeRecentChapters(
                search = sqlSearch,
                limit = limit,
                offset = offset,
                sortByFetched = sortInt,
            )
            .map { roomPojos ->

                // 3. Map the Room POJOs to Legacy Models
                roomPojos.map { pojo ->
                    LegacyMangaChapter(
                        manga = pojo.manga.toManga(),
                        chapter = pojo.chapter.toChapter(),
                    )
                }
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }
}
