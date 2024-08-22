package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.executeOnIO
import okhttp3.OkHttpClient
import org.nekomanga.domain.track.TrackItem
import uy.kohesive.injekt.injectLazy

sealed class TrackService(val id: Int) {
    val preferences: PreferencesHelper by injectLazy()
    val networkService: NetworkHelper by injectLazy()
    val db: DatabaseHelper by injectLazy()

    open fun canRemoveFromService() = false

    open fun isAutoAddTracker() = false

    open fun isMdList() = false

    open val client: OkHttpClient
        get() = networkService.client

    // Name of the manga sync service to display
    @StringRes abstract fun nameRes(): Int

    // Application and remote support for reading dates
    open val supportsReadingDates: Boolean = false

    @DrawableRes abstract fun getLogo(): Int

    @ColorInt abstract fun getLogoColor(): Int

    abstract fun getScoreList(): List<String>

    open fun indexToScore(index: Int): Float {
        return index.toFloat()
    }

    open fun get10PointScore(score: Float): Float {
        return score
    }

    abstract fun displayScore(track: Track): String

    abstract suspend fun add(track: Track): Track

    abstract suspend fun bind(track: Track): Track

    abstract suspend fun search(
        query: String,
        manga: Manga,
        wasPreviouslyTracked: Boolean,
    ): List<TrackSearch>

    abstract suspend fun refresh(track: Track): Track

    abstract suspend fun login(username: String, password: String): Boolean

    open suspend fun removeFromService(track: Track): Boolean = false

    @CallSuper
    open fun logout() {
        preferences.setTrackCredentials(this, "", "")
    }

    open fun isLogged(): Boolean =
        getUsername().get().isNotEmpty() && getPassword().get().isNotEmpty()

    fun getUsername() = preferences.trackUsername(this)!!

    fun getPassword() = preferences.trackPassword(this)!!

    fun saveCredentials(username: String, password: String) {
        preferences.setTrackCredentials(this, username, password)
    }

    fun matchingTrack(track: TrackItem): Boolean {
        return track.trackServiceId == this.id
    }

    suspend fun getStartDate(track: Track): Long {
        if (db.getChapters(track.manga_id).executeOnIO().any { it.read }) {
            val chapters =
                db.getHistoryByMangaId(track.manga_id).executeOnIO().filter { it.last_read > 0 }
            val date = chapters.minOfOrNull { it.last_read } ?: return 0L
            return if (date <= 0L) 0L else date
        }
        return 0L
    }

    suspend fun getCompletedDate(track: Track, allRead: Boolean): Long {
        if (allRead) {
            val chapters = db.getHistoryByMangaId(track.manga_id).executeOnIO()
            val date = chapters.maxOfOrNull { it.last_read } ?: return 0L
            return if (date <= 0L) 0L else date
        }
        return 0L
    }

    suspend fun getLastChapterRead(track: Track): Float {
        val chapters = db.getChapters(track.manga_id).executeOnIO()
        val lastChapterRead = chapters.filter { it.read }.minByOrNull { it.source_order }
        return lastChapterRead?.takeIf { it.isRecognizedNumber }?.chapter_number ?: 0f
    }
}

abstract class TrackListService(_id: Int) : TrackService(_id) {
    abstract suspend fun populateLists()

    abstract fun viewLists(): List<TrackList>

    abstract suspend fun addToList(track: Track, listId: String): Track

    abstract suspend fun removeFromList(track: Track, listId: String): Track

    abstract suspend fun addToLists(track: Track, listIds: List<String>): Track

    abstract suspend fun removeFromLists(track: Track, listIds: List<String>): Track
}

abstract class TrackStatusService(_id: Int) : TrackService(_id) {
    abstract fun getStatusList(): List<Int>

    abstract fun isCompletedStatus(index: Int): Boolean

    abstract fun completedStatus(): Int

    abstract fun readingStatus(): Int

    abstract fun planningStatus(): Int

    abstract fun getStatus(status: Int): String

    abstract fun getGlobalStatus(status: Int): String

    abstract suspend fun update(track: Track, setToRead: Boolean = false): Track

    open fun updateTrackStatus(
        track: Track,
        setToReadStatus: Boolean,
        setToComplete: Boolean = false,
        mustReadToComplete: Boolean = false,
    ) {
        if (setToReadStatus && track.status == planningStatus() && track.last_chapter_read != 0f) {
            track.status = readingStatus()
        }
        if (setToComplete &&
            (!mustReadToComplete || track.status == readingStatus()) &&
            track.total_chapters != 0 &&
            track.last_chapter_read.toInt() == track.total_chapters) {
            track.status = completedStatus()
        }
    }

    suspend fun updateNewTrackInfo(track: Track, planningStatus: Int) {
        val manga = db.getManga(track.manga_id).executeOnIO()
        val allRead =
            manga?.isOneShotOrCompleted(db) == true &&
                db.getChapters(track.manga_id).executeOnIO().all { it.read }
        if (supportsReadingDates) {
            track.started_reading_date = getStartDate(track)
            track.finished_reading_date = getCompletedDate(track, allRead)
        }
        track.last_chapter_read = getLastChapterRead(track).takeUnless { it == 0f && allRead } ?: 1f
        if (track.last_chapter_read == 0f) {
            track.status = planningStatus
        }
        if (allRead) {
            track.status = completedStatus()
        }
    }
}

data class TrackList(val name: String, val id: String)
