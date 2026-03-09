package eu.kanade.tachiyomi.ui.similar

import androidx.annotation.StringRes
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapBoth
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.util.manga.toDisplayManga
import eu.kanade.tachiyomi.util.system.logTimeTaken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.SourceManga
import org.nekomanga.domain.network.ResultError
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SimilarRepository {

    private val similarHandler: SimilarHandler by injectLazy()
    private val db: DatabaseHelper by injectLazy()
    private val mangaDex: MangaDex by lazy { Injekt.get<SourceManager>().mangaDex }

    suspend fun fetchSimilar(
        dexId: String,
        forceRefresh: Boolean = false,
    ): Result<List<SimilarMangaGroup>, ResultError> {

        return withContext(Dispatchers.IO) {
            val similarDbEntry = db.getSimilar(dexId).executeAsBlocking()
            val actualRefresh =
                when (similarDbEntry == null) {
                    true -> true
                    false -> forceRefresh
                }

            val relatedAsync =
                fetchSimilarGroupAsync(
                    " Related Rec:",
                    { similarHandler.fetchRelated(dexId, actualRefresh) },
                    R.string.related_type,
                    "Failed to get related",
                )

            val recommendedAsync =
                fetchSimilarGroupAsync(
                    " Recommended Rec:",
                    { similarHandler.fetchRecommended(dexId, actualRefresh) },
                    R.string.recommended_type,
                    "Failed to get recommended",
                )

            val similarAsync =
                fetchSimilarGroupAsync(
                    "Similar Recs:",
                    { similarHandler.fetchSimilar(dexId, actualRefresh) },
                    R.string.similar_type,
                    "Failed to get similar",
                )

            val muAsync =
                fetchSimilarGroupAsync(
                    "MU Recs:",
                    { similarHandler.fetchSimilarExternalMUManga(dexId, actualRefresh) },
                    R.string.manga_updates,
                    "Failed to get MU recs",
                )

            val anilistAsync =
                fetchSimilarGroupAsync(
                    "Anilist Recs:",
                    { similarHandler.fetchAnilist(dexId, actualRefresh) },
                    R.string.anilist,
                    "Failed to get anilist recs",
                )

            val malAsync =
                fetchSimilarGroupAsync(
                    "Mal Recs:",
                    { similarHandler.fetchSimilarExternalMalManga(dexId, actualRefresh) },
                    R.string.myanimelist,
                    "Failed to get mal recs",
                )

            val rawResults =
                listOfNotNull(
                    relatedAsync.await(),
                    recommendedAsync.await(),
                    similarAsync.await(),
                    muAsync.await(),
                    anilistAsync.await(),
                    malAsync.await(),
                )

            val errors = rawResults.filterIsInstance<ResultError>()
            val groups = rawResults.filterIsInstance<SimilarMangaGroup>()

            if (groups.isEmpty() && errors.isNotEmpty()) {
                Err(errors.first())
            } else {
                Ok(groups)
            }
        }
    }

    private fun CoroutineScope.fetchSimilarGroupAsync(
        logMessage: String,
        fetchCall: suspend () -> Result<List<SourceManga>, ResultError>,
        @StringRes titleRes: Int,
        errorLogMessage: String,
    ): Deferred<Any?> {
        return async {
            logTimeTaken(logMessage) {
                fetchCall()
                    .mapBoth(
                        success = { createGroup(titleRes, it) },
                        failure = {
                            TimberKt.e { "$errorLogMessage: $it" }
                            it
                        },
                    )
            }
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
