package org.nekomanga.data.database.repository

import eu.kanade.tachiyomi.data.database.models.Chapter
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveAtMostSize
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.nekomanga.data.database.dao.ChapterDao
import org.nekomanga.data.database.mapper.toEntity

class ChapterRepositoryImplTest {

    // The SQLite limit on Android 11 and older, not the chunk size the repository uses, so the
    // test fails only for a chunk size that would crash.
    private val sqliteMaxVariables = 999

    private val chapterDao: ChapterDao = mockk()

    private val repository = ChapterRepositoryImpl(chapterDao = chapterDao)

    private val urls = (1..2000).map { "/chapter/uuid-$it" }

    @Test
    fun `getChaptersByUrls splits the url list so no query exceeds the SQLite variable limit`() =
        runTest {
            val capturedQueries = mutableListOf<List<String>>()
            coEvery { chapterDao.getChaptersByUrls(capture(capturedQueries)) } returns emptyList()

            repository.getChaptersByUrls(urls)

            capturedQueries.forEach { query -> query shouldHaveAtMostSize sqliteMaxVariables }
            capturedQueries.flatten() shouldContainExactlyInAnyOrder urls
        }

    @Test
    fun `getChaptersByUrls returns the rows of every chunk`() = runTest {
        coEvery { chapterDao.getChaptersByUrls(any()) } answers
            {
                firstArg<List<String>>().map { url ->
                    Chapter.create()
                        .apply {
                            manga_id = 1L
                            this.url = url
                            name = url
                        }
                        .toEntity()
                }
            }

        repository.getChaptersByUrls(urls).map { it.url } shouldContainExactlyInAnyOrder urls
    }

    @Test
    fun `getChaptersByUrls queries a repeated url once`() = runTest {
        val capturedQueries = mutableListOf<List<String>>()
        coEvery { chapterDao.getChaptersByUrls(capture(capturedQueries)) } returns emptyList()

        repository.getChaptersByUrls(listOf("/chapter/a", "/chapter/b", "/chapter/a"))

        capturedQueries.flatten() shouldContainExactlyInAnyOrder listOf("/chapter/a", "/chapter/b")
    }
}
