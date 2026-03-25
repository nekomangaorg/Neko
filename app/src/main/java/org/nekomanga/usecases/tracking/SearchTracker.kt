package org.nekomanga.usecases.tracking

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toTrackSearchItem
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SearchTracker(private val trackManager: TrackManager = Injekt.get()) {
    suspend fun await(
        title: String,
        service: TrackServiceItem,
        manga: Manga,
        previouslyTracker: Boolean,
    ): Flow<TrackingConstants.TrackSearchResult> =
        flow {
                emit(TrackingConstants.TrackSearchResult.Loading)

                val id = trackManager.getIdFromManga(service, manga) ?: ""

                val results =
                    trackManager.getService(service.id)!!.search(title, manga, previouslyTracker)
                emit(
                    when (results.isEmpty()) {
                        true -> TrackingConstants.TrackSearchResult.NoResult
                        false ->
                            TrackingConstants.TrackSearchResult.Success(
                                results.map { it.toTrackSearchItem() }.toPersistentList(),
                                hasMatchingId = id.isNotEmpty(),
                            )
                    }
                )
            }
            .catch {
                TimberKt.e(it) { "error searching tracker" }
                emit(
                    TrackingConstants.TrackSearchResult.Error(
                        it.message ?: "Error searching tracker",
                        service.nameRes,
                    )
                )
            }

    suspend fun awaitNonFlow(
        title: String,
        service: TrackServiceItem,
        manga: Manga,
        previouslyTracker: Boolean,
    ): TrackingConstants.TrackSearchResult {
        return kotlin
            .runCatching {
                val results =
                    trackManager.getService(service.id)!!.search(title, manga, previouslyTracker)
                when (results.isEmpty()) {
                    true -> TrackingConstants.TrackSearchResult.NoResult
                    false ->
                        TrackingConstants.TrackSearchResult.Success(
                            results.map { it.toTrackSearchItem() }.toPersistentList()
                        )
                }
            }
            .getOrElse {
                TimberKt.e(it) { "error searching tracker" }
                TrackingConstants.TrackSearchResult.Error(
                    it.message ?: "Error searching tracker",
                    service.nameRes,
                )
            }
    }
}
