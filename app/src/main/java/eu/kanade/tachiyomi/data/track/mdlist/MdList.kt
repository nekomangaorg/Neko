package eu.kanade.tachiyomi.data.track.mdlist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.core.text.isDigitsOnly
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.constants.MdConstants
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MdList(private val context: Context, id: Int) : TrackService(id) {

    private val mdex by lazy { Injekt.get<SourceManager>().mangaDex }

    private val mangaDexLoginHelper by lazy { Injekt.get<MangaDexLoginHelper>() }

    override fun nameRes() = R.string.mdlist

    override fun isAutoAddTracker() = true

    override fun getLogo(): Int {
        return R.drawable.ic_tracker_mangadex_logo
    }

    override fun getLogoColor(): Int {
        return Color.rgb(43, 48, 53)
    }

    override fun getStatusList(): List<Int> {
        return FollowStatus.values().map { it.int }
    }

    override fun getStatus(status: Int): String =
        context.resources.getStringArray(R.array.follows_options).asList()[status]

    override fun getGlobalStatus(status: Int): String = getStatus(status)

    override fun getScoreList() = IntRange(0, 10).map(Int::toString)

    override fun displayScore(track: Track) = track.score.toInt().toString()

    override suspend fun add(track: Track): Track {
        throw Exception("Not Used")
    }

    suspend fun updateScore(track: Track) {
        withContext(Dispatchers.IO) { mdex.updateRating(track) }
    }

    override suspend fun update(track: Track, setToRead: Boolean): Track {
        return withContext(Dispatchers.IO) {
            try {
                val manga =
                    db.getManga(track.tracking_url.substringAfter(".org"), mdex.id)
                        .executeAsBlocking() ?: return@withContext track
                val followStatus = FollowStatus.fromInt(track.status)

                // allow follow status to update
                mdex.updateFollowStatus(MdUtil.getMangaUUID(track.tracking_url), followStatus)
                manga.follow_status = followStatus
                db.insertManga(manga).executeAsBlocking()

                // mangadex wont update chapters if manga is not follows this prevents unneeded
                // network call

                if (followStatus != FollowStatus.UNFOLLOWED) {
                    if (track.total_chapters != 0 &&
                        track.last_chapter_read.toInt() == track.total_chapters) {
                        track.status = FollowStatus.COMPLETED.int
                        mdex.updateFollowStatus(
                            MdUtil.getMangaUUID(track.tracking_url),
                            FollowStatus.COMPLETED,
                        )
                    }
                    if (followStatus == FollowStatus.PLAN_TO_READ && track.last_chapter_read > 0) {
                        val newFollowStatus = FollowStatus.READING
                        track.status = FollowStatus.READING.int
                        mdex.updateFollowStatus(
                            MdUtil.getMangaUUID(track.tracking_url),
                            newFollowStatus,
                        )
                        manga.follow_status = newFollowStatus
                        db.insertManga(manga).executeAsBlocking()
                    }
                    mdex.updateReadingProgress(track)
                } else if (track.last_chapter_read.toInt() != 0) {
                    // When followStatus has been changed to unfollowed 0 out read chapters since
                    // dex does
                    track.last_chapter_read = 0f
                }
            } catch (e: Exception) {
                TimberKt.e(e) { "error updating MDList" }
            }
            db.insertTrack(track).executeAsBlocking()
            track
        }
    }

    override fun isCompletedStatus(index: Int) =
        getStatusList()[index] == FollowStatus.COMPLETED.int

    override fun completedStatus() = FollowStatus.COMPLETED.int

    override fun readingStatus() = FollowStatus.READING.int

    override fun planningStatus() = FollowStatus.PLAN_TO_READ.int

    override suspend fun bind(track: Track): Track {
        if (MdUtil.getMangaUUID(track.tracking_url).isDigitsOnly()) {
            TimberKt.i { "v3 tracking ${track.tracking_url} skipping bind" }
            return track
        }
        val remoteTrack = mdex.fetchTrackingInfo(track.tracking_url)
        track.copyPersonalFrom(remoteTrack)
        return update(track)
    }

    override suspend fun refresh(track: Track): Track {
        if (MdUtil.getMangaUUID(track.tracking_url).isDigitsOnly()) {
            TimberKt.i { "v3 tracking ${track.tracking_url} skipping bind" }
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
        track.status = FollowStatus.UNFOLLOWED.int
        track.tracking_url = MdConstants.baseUrl + manga.url
        track.title = manga.title
        return track
    }

    override suspend fun search(
        query: String,
        manga: Manga,
        wasPreviouslyTracked: Boolean,
    ): List<TrackSearch> {
        val track =
            TrackSearch.create(TrackManager.MDLIST).apply {
                this.manga_id = manga.id!!
                this.status = FollowStatus.UNFOLLOWED.int
                this.tracking_url = MdConstants.baseUrl + manga.url
                this.title = manga.title
            }

        return listOf(track)
    }

    override suspend fun login(username: String, password: String): Boolean =
        throw Exception("not used")

    @SuppressLint("MissingSuperCall") override fun logout() = throw Exception("not used")

    override fun isLogged() = mangaDexLoginHelper.isLoggedIn()

    override fun isMdList() = true

    fun isUnfollowed(track: Track) = track.status == 0
}
