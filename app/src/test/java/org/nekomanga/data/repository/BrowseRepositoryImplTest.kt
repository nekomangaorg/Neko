package org.nekomanga.data.repository

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.ScanlatorGroupImpl
import eu.kanade.tachiyomi.data.database.models.UploaderImpl
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.handlers.ListResults
import eu.kanade.tachiyomi.ui.source.latest.DisplayScreenType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.ScanlatorGroupRepository
import org.nekomanga.data.database.repository.UploaderRepository
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import tachiyomi.core.preference.Preference

class BrowseRepositoryImplTest {

    private val mangaDex: MangaDex = mockk()
    private val loginHelper: MangaDexLoginHelper = mockk()
    private val mangaRepository: MangaRepository = mockk()
    private val scanlatorGroupRepository: ScanlatorGroupRepository = mockk()
    private val uploaderRepository: UploaderRepository = mockk()
    private val mangaDexPreferences: MangaDexPreferences = mockk()

    private val repository =
        BrowseRepositoryImpl(
            mangaDex = mangaDex,
            loginHelper = loginHelper,
            mangaRepository = mangaRepository,
            scanlatorGroupRepository = scanlatorGroupRepository,
            uploaderRepository = uploaderRepository,
            mangaDexPreferences = mangaDexPreferences,
        )

    @Test
    fun `isLoggedIn delegates to loginHelper`() {
        every { loginHelper.isLoggedIn() } returns true
        repository.isLoggedIn() shouldBe true

        every { loginHelper.isLoggedIn() } returns false
        repository.isLoggedIn() shouldBe false
    }

    @Test
    fun `isLoggedInFlow delegates to loginHelper`() = runTest {
        val flow = MutableStateFlow(true)
        every { loginHelper.isLoggedInFlow() } returns flow

        repository.isLoggedInFlow().first() shouldBe true
    }

    @Test
    fun `getRandomManga returns mapped DisplayManga on success`() = runTest {
        val sourceManga =
            SourceManga(
                currentThumbnail = "https://thumb.jpg",
                url = "/title/123",
                title = "Random Manga",
            )
        val dbManga =
            Manga.create("/title/123", "Random Manga", 1L).apply {
                id = 123L
                thumbnail_url = "https://thumb.jpg"
            }

        every { mangaDex.id } returns 1L
        coEvery { mangaDex.getRandomManga() } returns Ok(sourceManga)
        coEvery { mangaRepository.getMangaByUrlAndSource("/title/123", 1L) } returns dbManga

        val result = repository.getRandomManga()

        result.get() shouldNotBe null
        result.get()!!.mangaId shouldBe 123L
        result.get()!!.originalTitle shouldBe "Random Manga"
    }

    @Test
    fun `getRandomManga propagates error on failure`() = runTest {
        val error = ResultError.Generic("Network failure")
        coEvery { mangaDex.getRandomManga() } returns Err(error)

        val result = repository.getRandomManga()

        result.getError() shouldBe error
    }

    @Test
    fun `getSearchPage returns empty list and hasNextPage false for empty results`() = runTest {
        val dexFilters: DexFilters = mockk()
        every { mangaDex.id } returns 1L
        coEvery { mangaDex.search(1, dexFilters) } returns
            Ok(MangaListPage(hasNextPage = false, sourceManga = emptyList()))

        val result = repository.getSearchPage(1, dexFilters).get()

        result shouldBe Pair(false, emptyList())
    }

    @Test
    fun `getSearchPage propagates error on failure`() = runTest {
        val dexFilters: DexFilters = mockk()
        val error = ResultError.Generic("Search failed")
        coEvery { mangaDex.search(1, dexFilters) } returns Err(error)

        val result = repository.getSearchPage(1, dexFilters)

        result.getError() shouldBe error
    }

    @Test
    fun `getSearchPage maps sourceManga to displayManga correctly`() = runTest {
        val dexFilters: DexFilters = mockk()
        val sourceManga =
            SourceManga(
                currentThumbnail = "https://thumb.jpg",
                url = "/title/456",
                title = "Search Manga",
            )
        val dbManga =
            Manga.create("/title/456", "Search Manga", 1L).apply {
                id = 456L
                thumbnail_url = "https://thumb.jpg"
            }

        every { mangaDex.id } returns 1L
        coEvery { mangaDex.search(1, dexFilters) } returns
            Ok(MangaListPage(hasNextPage = true, sourceManga = listOf(sourceManga)))
        coEvery { mangaRepository.getMangaByUrls(listOf("/title/456")) } returns listOf(dbManga)

        val result = repository.getSearchPage(1, dexFilters).get()

        result shouldNotBe null
        result!!.first shouldBe true
        result.second.size shouldBe 1
        result.second.first().mangaId shouldBe 456L
    }

    @Test
    fun `getHomePage returns sections with blocked groups and uploaders handled`() = runTest {
        val blockedGroupsPref = mockk<Preference<Set<String>>>()
        val blockedUploadersPref = mockk<Preference<Set<String>>>()
        every { blockedGroupsPref.get() } returns setOf("Group A")
        every { blockedUploadersPref.get() } returns setOf("Uploader B")
        every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
        every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

        val groupA = ScanlatorGroupImpl(name = "Group A", uuid = "group-uuid-a", description = null)
        val uploaderB = UploaderImpl(username = "Uploader B", uuid = "uploader-uuid-b")

        coEvery { scanlatorGroupRepository.getScanlatorGroupsByNames(listOf("Group A")) } returns
            listOf(groupA)
        coEvery { uploaderRepository.getUploadersByNames(listOf("Uploader B")) } returns
            listOf(uploaderB)

        val sourceManga =
            SourceManga(
                currentThumbnail = "https://thumb.jpg",
                url = "/title/789",
                title = "Home Manga",
            )
        val dbManga =
            Manga.create("/title/789", "Home Manga", 1L).apply {
                id = 789L
                thumbnail_url = "https://thumb.jpg"
            }

        val listResults =
            ListResults(
                displayScreenType = DisplayScreenType.PopularNewTitles,
                sourceManga = listOf(sourceManga),
            )

        every { mangaDex.id } returns 1L
        coEvery {
            mangaDex.fetchHomePageInfo(
                blockedGroupUUIDs = listOf("group-uuid-a"),
                blockedUploaderUUIDs = listOf("uploader-uuid-b"),
            )
        } returns Ok(listOf(listResults))
        coEvery { mangaRepository.getMangaByUrls(listOf("/title/789")) } returns listOf(dbManga)

        val result = repository.getHomePage()

        result.get() shouldNotBe null
        val sections = result.get()!!
        sections.size shouldBe 1
        sections.first().displayScreenType shouldBe DisplayScreenType.PopularNewTitles
        sections.first().mangaList.size shouldBe 1
        sections.first().mangaList.first().mangaId shouldBe 789L
    }

    @Test
    fun `getHomePage propagates error when fetchHomePageInfo fails`() = runTest {
        val blockedGroupsPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        val blockedUploadersPref =
            mockk<Preference<Set<String>>> { every { get() } returns emptySet() }
        every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPref
        every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPref

        val error = ResultError.Generic("Failed to fetch home page")
        coEvery { mangaDex.fetchHomePageInfo(emptyList(), emptyList()) } returns Err(error)

        val result = repository.getHomePage()

        result.getError() shouldBe error
    }

    @Test
    fun `getFollows returns empty list for empty follows`() = runTest {
        every { mangaDex.id } returns 1L
        coEvery { mangaDex.fetchAllFollows() } returns Ok(emptyList())

        val result = repository.getFollows().get()

        result shouldBe emptyList()
    }

    @Test
    fun `getFollows propagates error on failure`() = runTest {
        val error = ResultError.Generic("Failed to fetch follows")
        coEvery { mangaDex.fetchAllFollows() } returns Err(error)

        val result = repository.getFollows()

        result.getError() shouldBe error
    }
}
