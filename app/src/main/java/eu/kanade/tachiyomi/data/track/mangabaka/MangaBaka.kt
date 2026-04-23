package eu.kanade.tachiyomi.data.track.mangabaka

import android.content.Context
import android.graphics.Color
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.updateNewTrackInfo
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.Json
import org.nekomanga.R
import org.nekomanga.data.network.mangabaka.dto.MangaBakaOAuth
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

class MangaBaka(private val context: Context, id: Int) : TrackService(id) {

    @StringRes override fun nameRes() = R.string.mangabaka

    private val scorePreference = preferences.mangabakaScoreType()

    private val json: Json by injectLazy()

    private val interceptor by lazy { MangaBakaInterceptor(this) }
    private val api by lazy { MangaBakaApi(id, client, interceptor) }

    override val supportsReadingDates: Boolean = true

    override fun getLogo(): Int = R.drawable.ic_tracker_mangabaka

    override fun getLogoColor() = Color.rgb(18, 25, 35)

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, PAUSED, DROPPED, PLAN_TO_READ, REREADING, CONSIDERING)
    }

    override fun isCompletedStatus(index: Int) = getStatusList()[index] == COMPLETED

    override fun completedStatus() = COMPLETED

    override fun readingStatus() = READING

    override fun planningStatus() = PLAN_TO_READ

    override fun getStatus(status: Int): String =
        with(context) {
            when (status) {
                CONSIDERING -> getString(R.string.considering)
                COMPLETED -> getString(R.string.completed)
                DROPPED -> getString(R.string.dropped)
                PAUSED -> getString(R.string.paused)
                PLAN_TO_READ -> getString(R.string.plan_to_read)
                READING -> getString(R.string.reading)
                REREADING -> getString(R.string.repeating)
                else -> ""
            }
        }

    override fun canRemoveFromService() = true

    override fun isAutoAddTracker() = false

    override suspend fun removeFromService(track: Track): Boolean {
        return api.remove(track)
    }

    override fun getGlobalStatus(status: Int): String {
        return with(context) {
            when (status) {
                READING -> getString(R.string.follows_reading)
                PLAN_TO_READ,
                CONSIDERING -> getString(R.string.follows_plan_to_read)
                COMPLETED -> getString(R.string.follows_completed)
                PAUSED -> getString(R.string.follows_on_hold)
                DROPPED -> getString(R.string.follows_dropped)
                else -> ""
            }
        }
    }

    override fun getScoreList(): ImmutableList<String> {
        return when (scorePreference.get()) {
            // 1, 2, ..., 99, 100
            STEP_1 -> IntRange(0, 100).map(Int::toString).toImmutableList()
            // 5, 10, ..., 95, 100
            STEP_5 -> IntRange(0, 100).step(5).map(Int::toString).toImmutableList()
            // 10, 20, ..., 90, 100
            STEP_10 -> IntRange(0, 100).step(10).map(Int::toString).toImmutableList()
            // 20, 40, ..., 80, 100
            STEP_20 -> IntRange(0, 100).step(20).map(Int::toString).toImmutableList()
            // 25, 50, 75, 100
            STEP_25 -> IntRange(0, 100).step(25).map(Int::toString).toImmutableList()
            else -> throw Exception("Unknown score type")
        }
    }

    override fun displayScore(track: Track): String = track.score.toInt().toString()

    override suspend fun add(track: Track): Track {
        updateNewTrackInfo(track, PLAN_TO_READ)
        return api.addLibManga(track)
    }

    override suspend fun update(track: Track, setToRead: Boolean): Track {
        if (track.status != COMPLETED && setToRead) {
            if (
                track.total_chapters > 0 &&
                    track.last_chapter_read.toLong() == track.total_chapters.toLong()
            ) {
                track.status = COMPLETED
                track.finished_reading_date = System.currentTimeMillis()
            } else if (track.status != REREADING) {
                track.status = READING
                if (track.last_chapter_read == 1.0f) {
                    track.started_reading_date = System.currentTimeMillis()
                }
            }
        }

        return api.updateLibManga(track)
    }

    override suspend fun bind(track: Track): Track {
        val remoteTrack = api.findLibManga(track)
        return if (remoteTrack != null) {
            track.copyPersonalFrom(remoteTrack)
            track.title = remoteTrack.title
            track.media_id = remoteTrack.media_id

            if (track.status != COMPLETED) {
                val isRereading = track.status == REREADING
                track.status = if (!isRereading) READING else track.status
            }

            update(track)
        } else {
            // Set default fields if it's not found in the list
            track.status = PLAN_TO_READ
            track.score = 0.0f

            api.addLibManga(track)
        }
    }

    override suspend fun search(
        query: String,
        manga: Manga,
        wasPreviouslyTracked: Boolean,
    ): List<TrackSearch> {
        if (query.startsWith(SEARCH_ID_PREFIX) && !wasPreviouslyTracked) {
            query.substringAfter(SEARCH_ID_PREFIX).toIntOrNull()?.let { id ->
                return api.getMangaDetails(id)?.let { listOf(it) } ?: emptyList()
            }
        }

        return api.search(query)
    }

    override suspend fun refresh(track: Track): Track {
        val remoteTrack = api.findLibManga(track) ?: throw Exception("Could not find manga")
        track.copyPersonalFrom(remoteTrack)
        track.media_id = remoteTrack.media_id
        track.title = remoteTrack.title
        return track
    }

    fun verifyOAuthState(state: String): Boolean = api.verifyOAuthState(state)

    override suspend fun login(username: String, password: String) = login(password)

    suspend fun login(code: String): Boolean {
        try {
            val codeVerifier = preferences.mangabakaCodeVerifier().get()
            val oauth = api.getAccessToken(code, codeVerifier)
            interceptor.setAuth(oauth)
            val userProfile = api.getUserProfile()
            val user = userProfile.preferredUsername ?: userProfile.nickname ?: "username not set"
            saveCredentials(user, oauth.accessToken)

            val scoreType =
                when (val scoreStep = userProfile.ratingSteps) {
                    1 -> STEP_1
                    5 -> STEP_5
                    10 -> STEP_10
                    20 -> STEP_20
                    25 -> STEP_25
                    else -> throw Exception("Unknown score step size $scoreStep")
                }
            scorePreference.set(scoreType)
            return true
        } catch (e: Exception) {
            TimberKt.e(e) { "Login failed" }
            logout()
        }
        return false
    }

    fun saveToken(oauth: MangaBakaOAuth?) {
        preferences.trackToken(this).set(json.encodeToString(oauth))
    }

    fun restoreToken(): MangaBakaOAuth? {
        return try {
            json.decodeFromString(preferences.trackToken(this).get())
        } catch (_: Exception) {
            null
        }
    }

    override fun logout() {
        super.logout()
        preferences.trackToken(this).delete()
        interceptor.setAuth(null)
    }

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val PAUSED = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 5
        const val REREADING = 6
        const val CONSIDERING = 7

        const val STEP_1 = "STEP_1"
        const val STEP_5 = "STEP_5"
        const val STEP_10 = "STEP_10"
        const val STEP_20 = "STEP_20"
        const val STEP_25 = "STEP_25"

        private const val SEARCH_ID_PREFIX = "id:"
    }
}
