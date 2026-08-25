package org.nekomanga.presentation.screens.similar

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.R
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.SimilarRepository
import org.nekomanga.domain.manga.SourceManga

class SimilarRepoTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var similarHandler: SimilarHandler
    private lateinit var mangaRepository: MangaRepository
    private lateinit var similarRepository: SimilarRepository
    private lateinit var sourceManager: SourceManager
    private lateinit var mangaDex: MangaDex
    private lateinit var similarRepo: SimilarRepo

    private val dexId = "test-uuid-1234"
    private val sourceId = 1L

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        similarHandler = mockk()
        mangaRepository = mockk()
        similarRepository = mockk()
        sourceManager = mockk()
        mangaDex = mockk()

        every { sourceManager.mangaDex } returns mangaDex
        every { mangaDex.id } returns sourceId

        similarRepo =
            SimilarRepo(
                similarHandler = similarHandler,
                mangaRepository = mangaRepository,
                similarRepository = similarRepository,
                sourceManager = sourceManager,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given empty recommendations from all sources when fetchSimilar called then returns empty list without querying manga`() =
        runTest {
            // Arrange
            val mockSimilarDb = mockk<MangaSimilar>()
            coEvery { similarRepository.getSimilar(dexId) } returns mockSimilarDb
            coEvery { similarHandler.fetchRelated(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchRecommended(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilar(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilarExternalMUManga(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchAnilist(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilarExternalMalManga(dexId, false) } returns
                emptyList()

            // Act
            val result = similarRepo.fetchSimilar(dexId, forceRefresh = false)

            // Assert
            assertTrue(result.isEmpty())
            coVerify(exactly = 0) { mangaRepository.getMangaByUrls(any()) }
            coVerify(exactly = 0) { mangaRepository.insertMangaList(any()) }
        }

    @Test
    fun `given recommendations across multiple sources when fetchSimilar called then fetches raw lists in parallel and batch queries manga repository once`() =
        runTest {
            // Arrange
            val mockSimilarDb = mockk<MangaSimilar>()
            coEvery { similarRepository.getSimilar(dexId) } returns mockSimilarDb

            val relatedManga =
                listOf(
                    SourceManga(
                        url = "/manga/related-1",
                        currentThumbnail = "https://thumb/1.jpg",
                        title = "Related 1",
                        displayText = "prequel",
                    )
                )
            val recommendedManga =
                listOf(
                    SourceManga(
                        url = "/manga/rec-1",
                        currentThumbnail = "https://thumb/2.jpg",
                        title = "Rec 1",
                        displayText = "95% match",
                    ),
                    SourceManga(
                        url = "/manga/rec-2",
                        currentThumbnail = "https://thumb/3.jpg",
                        title = "Rec 2",
                        displayText = "88% match",
                    ),
                )

            coEvery { similarHandler.fetchRelated(dexId, false) } returns relatedManga
            coEvery { similarHandler.fetchRecommended(dexId, false) } returns recommendedManga
            coEvery { similarHandler.fetchSimilar(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilarExternalMUManga(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchAnilist(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilarExternalMalManga(dexId, false) } returns
                emptyList()

            val dbManga1 =
                Manga.create("/manga/related-1", "Related 1", sourceId).apply {
                    id = 101L
                    thumbnail_url = "https://thumb/1.jpg"
                }
            val dbManga2 =
                Manga.create("/manga/rec-1", "Rec 1", sourceId).apply {
                    id = 102L
                    thumbnail_url = "https://thumb/2.jpg"
                }
            val dbManga3 =
                Manga.create("/manga/rec-2", "Rec 2", sourceId).apply {
                    id = 103L
                    thumbnail_url = "https://thumb/3.jpg"
                }

            coEvery {
                mangaRepository.getMangaByUrls(
                    listOf("/manga/related-1", "/manga/rec-1", "/manga/rec-2")
                )
            } returns listOf(dbManga1, dbManga2, dbManga3)

            // Act
            val result = similarRepo.fetchSimilar(dexId, forceRefresh = false)

            // Assert
            assertEquals(2, result.size)
            assertEquals(R.string.related_type, result[0].type)
            assertEquals(1, result[0].manga.size)
            assertEquals(101L, result[0].manga[0].mangaId)
            assertEquals("Related 1", result[0].manga[0].originalTitle)

            assertEquals(R.string.recommended_type, result[1].type)
            assertEquals(2, result[1].manga.size)
            assertEquals(102L, result[1].manga[0].mangaId)
            assertEquals(103L, result[1].manga[1].mangaId)

            // Verify exactly 1 batch query was made for all recommendations across categories
            coVerify(exactly = 1) { mangaRepository.getMangaByUrls(any()) }
            coVerify(exactly = 0) { mangaRepository.insertMangaList(any()) }
        }

    @Test
    fun `given new manga not present in database when fetchSimilar called then executes single batch insert`() =
        runTest {
            // Arrange
            val mockSimilarDb = mockk<MangaSimilar>()
            coEvery { similarRepository.getSimilar(dexId) } returns mockSimilarDb

            val similarManga =
                listOf(
                    SourceManga(
                        url = "/manga/new-1",
                        currentThumbnail = "https://thumb/new1.jpg",
                        title = "New Manga 1",
                        displayText = "99.00% match",
                    )
                )

            coEvery { similarHandler.fetchRelated(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchRecommended(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilar(dexId, false) } returns similarManga
            coEvery { similarHandler.fetchSimilarExternalMUManga(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchAnilist(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilarExternalMalManga(dexId, false) } returns
                emptyList()

            // Database does not have it yet
            coEvery { mangaRepository.getMangaByUrls(listOf("/manga/new-1")) } returns emptyList()
            coEvery { mangaRepository.insertMangaList(any()) } returns listOf(201L)

            // Act
            val result = similarRepo.fetchSimilar(dexId, forceRefresh = false)

            // Assert
            assertEquals(1, result.size)
            assertEquals(R.string.similar_type, result[0].type)
            assertEquals(1, result[0].manga.size)
            assertEquals(201L, result[0].manga[0].mangaId)
            assertEquals("New Manga 1", result[0].manga[0].originalTitle)

            coVerify(exactly = 1) { mangaRepository.getMangaByUrls(listOf("/manga/new-1")) }
            coVerify(exactly = 1) { mangaRepository.insertMangaList(any()) }
        }

    @Test
    fun `given a source throws exception when fetchSimilar called then gracefully returns results from other sources`() =
        runTest {
            // Arrange
            val mockSimilarDb = mockk<MangaSimilar>()
            coEvery { similarRepository.getSimilar(dexId) } returns mockSimilarDb

            val malManga =
                listOf(
                    SourceManga(
                        url = "/manga/mal-1",
                        currentThumbnail = "https://thumb/mal.jpg",
                        title = "MAL Manga",
                        displayText = "500 user votes",
                    )
                )

            coEvery { similarHandler.fetchRelated(dexId, false) } throws
                RuntimeException("Network error")
            coEvery { similarHandler.fetchRecommended(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilar(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilarExternalMUManga(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchAnilist(dexId, false) } returns emptyList()
            coEvery { similarHandler.fetchSimilarExternalMalManga(dexId, false) } returns malManga

            val dbManga =
                Manga.create("/manga/mal-1", "MAL Manga", sourceId).apply {
                    id = 301L
                    thumbnail_url = "https://thumb/mal.jpg"
                }
            coEvery { mangaRepository.getMangaByUrls(listOf("/manga/mal-1")) } returns
                listOf(dbManga)

            // Act
            val result = similarRepo.fetchSimilar(dexId, forceRefresh = false)

            // Assert
            assertEquals(1, result.size)
            assertEquals(R.string.myanimelist, result[0].type)
            assertEquals(1, result[0].manga.size)
            assertEquals(301L, result[0].manga[0].mangaId)
        }
}
