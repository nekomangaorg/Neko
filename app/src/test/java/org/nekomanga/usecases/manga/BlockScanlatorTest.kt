package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaUpdateCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.nekomanga.data.database.repository.ScanlatorGroupRepository
import org.nekomanga.data.database.repository.UploaderRepository
import org.nekomanga.domain.site.MangaDexPreferences
import tachiyomi.core.preference.Preference

class BlockScanlatorTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var scanlatorGroupRepository: ScanlatorGroupRepository
    private lateinit var uploaderRepository: UploaderRepository
    private lateinit var mangaUpdateCoordinator: MangaUpdateCoordinator
    private lateinit var mangaDexPreferences: MangaDexPreferences
    private lateinit var blockScanlator: BlockScanlator

    private val blockedGroupsPreference = mockk<Preference<Set<String>>>()
    private val blockedUploadersPreference = mockk<Preference<Set<String>>>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        scanlatorGroupRepository = mockk()
        uploaderRepository = mockk()
        mangaUpdateCoordinator = mockk()
        mangaDexPreferences = mockk()

        every { mangaDexPreferences.blockedGroups() } returns blockedGroupsPreference
        every { mangaDexPreferences.blockedUploaders() } returns blockedUploadersPreference

        blockScanlator =
            BlockScanlator(
                scanlatorGroupRepository = scanlatorGroupRepository,
                uploaderRepository = uploaderRepository,
                mangaUpdateCoordinator = mangaUpdateCoordinator,
                mangaDexPreferences = mangaDexPreferences,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `block group when group exists in db should only update preferences`() = runTest {
        // Arrange
        val name = "TestGroup"
        val mockGroup = mockk<eu.kanade.tachiyomi.data.database.models.ScanlatorGroupImpl>()
        coEvery { scanlatorGroupRepository.getScanlatorGroupByName(name) } returns mockGroup
        every { blockedGroupsPreference.get() } returns emptySet()
        every { blockedGroupsPreference.set(any()) } just runs

        // Act
        blockScanlator.block(MangaConstants.BlockType.Group, name, this)

        // Assert
        coVerify(exactly = 1) { scanlatorGroupRepository.getScanlatorGroupByName(name) }
        coVerify(exactly = 0) { mangaUpdateCoordinator.updateGroup(any()) }
        verify(exactly = 1) { blockedGroupsPreference.set(setOf(name)) }
    }

    @Test
    fun `block group when group does not exist in db should update coordinator and preferences`() =
        runTest {
            // Arrange
            val name = "TestGroup"
            coEvery { scanlatorGroupRepository.getScanlatorGroupByName(name) } returns null
            coEvery { mangaUpdateCoordinator.updateGroup(name) } just runs
            every { blockedGroupsPreference.get() } returns emptySet()
            every { blockedGroupsPreference.set(any()) } just runs

            // Act
            blockScanlator.block(MangaConstants.BlockType.Group, name, this)
            runCurrent()

            // Assert
            coVerify(exactly = 1) { scanlatorGroupRepository.getScanlatorGroupByName(name) }
            coVerify(exactly = 1) { mangaUpdateCoordinator.updateGroup(name) }
            verify(exactly = 1) { blockedGroupsPreference.set(setOf(name)) }
        }

    @Test
    fun `block uploader when uploader exists in db should only update preferences`() = runTest {
        // Arrange
        val name = "TestUploader"
        val mockUploader = mockk<eu.kanade.tachiyomi.data.database.models.UploaderImpl>()
        coEvery { uploaderRepository.getUploaderByName(name) } returns mockUploader
        every { blockedUploadersPreference.get() } returns emptySet()
        every { blockedUploadersPreference.set(any()) } just runs

        // Act
        blockScanlator.block(MangaConstants.BlockType.Uploader, name, this)

        // Assert
        coVerify(exactly = 1) { uploaderRepository.getUploaderByName(name) }
        coVerify(exactly = 0) { mangaUpdateCoordinator.updateUploader(any()) }
        verify(exactly = 1) { blockedUploadersPreference.set(setOf(name)) }
    }

    @Test
    fun `block uploader when uploader does not exist in db should update coordinator and preferences`() =
        runTest {
            // Arrange
            val name = "TestUploader"
            coEvery { uploaderRepository.getUploaderByName(name) } returns null
            coEvery { mangaUpdateCoordinator.updateUploader(name) } just runs
            every { blockedUploadersPreference.get() } returns emptySet()
            every { blockedUploadersPreference.set(any()) } just runs

            // Act
            blockScanlator.block(MangaConstants.BlockType.Uploader, name, this)
            runCurrent()

            // Assert
            coVerify(exactly = 1) { uploaderRepository.getUploaderByName(name) }
            coVerify(exactly = 1) { mangaUpdateCoordinator.updateUploader(name) }
            verify(exactly = 1) { blockedUploadersPreference.set(setOf(name)) }
        }

    @Test
    fun `unblock group should delete group and update preferences`() = runTest {
        // Arrange
        val name = "TestGroup"
        coEvery { scanlatorGroupRepository.deleteScanlatorGroup(name) } returns Unit
        every { blockedGroupsPreference.get() } returns setOf(name)
        every { blockedGroupsPreference.set(any()) } just runs

        // Act
        blockScanlator.unblock(MangaConstants.BlockType.Group, name)

        // Assert
        coVerify(exactly = 1) { scanlatorGroupRepository.deleteScanlatorGroup(name) }
        verify(exactly = 1) { blockedGroupsPreference.set(emptySet()) }
    }

    @Test
    fun `unblock uploader should delete uploader and update preferences`() = runTest {
        // Arrange
        val name = "TestUploader"
        coEvery { uploaderRepository.deleteUploader(name) } returns Unit
        every { blockedUploadersPreference.get() } returns setOf(name)
        every { blockedUploadersPreference.set(any()) } just runs

        // Act
        blockScanlator.unblock(MangaConstants.BlockType.Uploader, name)

        // Assert
        coVerify(exactly = 1) { uploaderRepository.deleteUploader(name) }
        verify(exactly = 1) { blockedUploadersPreference.set(emptySet()) }
    }
}
