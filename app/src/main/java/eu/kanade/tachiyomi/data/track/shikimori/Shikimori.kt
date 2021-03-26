package eu.kanade.tachiyomi.data.track.shikimori

import android.content.Context
import android.graphics.Color
import androidx.annotation.StringRes
import com.google.gson.Gson
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.updateNewTrackInfo
import timber.log.Timber
import uy.kohesive.injekt.injectLazy

class Shikimori(private val context: Context, id: Int) : TrackService(id) {

    @StringRes
    override fun nameRes() = R.string.shikimori

    private val gson: Gson by injectLazy()

    private val interceptor by lazy { ShikimoriInterceptor(this, gson) }

    private val api by lazy { ShikimoriApi(client, interceptor) }

    override fun getLogo() = R.drawable.ic_tracker_shikimori

    override fun getLogoColor() = Color.rgb(40, 40, 40)

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLANNING, REPEATING)
    }

    override fun isCompletedStatus(index: Int) = getStatusList()[index] == COMPLETED

    override fun completedStatus(): Int = MyAnimeList.COMPLETED

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(R.string.reading)
            COMPLETED -> getString(R.string.completed)
            ON_HOLD -> getString(R.string.on_hold)
            DROPPED -> getString(R.string.dropped)
            PLANNING -> getString(R.string.plan_to_read)
            REPEATING -> getString(R.string.rereading)
            else -> ""
        }
    }

    override fun getGlobalStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(R.string.reading)
            PLANNING -> getString(R.string.plan_to_read)
            COMPLETED -> getString(R.string.completed)
            ON_HOLD -> getString(R.string.on_hold)
            DROPPED -> getString(R.string.dropped)
            REPEATING -> getString(R.string.rereading)
            else -> ""
        }
    }

    override fun getScoreList(): List<String> {
        return IntRange(0, 10).map(Int::toString)
    }

    override fun displayScore(track: Track): String {
        return track.score.toInt().toString()
    }

    override suspend fun update(track: Track): Track {
        if (track.total_chapters != 0 && track.last_chapter_read == track.total_chapters) {
            track.status = COMPLETED
        }
        return api.updateLibManga(track, getUsername())
    }

    override suspend fun add(track: Track): Track {
        track.score = DEFAULT_SCORE.toFloat()
        track.status = DEFAULT_STATUS
        updateNewTrackInfo(track)
        return api.addLibManga(track, getUsername())
    }
    override suspend fun bind(track: Track): Track {
        val remoteTrack = api.findLibManga(track, getUsername())

        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.library_id = remoteTrack.library_id
            update(track)
        } else {
            add(track)
        }
    }

    override fun canRemoveFromService(): Boolean = true

    override suspend fun removeFromService(track: Track): Boolean {
        return api.remove(track, getUsername())
    }

    override suspend fun search(query: String) = api.search(query)

    override suspend fun refresh(track: Track): Track {
        val remoteTrack = api.findLibManga(track, getUsername())

        if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.total_chapters = remoteTrack.total_chapters
        }
        return track
    }

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String): Boolean {
        return try {
            val oauth = api.accessToken(code)

            interceptor.newAuth(oauth)
            val user = api.getCurrentUser()
            saveCredentials(user.toString(), oauth.access_token)
            true
        } catch (e: java.lang.Exception) {
            Timber.e(e)
            logout()
            false
        }
    }

    fun saveToken(oauth: OAuth?) {
        val json = gson.toJson(oauth)
        preferences.trackToken(this).set(json)
    }

    fun restoreToken(): OAuth? {
        return try {
            gson.fromJson(preferences.trackToken(this).get(), OAuth::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun logout() {
        super.logout()
        preferences.trackToken(this).delete()
        interceptor.newAuth(null)
    }

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLANNING = 5
        const val REPEATING = 6

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0
    }
}
