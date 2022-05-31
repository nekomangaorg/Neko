package eu.kanade.tachiyomi.data.track.mangaupdates

import android.content.Context
import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.copyTo
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.toTrackSearch
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.updateNewTrackInfo

class MangaUpdates(private val context: Context, id: Int) : TrackService(id) {

    private val interceptor by lazy { MangaUpdatesInterceptor(this) }

    private val api by lazy { MangaUpdatesApi(interceptor, client) }

    @StringRes
    override fun nameRes(): Int = R.string.manga_updates

    override fun getLogo(): Int = R.drawable.ic_tracker_manga_updates_logo

    override fun getLogoColor(): Int = Color.rgb(146, 160, 173)

    override fun getStatusList(): List<Int> {
        return listOf(READING_LIST, COMPLETE_LIST, ON_HOLD_LIST, UNFINISHED_LIST, WISH_LIST)
    }

    override fun isCompletedStatus(index: Int) = getStatusList()[index] == COMPLETE_LIST

    override fun getStatus(status: Int): String =
        when (status) {
            READING_LIST -> "Reading List"
            WISH_LIST -> "Wish List"
            COMPLETE_LIST -> "Complete List "
            ON_HOLD_LIST -> "On Hold List"
            UNFINISHED_LIST -> "Unfinished List"
            else -> ""
        }

    override fun getGlobalStatus(status: Int) = with(context) {
        when (status) {
            READING_LIST -> getString(R.string.reading)
            COMPLETE_LIST -> getString(R.string.completed)
            ON_HOLD_LIST -> getString(R.string.on_hold)
            UNFINISHED_LIST -> getString(R.string.dropped)
            WISH_LIST -> getString(R.string.plan_to_read)
            else -> ""
        }
    }

    override fun readingStatus(): Int = READING_LIST

    override fun planningStatus(): Int = WISH_LIST

    override fun completedStatus(): Int = COMPLETE_LIST

    private val _scoreList = (0..9).flatMap { i -> (0..9).map { j -> "$i.$j" } } + listOf("10.0")

    override fun getScoreList(): List<String> = _scoreList

    override fun indexToScore(index: Int): Float = _scoreList[index].toFloat()

    override fun displayScore(track: Track): String = track.score.toString()

    override suspend fun add(track: Track): Track {
        track.score = DEFAULT_SCORE
        track.status = DEFAULT_STATUS
        updateNewTrackInfo(track, WISH_LIST)
        api.addSeriesToList(track)
        return track
    }

    override suspend fun update(track: Track, setToRead: Boolean): Track {
        updateTrackStatus(track, setToRead, setToComplete = true, mustReadToComplete = true)
        api.updateSeriesListItem(track)
        return track
    }

    override suspend fun bind(track: Track): Track {
        return try {
            val (series, rating) = api.getSeriesListItem(track)
            series.copyTo(track)
            update(rating?.copyTo(track) ?: track)
        } catch (e: Exception) {
            add(track)
        }
    }

//    override suspend fun bind(track: Track, hasReadChapters: Boolean): Track {
//        return try {
//            val (series, rating) = api.getSeriesListItem(track)
//            series.copyTo(track)
//            rating?.copyTo(track) ?: track
//        } catch (e: Exception) {
//            api.addSeriesToList(track, hasReadChapters)
//            track
//        }
//    }

    override suspend fun search(query: String, manga: Manga, wasPreviouslyTracked: Boolean): List<TrackSearch> {
        return api.search(query, manga, wasPreviouslyTracked)
            .map {
                it.toTrackSearch(id)
            }
    }

    override suspend fun refresh(track: Track): Track {
        val (series, rating) = api.getSeriesListItem(track)
        series.copyTo(track)
        return rating?.copyTo(track) ?: track
    }

    override fun canRemoveFromService(): Boolean = true

    override suspend fun removeFromService(track: Track): Boolean {
        return api.removeSeriesFromList(track)
    }

    override suspend fun login(username: String, password: String): Boolean {
        val authenticated = api.authenticate(username, password) ?: throw Throwable("Unable to login")
        saveCredentials(authenticated.uid.toString(), authenticated.sessionToken)
        interceptor.newAuth(authenticated.sessionToken)
        return true
    }

    fun restoreSession(): String? {
        return preferences.trackPassword(this)
    }

    companion object {
        const val READING_LIST = 0
        const val WISH_LIST = 1
        const val COMPLETE_LIST = 2
        const val UNFINISHED_LIST = 3
        const val ON_HOLD_LIST = 4

        const val DEFAULT_STATUS = READING_LIST
        const val DEFAULT_SCORE = 0f
    }
}
