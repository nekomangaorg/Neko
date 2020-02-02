package eu.kanade.tachiyomi.data.track.mdlist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MdList(private val context: Context, id: Int) : TrackService(id) {


    private val mdex by lazy { Injekt.get<SourceManager>().getMangadex() as HttpSource }

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

    override fun getStatus(status: Int): String = context.resources.getStringArray(R.array.follows_options).asList()[status]


    override fun getScoreList() = IntRange(0, 10).map(Int::toString)

    override fun displayScore(track: Track) = track.score.toInt().toString()

    
    override fun update(track: Track): Observable<Track> {
        if (track.total_chapters != 0 && track.last_chapter_read == track.total_chapters) {
            track.status = FollowStatus.COMPLETED.int
        }

        return Observable.just(track)
        //return api.updateLibManga(track)
    }


    override fun login(username: String, password: String) = throw Exception("not used")

    @SuppressLint("MissingSuperCall")
    override fun logout() = throw Exception("not used")

    override val isLogged = mdex.isLogged()

    override fun isMdList() = true

}
