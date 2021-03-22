package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.manga.track.SetTrackReadingDatesDialog
import eu.kanade.tachiyomi.util.system.executeOnIO
import okhttp3.OkHttpClient
import uy.kohesive.injekt.injectLazy

abstract class TrackService(val id: Int) {

    val preferences: PreferencesHelper by injectLazy()
    val networkService: NetworkHelper by injectLazy()
    val db: DatabaseHelper by injectLazy()
    open fun canRemoveFromService() = false
    open val client: OkHttpClient
        get() = networkService.client

    // Name of the manga sync service to display
    @StringRes
    abstract fun nameRes(): Int

    // Application and remote support for reading dates
    open val supportsReadingDates: Boolean = false

    @DrawableRes
    abstract fun getLogo(): Int

    abstract fun getLogoColor(): Int

    abstract fun getStatusList(): List<Int>

    abstract fun isCompletedStatus(index: Int): Boolean

    abstract fun completedStatus(): Int

    abstract fun getStatus(status: Int): String

    abstract fun getGlobalStatus(status: Int): String

    abstract fun getScoreList(): List<String>

    open fun indexToScore(index: Int): Float {
        return index.toFloat()
    }

    abstract fun displayScore(track: Track): String

    abstract suspend fun add(track: Track): Track

    abstract suspend fun update(track: Track): Track

    abstract suspend fun bind(track: Track): Track

    abstract suspend fun search(query: String): List<TrackSearch>

    abstract suspend fun refresh(track: Track): Track

    abstract suspend fun login(username: String, password: String): Boolean

    open suspend fun removeFromService(track: Track): Boolean = false

    @CallSuper
    open fun logout() {
        preferences.setTrackCredentials(this, "", "")
    }

    open val isLogged: Boolean
        get() = getUsername().isNotEmpty() &&
            getPassword().isNotEmpty()

    fun getUsername() = preferences.trackUsername(this)!!

    fun getPassword() = preferences.trackPassword(this)!!

    fun saveCredentials(username: String, password: String) {
        preferences.setTrackCredentials(this, username, password)
    }
}

suspend fun TrackService.updateNewTrackInfo(track: Track) {
    val allRead = db.getManga(track.manga_id).executeOnIO()?.status == SManga.COMPLETED &&
        db.getChapters(track.manga_id).executeOnIO().all { it.read }
    if (supportsReadingDates) {
        track.started_reading_date = getStartDate(track)
        track.finished_reading_date = getCompletedDate(track, allRead)
    }
    track.last_chapter_read = getLastChapterRead(track)
    if (allRead) {
        track.status = completedStatus()
    }
}

suspend fun TrackService.getStartDate(track: Track): Long {
    if (db.getChapters(track.manga_id).executeOnIO().any { it.read }) {
        val chapters = db.getHistoryByMangaId(track.manga_id).executeOnIO()
        val date = chapters.minOfOrNull { it.last_read } ?: return 0L
        return if (date <= 0L) 0L else date
    }
    return 0L
}

suspend fun TrackService.getCompletedDate(track: Track, allRead: Boolean): Long {
    if (allRead) {
        val chapters = db.getHistoryByMangaId(track.manga_id).executeOnIO()
        val date = chapters.maxOfOrNull { it.last_read } ?: return 0L
        return if (date <= 0L) 0L else date
    }
    return 0L
}

suspend fun TrackService.getLastChapterRead(track: Track): Int {
    val chapters = db.getChapters(track.manga_id).executeOnIO()
    return chapters.filter { it.read }.minByOrNull { it.source_order }?.chapter_number?.toInt() ?: 0
}
