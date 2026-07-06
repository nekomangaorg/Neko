package org.nekomanga.usecases.manga

import eu.kanade.tachiyomi.ui.manga.MangaConstants
import eu.kanade.tachiyomi.ui.manga.MangaUpdateCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.nekomanga.data.database.repository.ScanlatorGroupRepository
import org.nekomanga.data.database.repository.UploaderRepository
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BlockScanlator(
    private val scanlatorGroupRepository: ScanlatorGroupRepository = Injekt.get(),
    private val uploaderRepository: UploaderRepository = Injekt.get(),
    private val mangaUpdateCoordinator: MangaUpdateCoordinator = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) {
    suspend fun block(blockType: MangaConstants.BlockType, name: String, scope: CoroutineScope) {
        when (blockType) {
            MangaConstants.BlockType.Group -> {
                val scanlatorGroupImpl = scanlatorGroupRepository.getScanlatorGroupByName(name)
                if (scanlatorGroupImpl == null) {
                    scope.launch { mangaUpdateCoordinator.updateGroup(name) }
                }
                val blockedGroups = mangaDexPreferences.blockedGroups().get().toMutableSet()
                blockedGroups.add(name)
                mangaDexPreferences.blockedGroups().set(blockedGroups)
            }

            MangaConstants.BlockType.Uploader -> {
                val uploaderImpl = uploaderRepository.getUploaderByName(name)
                if (uploaderImpl == null) {
                    scope.launch { mangaUpdateCoordinator.updateUploader(name) }
                }
                val blockedUploaders =
                    mangaDexPreferences.blockedUploaders().get().toMutableSet()
                blockedUploaders.add(name)
                mangaDexPreferences.blockedUploaders().set(blockedUploaders)
            }
        }
    }

    suspend fun unblock(blockType: MangaConstants.BlockType, name: String) {
        when (blockType) {
            MangaConstants.BlockType.Group -> {
                scanlatorGroupRepository.deleteScanlatorGroup(name)
                val allBlockedGroups =
                    mangaDexPreferences.blockedGroups().get().toMutableSet()
                allBlockedGroups.remove(name)
                mangaDexPreferences.blockedGroups().set(allBlockedGroups)
            }

            MangaConstants.BlockType.Uploader -> {
                uploaderRepository.deleteUploader(name)
                val allBlockedUploaders =
                    mangaDexPreferences.blockedUploaders().get().toMutableSet()
                allBlockedUploaders.remove(name)
                mangaDexPreferences.blockedUploaders().set(allBlockedUploaders)
            }
        }
    }
}
