package eu.kanade.tachiyomi.data.track.mdlist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MdList(private val context: Context, id: Int) : TrackService(id) {

    private val mdex by lazy { Injekt.get<SourceManager>().getMangadex() as HttpSource }
    private val db: DatabaseHelper by lazy { Injekt.get<DatabaseHelper>() }

    override val name = "MDList"

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

    override fun getScoreList() = IntRange(0, 10).map(Int::toString)

    override fun displayScore(track: Track) = track.score.toInt().toString()

    override suspend fun update(track: Track): Track {
        return withContext(Dispatchers.IO) {
            val manga = db.getManga(track.tracking_url.substringAfter(".org"), mdex.id)
                .executeAsBlocking()!!
            val followStatus = FollowStatus.fromInt(track.status)!!

            // allow follow status to update
            if (manga.follow_status != followStatus) {
                mdex.updateFollowStatus(MdUtil.getMangaId(track.tracking_url), followStatus)
                manga.follow_status = followStatus
                db.insertManga(manga).executeAsBlocking()
            }

            // mangadex wont update chapters if manga is not follows this prevents unneeded network call
            if (followStatus != FollowStatus.UNFOLLOWED) {
                if (track.total_chapters != 0 && track.last_chapter_read == track.total_chapters) {
                    track.status = FollowStatus.COMPLETED.int
                }
                mdex.updateReadingProgress(track)
            }
            db.insertTrack(track).executeAsBlocking()
            track
        }
    }

    override fun isCompletedStatus(index: Int) =
        getStatusList()[index] == FollowStatus.COMPLETED.int

    override suspend fun bind(track: Track): Track {
        val remoteTrack = mdex.fetchTrackingInfo(track.tracking_url)
        track.copyPersonalFrom(remoteTrack)
        track.library_id = remoteTrack.library_id
        return update(track)
    }

    override suspend fun refresh(track: Track): Track {
        TODO("Not yet implemented")
    }

    fun createInitialTracker(manga: Manga): Track {
        val track = Track.create(TrackManager.MDLIST)
        track.manga_id = manga.id!!
        track.status = FollowStatus.UNFOLLOWED.int
        track.tracking_url = MdUtil.baseUrl + manga.url
        track.title = manga.title
        return track
    }

    override suspend fun search(query: String): List<TrackSearch> = throw Exception("not used")

    override suspend fun login(username: String, password: String): Boolean = throw Exception("not used")

    @SuppressLint("MissingSuperCall")
    override fun logout() = throw Exception("not used")

    override val isLogged = mdex.isLogged()

    override fun isMdList() = true
}
