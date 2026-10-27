package org.nekomanga.data.repository

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import eu.kanade.tachiyomi.source.model.MangaListPage
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import io.kotest.matchers.shouldBe
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
import org.nekomanga.domain.site.MangaDexPreferences

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
    fun `getSearchPage returns empty list and hasNextPage false for empty results`() = runTest {
        val dexFilters: DexFilters = mockk()
        every { mangaDex.id } returns 1L
        coEvery { mangaDex.search(1, dexFilters) } returns
            Ok(MangaListPage(hasNextPage = false, sourceManga = emptyList()))

        val result = repository.getSearchPage(1, dexFilters).get()

        result shouldBe Pair(false, emptyList())
    }

    @Test
    fun `getFollows returns empty list for empty follows`() = runTest {
        every { mangaDex.id } returns 1L
        coEvery { mangaDex.fetchAllFollows() } returns Ok(emptyList())

        val result = repository.getFollows().get()

        result shouldBe emptyList()
    }
}
