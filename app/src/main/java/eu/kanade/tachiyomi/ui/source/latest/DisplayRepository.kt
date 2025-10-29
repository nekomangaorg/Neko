package eu.kanade.tachiyomi.ui.source.latest

import androidx.annotation.StringRes
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.logTimeTaken
import eu.kanade.tachiyomi.util.toDisplayManga
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.domain.site.MangaDexPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DisplayRepository(
    private val mangaDex: MangaDex = Injekt.get<SourceManager>().mangaDex,
    private val db: DatabaseHelper = Injekt.get(),
    private val preferenceHelper: PreferencesHelper = Injekt.get(),
    private val mangaDexPreferences: MangaDexPreferences = Injekt.get(),
    private val similarHandler: SimilarHandler = Injekt.get(),
) {

    suspend fun getPage(
        page: Int,
        displayScreenType: DisplayScreenType,
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return when (displayScreenType) {
            is DisplayScreenType.LatestChapters -> getLatestChapterPage(page)
            is DisplayScreenType.List -> getListPage(displayScreenType.listUUID)
            is DisplayScreenType.RecentlyAdded -> getRecentlyAddedPage(page)
            is DisplayScreenType.PopularNewTitles -> getPopularNewTitles(page)
            is DisplayScreenType.FeedUpdates -> getFeedUpdatesPage(page)
            is DisplayScreenType.Similar ->
                error("Cannot call this with similar, should be calling getSimilarManga instead")
        }
    }

    private suspend fun getFeedUpdatesPage(
        page: Int
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        val blockedGroupUUIDs =
            mangaDexPreferences
                .blockedGroups()
                .get()
                .mapNotNull {
                    var scanlatorGroupImpl = db.getScanlatorGroupByName(it).executeAsBlocking()
                    if (scanlatorGroupImpl == null) {
                        mangaDex.getScanlatorGroup(group = it).map { scanlator ->
                            scanlatorGroupImpl = scanlator.toScanlatorGroupImpl()
                        }
                        db.insertScanlatorGroups(listOf(scanlatorGroupImpl!!)).executeOnIO()
                    }
                    scanlatorGroupImpl
                }
                .map { it.uuid }
        val blockedUploaderUUIDs =
            mangaDexPreferences
                .blockedUploaders()
                .get()
                .mapNotNull {
                    var uploaderImpl = db.getUploaderByName(it).executeAsBlocking()
                    if (uploaderImpl == null) {
                        mangaDex.getUploader(uploader = it).map { uploader ->
                            uploaderImpl = uploader.toUploaderImpl()
                        }
                        db.insertUploader(listOf(uploaderImpl!!)).executeOnIO()
                    }
                    uploaderImpl
                }
                .map { it.uuid }
        return mangaDex
            .feedUpdates(page, blockedGroupUUIDs, blockedUploaderUUIDs)
            .mapBoth(
                success = { mangaListPage ->
                    val displayMangaList =
                        mangaListPage.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(mangaListPage.hasNextPage to displayMangaList.toPersistentList())
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getLatestChapterPage(
        page: Int
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        val blockedGroupUUIDs =
            mangaDexPreferences
                .blockedGroups()
                .get()
                .mapNotNull {
                    var scanlatorGroupImpl = db.getScanlatorGroupByName(it).executeAsBlocking()
                    if (scanlatorGroupImpl == null) {
                        mangaDex
                            .getScanlatorGroup(group = it)
                            .map { group -> scanlatorGroupImpl = group.toScanlatorGroupImpl() }
                            .onSuccess {
                                db.insertScanlatorGroups(listOf(scanlatorGroupImpl!!)).executeOnIO()
                            }
                    }
                    scanlatorGroupImpl
                }
                .map { it.uuid }
        val blockedUploaderUUIDs =
            mangaDexPreferences
                .blockedUploaders()
                .get()
                .mapNotNull {
                    var uploaderImpl = db.getUploaderByName(it).executeAsBlocking()
                    if (uploaderImpl == null) {
                        mangaDex
                            .getUploader(uploader = it)
                            .map { uploader -> uploaderImpl = uploader.toUploaderImpl() }
                            .onSuccess { db.insertUploader(listOf(uploaderImpl!!)).executeOnIO() }
                    }
                    uploaderImpl
                }
                .map { it.uuid }
        return mangaDex
            .latestChapters(page, blockedGroupUUIDs, blockedUploaderUUIDs)
            .mapBoth(
                success = { mangaListPage ->
                    val displayMangaList =
                        mangaListPage.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(mangaListPage.hasNextPage to displayMangaList.toPersistentList())
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getListPage(
        listUUID: String
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex
            .fetchAllList(listUUID)
            .mapBoth(
                success = { listResults ->
                    val displayMangaList =
                        listResults.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(false to displayMangaList.toPersistentList())
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getRecentlyAddedPage(
        page: Int
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex
            .recentlyAdded(page)
            .mapBoth(
                success = { listResults ->
                    val displayMangaList =
                        listResults.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(listResults.hasNextPage to displayMangaList.toPersistentList())
                },
                failure = { Err(it) },
            )
    }

    private suspend fun getPopularNewTitles(
        page: Int
    ): Result<Pair<Boolean, List<DisplayManga>>, ResultError> {
        return mangaDex
            .popularNewTitles(page)
            .mapBoth(
                success = { listResults ->
                    val displayMangaList =
                        listResults.sourceManga.map { sourceManga ->
                            sourceManga.toDisplayManga(db, mangaDex.id)
                        }
                    Ok(listResults.hasNextPage to displayMangaList.toPersistentList())
                },
                failure = { Err(it) },
            )
    }

    suspend fun fetchSimilar(
        dexId: String,
        forceRefresh: Boolean = false,
    ): List<SimilarMangaGroup> {
        return withContext(Dispatchers.IO) {
            val similarDbEntry = db.getSimilar(dexId).executeAsBlocking()
            val actualRefresh =
                when (similarDbEntry == null) {
                    true -> true
                    false -> forceRefresh
                }

            val related = async {
                kotlin
                    .runCatching {
                        logTimeTaken(" Related Rec:") {
                            createGroup(
                                R.string.related_type,
                                similarHandler.fetchRelated(dexId, actualRefresh),
                            )
                        }
                    }
                    .onFailure { TimberKt.e(it) { "Failed to get related" } }
                    .getOrNull()
            }

            val recommended = async {
                kotlin
                    .runCatching {
                        logTimeTaken(" Recommended Rec:") {
                            createGroup(
                                R.string.recommended_type,
                                similarHandler.fetchRecommended(dexId, actualRefresh),
                            )
                        }
                    }
                    .onFailure { TimberKt.e(it) { "Failed to get recommended" } }
                    .getOrNull()
            }

            val similar = async {
                runCatching {
                        logTimeTaken("Similar Recs:") {
                            createGroup(
                                R.string.similar_type,
                                similarHandler.fetchSimilar(dexId, actualRefresh),
                            )
                        }
                    }
                    .onFailure { TimberKt.e(it) { "Failed to get similar" } }
                    .getOrNull()
            }

            val mu = async {
                runCatching {
                        logTimeTaken("MU Recs:") {
                            createGroup(
                                R.string.manga_updates,
                                similarHandler.fetchSimilarExternalMUManga(dexId, actualRefresh),
                            )
                        }
                    }
                    .onFailure { TimberKt.e(it) { "Failed to get MU recs" } }
                    .getOrNull()
            }

            val anilist = async {
                runCatching {
                        logTimeTaken("Anilist Recs:") {
                            createGroup(
                                R.string.anilist,
                                similarHandler.fetchAnilist(dexId, actualRefresh),
                            )
                        }
                    }
                    .onFailure { TimberKt.e(it) { "Failed to get anilist recs" } }
                    .getOrNull()
            }

            val mal = async {
                runCatching {
                        logTimeTaken("Mal Recs:") {
                            createGroup(
                                R.string.myanimelist,
                                similarHandler.fetchSimilarExternalMalManga(dexId, actualRefresh),
                            )
                        }
                    }
                    .onFailure { TimberKt.e(it) { "Failed to get mal recs" } }
                    .getOrNull()
            }

            listOfNotNull(
                related.await(),
                recommended.await(),
                similar.await(),
                mu.await(),
                anilist.await(),
                mal.await(),
            )
        }
    }

    private fun createGroup(@StringRes id: Int, manga: List<SourceManga>): SimilarMangaGroup? {
        return if (manga.isEmpty()) {
            null
        } else {
            SimilarMangaGroup(id, manga.map { it.toDisplayManga(db, mangaDex.id) })
        }
    }
}

data class SimilarMangaGroup(@param:StringRes val type: Int, val manga: List<DisplayManga>)
