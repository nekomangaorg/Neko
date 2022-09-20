package eu.kanade.tachiyomi.ui.similar

import androidx.annotation.StringRes
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.handlers.SimilarHandler
import eu.kanade.tachiyomi.util.system.logTimeTaken
import eu.kanade.tachiyomi.util.toDisplayManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.nekomanga.domain.manga.DisplayManga
import org.nekomanga.domain.manga.SourceManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SimilarRepository {

    private val similarHandler: SimilarHandler by injectLazy()
    private val db: DatabaseHelper by injectLazy()
    private val mangaDex: MangaDex by lazy { Injekt.get<SourceManager>().getMangadex() }

    suspend fun fetchSimilar(
        dexId: String,
        forceRefresh: Boolean = false,
    ): List<SimilarMangaGroup> {
        return withContext(Dispatchers.IO) {
            val similarDbEntry = db.getSimilar(dexId).executeAsBlocking()
            val actualRefresh = when (similarDbEntry == null) {
                true -> true
                false -> forceRefresh
            }

            val related = async {
                kotlin.runCatching {
                    logTimeTaken(" Related Rec:") {
                        createGroup(
                            R.string.related_type,
                            similarHandler.fetchRelated(dexId, actualRefresh),
                        )
                    }
                }.onFailure { XLog.e("Failed to get related", it) }
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
                }.onFailure { XLog.e("Failed to get similar", it) }
                    .getOrNull()
            }

            val mu = async {
                runCatching {
                    logTimeTaken("MU Recs:") {
                        createGroup(
                            R.string.manga_updates,
                            similarHandler.fetchSimilarExternalMUManga(
                                dexId,
                                actualRefresh,
                            ),
                        )
                    }
                }.onFailure { XLog.e("Failed to get MU recs", it) }
                    .getOrNull()
            }

            val anilist = async {
                runCatching {
                    logTimeTaken("Anilist Recs:") {
                        createGroup(
                            R.string.anilist,
                            similarHandler.fetchAnilist(
                                dexId,
                                actualRefresh,
                            ),
                        )
                    }
                }.onFailure { XLog.e("Failed to get anilist recs", it) }
                    .getOrNull()
            }

            val mal = async {
                runCatching {
                    logTimeTaken("Mal Recs:") {
                        createGroup(
                            R.string.myanimelist,
                            similarHandler.fetchSimilarExternalMalManga(
                                dexId,
                                actualRefresh,
                            ),
                        )
                    }
                }.onFailure { XLog.e("Failed to get mal recs", it) }
                    .getOrNull()
            }

            listOfNotNull(related.await(), similar.await(), mu.await(), anilist.await(), mal.await())
        }
    }

    private fun createGroup(@StringRes id: Int, manga: List<SourceManga>): SimilarMangaGroup? {
        return if (manga.isEmpty()) {
            null
        } else {
            SimilarMangaGroup(
                id,
                manga.map {
                    it.toDisplayManga(db, mangaDex.id)
                },
            )
        }
    }
}

data class SimilarMangaGroup(@StringRes val type: Int, val manga: List<DisplayManga>)
