package org.nekomanga.presentation.screens.deepLink

import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.manga.MangaMappings
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.MangaRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton

@OptIn(ExperimentalCoroutinesApi::class)
class DeepLinkViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockMangaMappings: MangaMappings
    private lateinit var mockSourceManager: SourceManager
    private lateinit var mockMangaDex: MangaDex
    private lateinit var mockMangaRepository: MangaRepository

    private lateinit var viewModel: DeepLinkViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockMangaMappings = mockk()
        mockSourceManager = mockk()
        mockMangaDex = mockk()
        mockMangaRepository = mockk()

        every { mockSourceManager.mangaDex } returns mockMangaDex

        Injekt.addSingleton(mockMangaMappings)
        Injekt.addSingleton(mockSourceManager)
        Injekt.addSingleton(mockMangaRepository)

        viewModel = DeepLinkViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        val fields = Injekt::class.java.declaredFields
        for (field in fields) {
            if (field.name == "registrars") {
                field.isAccessible = true
                val map = field.get(Injekt) as MutableMap<*, *>
                map.clear()
            }
        }
    }

    @Test
    fun `given anilist host and no mapping found when handling deep link then state is Error`() =
        runTest {
            // Arrange
            val host = "anilist.co"
            val path = "manga"
            val id = "12345"

            every { mockMangaMappings.getMangadexUUID(id, "al") } returns null

            // Act
            viewModel.handleDeepLink(host, path, id)
            advanceUntilIdle()

            // Assert
            val state = viewModel.deepLinkState.value
            assertTrue(state is DeepLinkState.Error)
            assertEquals(
                "Unable to map MangaDex manga, no mapping entry found for AniList ID",
                (state as DeepLinkState.Error).errorMessage,
            )
        }
}
