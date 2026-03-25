package org.nekomanga.usecases.tracking

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.util.system.executeOnIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.toChapterItem
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.track.toDbTrack
import org.nekomanga.domain.track.toTrackItem
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class RefreshTrackingUseCase(
    private val db: DatabaseHelper = Injekt.get(),
    private val trackManager: TrackManager = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
) {

    suspend fun refreshTracking(
        mangaId: Long,
        onRefreshError: suspend (Throwable, Int, String?) -> Unit,
        onChaptersToMarkRead: suspend (List<ChapterItem>) -> Unit,
    ) {
        val tracks = db.getTracks(mangaId).executeOnIO()
        if (tracks.isEmpty()) return

        val updatedTracks = coroutineScope {
            tracks
                .map { it.toTrackItem() }
                .mapNotNull { track ->
                    trackManager
                        .getService(track.trackServiceId)
                        ?.takeIf { it.isLogged() }
                        ?.let { service -> track to service }
                }
                .map { (trackItem, service) ->
                    async {
                        kotlin
                            .runCatching { service.refresh(trackItem.toDbTrack()) }
                            .onFailure { error ->
                                if (error !is CancellationException) {
                                    TimberKt.e(error) {
                                        "Error refreshing tracker: ${service.nameRes()}"
                                    }
                                    onRefreshError(error, service.nameRes(), error.message)
                                }
                            }
                            .getOrNull()
                            ?.also { updatedTrack -> db.insertTrack(updatedTrack).executeOnIO() }
                    }
                }
                .awaitAll()
                .filterNotNull()
        }

        if (updatedTracks.isEmpty()) return
        if (!preferences.syncChaptersWithTracker().get()) return

        val maxChapterRead = updatedTracks.maxOfOrNull { it.last_chapter_read } ?: 0f

        if (maxChapterRead > 0) {
            val allChapters =
                db.getChapters(mangaId).executeOnIO().mapNotNull {
                    it.toSimpleChapter()?.toChapterItem()
                }

            val chaptersToMark = allChapters.filter {
                !it.chapter.read &&
                    it.chapter.chapterNumber >= 0f &&
                    it.chapter.chapterNumber <= maxChapterRead
            }

            if (chaptersToMark.isNotEmpty()) {
                onChaptersToMarkRead(chaptersToMark)
            }
        }
    }
}
