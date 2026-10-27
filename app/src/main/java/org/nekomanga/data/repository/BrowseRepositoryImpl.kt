package org.nekomanga.data.repository

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.manga.toDisplayManga
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.ScanlatorGroupRepository
import org.nekomanga.data.database.repository.UploaderRepository
import org.nekomanga.domain.browse.BrowseHomeSection
import org.nekomanga.domain.filter.DexFilters
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BrowseRepositoryImpl(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().mangaDex,
    private val loginHelper: MangaDexLoginHelper = Injekt.get(),
    private val mangaRepository: MangaRepository = Injekt.get(),
    private val scanlatorGroupRepository: ScanlatorGroupRepository = Injekt.get(),
    private val uploaderRepository: UploaderRepository = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
) : BrowseRepository {

    override fun isLoggedIn(): Boolean = loginHelper.isLoggedIn()

    override fun isLoggedInFlow(): Flow<Boolean> = loginHelper.isLoggedInFlow()

    override suspend fun getRandomManga(): Result<DisplayManga, ResultError> {
        return mangaDex.getRandomManga().andThen {
            val displayManga = it.toDisplayManga(mangaRepository, mangaDex.id)
            Ok(displayManga)
        }
    }

    override suspend fun getSearchPage(
        page: Int,
        filters: DexFilters,
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex.search(page, filters).andThen { mangaListPage ->
            val displayMangaList =
                mangaListPage.sourceManga.toDisplayManga(mangaRepository, mangaDex.id)
            Ok(Pair(mangaListPage.hasNextPage, displayMangaList))
        }
    }

    override suspend fun getHomePage(): Result<List<BrowseHomeSection>, ResultError> {
        val blockedGroupNames = mangaDexPreferences.blockedGroups().get().toList()
        val groupChunks = blockedGroupNames.chunked(900)
        val existingGroups = groupChunks.flatMap { chunk ->
            scanlatorGroupRepository.getScanlatorGroupsByNames(chunk)
        }
        val existingGroupNames = existingGroups.map { it.name }.toSet()
        val missingGroupNames = blockedGroupNames.filterNot { it in existingGroupNames }

        val groupSemaphore = Semaphore(5)
        val fetchedGroups = coroutineScope {
            missingGroupNames
                .map { name ->
                    async { groupSemaphore.withPermit { mangaDex.getScanlatorGroup(group = name) } }
                }
                .awaitAll()
                .mapNotNull { result -> result.getOrElse { null }?.toScanlatorGroupImpl() }
        }

        if (fetchedGroups.isNotEmpty()) {
            scanlatorGroupRepository.insertScanlatorGroups(fetchedGroups)
        }
        val blockedGroupUUIDs = (existingGroups + fetchedGroups).map { it.uuid }

        val blockedUploaderNames = mangaDexPreferences.blockedUploaders().get().toList()
        val uploaderChunks = blockedUploaderNames.chunked(900)
        val existingUploaders = uploaderChunks.flatMap { chunk ->
            uploaderRepository.getUploadersByNames(chunk)
        }
        val existingUploaderNames = existingUploaders.map { it.username }.toSet()
        val missingUploaderNames = blockedUploaderNames.filterNot { it in existingUploaderNames }

        val uploaderSemaphore = Semaphore(5)
        val fetchedUploaders = coroutineScope {
            missingUploaderNames
                .map { name ->
                    async { uploaderSemaphore.withPermit { mangaDex.getUploader(uploader = name) } }
                }
                .awaitAll()
                .mapNotNull { result -> result.getOrElse { null }?.toUploaderImpl() }
        }

        if (fetchedUploaders.isNotEmpty()) {
            uploaderRepository.insertUploaders(fetchedUploaders)
        }
        val blockedUploaderUUIDs = (existingUploaders + fetchedUploaders).map { it.uuid }

        return mangaDex.fetchHomePageInfo(blockedGroupUUIDs, blockedUploaderUUIDs).andThen {
            listResults ->
            Ok(
                listResults.map { listResult ->
                    BrowseHomeSection(
                        displayScreenType = listResult.displayScreenType,
                        mangaList =
                            listResult.sourceManga
                                .toDisplayManga(mangaRepository, mangaDex.id)
                                .distinctBy { it.url },
                    )
                }
            )
        }
    }

    override suspend fun getFollows(): Result<List<DisplayManga>, ResultError> {
        return mangaDex.fetchAllFollows().andThen { sourceManga ->
            Ok(sourceManga.toDisplayManga(mangaRepository, mangaDex.id))
        }
    }
}
