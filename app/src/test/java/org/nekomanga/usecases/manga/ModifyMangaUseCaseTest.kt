package org.nekomanga.usecases.manga

import android.content.Context
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.model.toManga
import org.nekomanga.data.database.repository.CategoryRepositoryImpl
import org.nekomanga.data.database.repository.MangaRepositoryImpl
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton

class ModifyMangaUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mangaRepository: MangaRepositoryImpl
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var preferences: PreferencesHelper
    private lateinit var downloadManager: DownloadManager
    private lateinit var storageManager: StorageManager
    private lateinit var modifyMangaUseCase: ModifyMangaUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mangaRepository = mockk()
        categoryRepository = mockk()
        preferences = mockk()
        downloadManager = mockk()
        storageManager = mockk()

        modifyMangaUseCase =
            ModifyMangaUseCase(
                mangaRepository,
                categoryRepository,
                preferences,
                downloadManager,
                storageManager,
            )

        mockkStatic("org.nekomanga.data.database.model.MangaMappersKt")
        mockkConstructor(DownloadProvider::class)

        // Setup Injekt for DownloadProvider implicit dependencies
        Injekt.addSingleton<StorageManager>(storageManager)
        val mockSourceManager = mockk<SourceManager>()
        val mockMangaDex = mockk<MangaDex>()
        every { mockSourceManager.mangaDex } returns mockMangaDex
        Injekt.addSingleton<SourceManager>(mockSourceManager)
    }

    @After
    fun tearDown() {
        try {
            unmockkStatic("org.nekomanga.data.database.model.MangaMappersKt")
            unmockkConstructor(DownloadProvider::class)
        } finally {
            Dispatchers.resetMain()
            // clear Injekt modules
            val fields = Injekt::class.java.declaredFields
            for (field in fields) {
                if (field.name == "registrars") {
                    field.isAccessible = true
                    val map = field.get(Injekt) as MutableMap<*, *>
                    map.clear()
                }
            }
        }
    }

    @Test
    fun `given alt title different from previous when setting alt title then changes are saved and directories renamed`() =
        runTest {
            // Arrange
            val mangaId = 1L
            val oldTitle = "Old Title"
            val newTitle = "New Title"

            val mockMangaEntity = mockk<MangaEntity>(relaxed = true)
            every { mockMangaEntity.id } returns mangaId
            every { mockMangaEntity.title } returns oldTitle
            every { mockMangaEntity.userTitle } returns null

            val mockManga = mockk<Manga>(relaxed = true)
            every { mockMangaEntity.toManga() } returns mockManga
            every { mockManga.id } returns mangaId
            every { mockManga.title } returns oldTitle
            every { mockManga.user_title } returns null

            coEvery { mangaRepository.getMangaById(mangaId) } returns mockMangaEntity
            coEvery { mangaRepository.insertManga(any()) } returns mangaId

            val mockContext = mockk<Context>()
            every { preferences.context } returns mockContext

            every { anyConstructed<DownloadProvider>().renameMangaFolder(any(), any()) } returns
                Unit
            every { downloadManager.updateDownloadCacheForManga(any()) } returns Unit
            every { storageManager.renamePagesAndCoverDirectory(any(), any()) } returns Unit

            // Act
            val result = modifyMangaUseCase.setAltTitle(mangaId, newTitle)

            // Assert
            assertNotNull(result)
            coVerify(exactly = 1) { mockManga.user_title = newTitle }
            coVerify(exactly = 1) { mangaRepository.insertManga(any()) }
            coVerify(exactly = 1) {
                anyConstructed<DownloadProvider>().renameMangaFolder(oldTitle, newTitle)
            }
            coVerify(exactly = 1) { downloadManager.updateDownloadCacheForManga(any()) }
            coVerify(exactly = 1) {
                storageManager.renamePagesAndCoverDirectory(oldTitle, newTitle)
            }
        }
}
