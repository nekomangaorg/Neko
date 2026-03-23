package org.nekomanga.usecases.manga

import android.content.Context
import com.pushtorefresh.storio.sqlite.operations.get.PreparedGetObject
import com.pushtorefresh.storio.sqlite.operations.put.PreparedPutObject
import com.pushtorefresh.storio.sqlite.operations.put.PutResult
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.util.system.executeOnIO
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
import org.nekomanga.domain.storage.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.get

class ModifyMangaUseCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: DatabaseHelper
    private lateinit var preferences: PreferencesHelper
    private lateinit var downloadManager: DownloadManager
    private lateinit var storageManager: StorageManager
    private lateinit var modifyMangaUseCase: ModifyMangaUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = mockk()
        preferences = mockk()
        downloadManager = mockk()
        storageManager = mockk()

        modifyMangaUseCase = ModifyMangaUseCase(db, preferences, downloadManager, storageManager)

        mockkStatic("eu.kanade.tachiyomi.util.system.DatabaseExtensionsKt")
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
            unmockkStatic("eu.kanade.tachiyomi.util.system.DatabaseExtensionsKt")
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

            val mockManga = mockk<Manga>(relaxed = true)
            every { mockManga.title } returns oldTitle
            every { mockManga.user_title } returns null

            val mockGetManga = mockk<PreparedGetObject<Manga>>()
            every { db.getManga(mangaId) } returns mockGetManga
            coEvery { mockGetManga.executeOnIO() } returns mockManga

            val mockInsertManga = mockk<PreparedPutObject<Manga>>()
            every { db.insertManga(mockManga) } returns mockInsertManga
            val mockPutResult = mockk<PutResult>(relaxed = true)
            coEvery { mockInsertManga.executeOnIO() } returns mockPutResult

            val mockContext = mockk<Context>()
            every { preferences.context } returns mockContext

            every { anyConstructed<DownloadProvider>().renameMangaFolder(any(), any()) } returns
                Unit
            every { downloadManager.updateDownloadCacheForManga(mockManga) } returns Unit
            every { storageManager.renamePagesAndCoverDirectory(any(), any()) } returns Unit

            // Act
            val result = modifyMangaUseCase.setAltTitle(mangaId, newTitle)

            // Assert
            assertNotNull(result)
            coVerify(exactly = 1) { mockManga.user_title = newTitle }
            coVerify(exactly = 1) { mockInsertManga.executeOnIO() }
            coVerify(exactly = 1) {
                anyConstructed<DownloadProvider>().renameMangaFolder(oldTitle, newTitle)
            }
            coVerify(exactly = 1) { downloadManager.updateDownloadCacheForManga(mockManga) }
            coVerify(exactly = 1) {
                storageManager.renamePagesAndCoverDirectory(oldTitle, newTitle)
            }
        }
}
