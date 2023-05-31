package eu.kanade.tachiyomi.data.track.mdlist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.core.text.isDigitsOnly
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackList
import eu.kanade.tachiyomi.data.track.TrackListService
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.loggycat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MdList(private val context: Context, id: Int) : TrackListService(id) {

    private val mdex by lazy { Injekt.get<SourceManager>().mangaDex }

    private val mangaDexLoginHelper by lazy { Injekt.get<MangaDexLoginHelper>() }

    private var lists: List<TrackList> = emptyList()

    override fun nameRes() = R.string.mdlist
    override suspend fun populateLists() {
        mdex.fetchAllUserLists().onSuccess { resultListPage ->
            lists = resultListPage.results.map { TrackList(id = it.uuid, name = it.title) }
        }
    }

    override fun viewLists(): List<TrackList> {
        return lists
    }

    override suspend fun addToList(track: Track, listId: String): Track {
        return withContext(Dispatchers.IO) {
            kotlin.runCatching {
                if (mdex.addToCustomList(MdUtil.getMangaUUID(track.tracking_url), listId)) {
                    track.listIds = (track.listIds + listOf(listId)).distinct()
                    db.insertTrack(track).executeAsBlocking()
                }
            }.onFailure { e ->
                loggycat(LogPriority.ERROR, e) { "error updating MDList" }
            }

            track
        }
    }

    override suspend fun removeFromList(track: Track, listId: String): Track {
        return withContext(Dispatchers.IO) {
            kotlin.runCatching {
                if (mdex.removeFromCustomList(MdUtil.getMangaUUID(track.tracking_url), listId)) {
                    track.listIds = track.listIds.filterNot { it == listId }
                    db.insertTrack(track).executeAsBlocking()
                }
            }.onFailure { e ->
                loggycat(LogPriority.ERROR, e) { "error updating MDList" }
            }

            track
        }
    }

    override fun isAutoAddTracker() = true

    override fun getLogo(): Int {
        return R.drawable.ic_tracker_mangadex_logo
    }

    override fun getLogoColor(): Int {
        return Color.rgb(43, 48, 53)
    }

    override fun getScoreList() = IntRange(0, 10).map(Int::toString)

    override fun displayScore(track: Track) = track.score.toInt().toString()

    override suspend fun add(track: Track): Track {
        throw Exception("Not Used")
    }

    suspend fun updateScore(track: Track) {
        withContext(Dispatchers.IO) {
            mdex.updateRating(track)
        }
    }

    override suspend fun bind(track: Track): Track {
        if (MdUtil.getMangaUUID(track.tracking_url).isDigitsOnly()) {
            loggycat(LogPriority.INFO) { "v3 tracking ${track.tracking_url} skipping bind" }
            return track
        }
        val remoteTrack = mdex.fetchTrackingInfo(track.tracking_url)
        track.copyPersonalFrom(remoteTrack)
        db.insertTrack(track).executeAsBlocking()
        return track
    }

    override suspend fun refresh(track: Track): Track {
        if (MdUtil.getMangaUUID(track.tracking_url).isDigitsOnly()) {
            loggycat(LogPriority.INFO) { "v3 tracking ${track.tracking_url} skipping bind" }
            return track
        }
        val remoteTrack = mdex.fetchTrackingInfo(track.tracking_url)
        track.copyPersonalFrom(remoteTrack)
        track.total_chapters = remoteTrack.total_chapters
        return track
    }

    fun createInitialTracker(manga: Manga): Track {
        val track = Track.create(TrackManager.MDLIST)
        track.manga_id = manga.id!!
        track.tracking_url = MdConstants.baseUrl + manga.url
        track.title = manga.title
        return track
    }

    override suspend fun search(
        query: String,
        manga: Manga,
        wasPreviouslyTracked: Boolean,
    ): List<TrackSearch> {
        val track = TrackSearch.create(TrackManager.MDLIST).apply {
            this.manga_id = manga.id!!
            this.tracking_url = MdConstants.baseUrl + manga.url
            this.title = manga.title
        }

        return listOf(track)
    }

    override suspend fun login(username: String, password: String): Boolean =
        throw Exception("not used")

    @SuppressLint("MissingSuperCall")
    override fun logout() = throw Exception("not used")

    override fun isLogged() = mangaDexLoginHelper.isLoggedIn()

    override fun isMdList() = true
}
