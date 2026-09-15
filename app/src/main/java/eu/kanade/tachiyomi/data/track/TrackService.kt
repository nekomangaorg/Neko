package eu.kanade.tachiyomi.data.track

import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import okhttp3.OkHttpClient
import org.nekomanga.data.database.repository.ChapterRepository
import org.nekomanga.data.database.repository.HistoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.domain.track.TrackItem
import uy.kohesive.injekt.injectLazy

abstract class TrackService(val id: Int) {

    val preferences: PreferencesHelper by injectLazy()
    val networkService: NetworkHelper by injectLazy()
    val chapterRepository: ChapterRepository by injectLazy()
    val historyRepository: HistoryRepository by injectLazy()
    val mangaRepository: MangaRepository by injectLazy()
    val trackRepository: TrackRepository by injectLazy()

    open fun canRemoveFromService() = false

    open fun isAutoAddTracker() = false

    open val client: OkHttpClient
        get() = networkService.client

    // Name of the manga sync service to display
    @StringRes abstract fun nameRes(): Int

    // Application and remote support for reading dates
    open val supportsReadingDates: Boolean = false

    @DrawableRes abstract fun getLogo(): Int

    @ColorInt abstract fun getLogoColor(): Int

    abstract fun getStatusList(): List<Int>

    abstract fun isCompletedStatus(index: Int): Boolean

    abstract fun completedStatus(): Int

    abstract fun readingStatus(): Int

    abstract fun planningStatus(): Int

    abstract fun getStatus(status: Int): String

    abstract fun getGlobalStatus(status: Int): String

    abstract fun getScoreList(): List<String>

    open fun indexToScore(index: Int): Float {
        return index.toFloat()
    }

    open fun get10PointScore(score: Float): Float {
        return score
    }

    abstract fun displayScore(track: Track): String

    abstract suspend fun add(track: Track): Track

    abstract suspend fun update(
        track: Track,
        setToRead: Boolean = false,
        manga: Manga? = null,
        chapters: List<Chapter>? = null,
    ): Track

    abstract suspend fun bind(track: Track): Track

    abstract suspend fun search(
        query: String,
        manga: Manga,
        wasPreviouslyTracked: Boolean,
    ): List<TrackSearch>

    abstract suspend fun refresh(track: Track): Track

    abstract suspend fun login(username: String, password: String): Boolean

    open suspend fun removeFromService(track: Track): Boolean = false

    open fun isMdList() = false

    open suspend fun updateTrackStatus(
        track: Track,
        setToReadStatus: Boolean,
        mustReadToComplete: Boolean = false,
        manga: Manga? = null,
        chapters: List<Chapter>? = null,
    ) {
        if (setToReadStatus && track.status == planningStatus() && track.last_chapter_read != 0f) {
            track.status = readingStatus()
        }

        val canComplete = !mustReadToComplete || track.status == readingStatus()

        val mangaModel =
            manga
                ?: if (track.manga_id != 0L) mangaRepository.getMangaById(track.manga_id) else null
        val mangaChapters =
            chapters
                ?: if (track.manga_id != 0L) chapterRepository.getChaptersForManga(track.manga_id)
                else emptyList()

        val hasTotalChaptersMatch =
            track.total_chapters > 0 && track.last_chapter_read >= track.total_chapters.toFloat()

        val isMangaCompleted =
            mangaModel?.isOneShotOrCompleted(chapterRepository, mangaChapters) == true ||
                mangaChapters.any { it.name.contains("[END]", ignoreCase = true) }

        val allChaptersRead = mangaChapters.isNotEmpty() && mangaChapters.all { it.read }

        val reachedLastChapter = run {
            val lastChapterNum =
                listOfNotNull(
                        mangaModel?.last_chapter_number?.toFloat()?.takeIf { it > 0f },
                        mangaChapters
                            .filter { it.isRecognizedNumber }
                            .maxOfOrNull { it.chapter_number }
                            ?.takeIf { it > 0f },
                    )
                    .maxOrNull()
            lastChapterNum != null &&
                lastChapterNum > 0f &&
                track.last_chapter_read >= lastChapterNum
        }

        val endChapterRead = mangaChapters.any {
            it.name.contains("[END]", ignoreCase = true) && it.read
        }

        val isCompleted =
            hasTotalChaptersMatch ||
                (isMangaCompleted && (allChaptersRead || reachedLastChapter || endChapterRead))

        if (canComplete && isCompleted) {
            track.status = completedStatus()
            if (track.total_chapters == 0) {
                val total =
                    mangaModel?.last_chapter_number
                        ?: mangaChapters
                            .filter { it.isRecognizedNumber }
                            .maxOfOrNull { it.chapter_number }
                            ?.toInt()
                        ?: track.last_chapter_read.toInt().takeIf { it > 0 }
                if (total != null && total > 0) {
                    track.total_chapters = total
                }
            }
        }

        if (supportsReadingDates) {
            if (track.status == completedStatus()) {
                val needsFinishDate = track.finished_reading_date <= 0L
                val needsStartDate = track.started_reading_date <= 0L
                if (needsFinishDate || needsStartDate) {
                    val history =
                        if (track.manga_id != 0L)
                            historyRepository.getHistoryByMangaId(track.manga_id)
                        else emptyList()
                    if (needsFinishDate) {
                        val completedDate =
                            getCompletedDate(
                                track,
                                allChaptersRead || isCompleted,
                                history = history,
                            )
                        track.finished_reading_date =
                            if (completedDate > 0L) completedDate else System.currentTimeMillis()
                    }
                    if (needsStartDate) {
                        val startDate =
                            getStartDate(
                                track,
                                hasReadChapters = mangaChapters.any { it.read },
                                history = history,
                            )
                        track.started_reading_date =
                            if (startDate > 0L) startDate else track.finished_reading_date
                    }
                }
            } else if (track.status == readingStatus() && track.started_reading_date <= 0L) {
                val startDate = getStartDate(track, hasReadChapters = mangaChapters.any { it.read })
                track.started_reading_date =
                    if (startDate > 0L) startDate else System.currentTimeMillis()
            }
        }
    }

    @CallSuper
    open fun logout() {
        preferences.setTrackCredentials(this, "", "")
    }

    open fun isLogged(): Boolean =
        getUsername().get().isNotEmpty() && getPassword().get().isNotEmpty()

    open fun isLoggedInFlow(): Flow<Boolean> {
        return combine(getUsername().changes(), getPassword().changes()) { username, password ->
                username.isNotEmpty() && password.isNotEmpty()
            }
            .distinctUntilChanged()
    }

    fun getUsername() = preferences.trackUsername(this)

    fun getPassword() = preferences.trackPassword(this)

    fun saveCredentials(username: String, password: String) {
        preferences.setTrackCredentials(this, username, password)
    }
}

fun TrackService.matchingTrack(track: TrackItem): Boolean {
    return track.trackServiceId == this.id
}

suspend fun TrackService.updateNewTrackInfo(track: Track, planningStatus: Int) {
    val manga = if (track.manga_id != 0L) mangaRepository.getMangaById(track.manga_id) else null
    val chapters =
        if (track.manga_id != 0L) chapterRepository.getChaptersForManga(track.manga_id)
        else emptyList()
    val allRead =
        manga?.isOneShotOrCompleted(chapterRepository, chapters) == true &&
            chapters.isNotEmpty() &&
            chapters.all { it.read }
    if (supportsReadingDates) {
        val history =
            if (track.manga_id != 0L) historyRepository.getHistoryByMangaId(track.manga_id)
            else emptyList()
        track.started_reading_date =
            getStartDate(track, hasReadChapters = chapters.any { it.read }, history = history)
        track.finished_reading_date = getCompletedDate(track, allRead, history = history)
    }
    track.last_chapter_read =
        getLastChapterRead(track, chapters).takeUnless { it == 0f && allRead } ?: 1f
    if (track.last_chapter_read == 0f) {
        track.status = planningStatus
    }
    if (allRead) {
        track.status = completedStatus()
        if (track.total_chapters == 0) {
            val total =
                manga.last_chapter_number
                    ?: chapters
                        .filter { it.isRecognizedNumber }
                        .maxOfOrNull { it.chapter_number }
                        ?.toInt()
                    ?: track.last_chapter_read.toInt().takeIf { it > 0 }
            if (total != null && total > 0) {
                track.total_chapters = total
            }
        }
    }
}

suspend fun TrackService.getStartDate(
    track: Track,
    hasReadChapters: Boolean? = null,
    history: List<History>? = null,
): Long {
    if (track.manga_id != 0L) {
        val hasRead =
            hasReadChapters ?: chapterRepository.getChaptersForManga(track.manga_id).any { it.read }
        if (hasRead) {
            val chapters =
                (history ?: historyRepository.getHistoryByMangaId(track.manga_id)).filter {
                    it.last_read > 0
                }
            val date = chapters.minOfOrNull { it.last_read } ?: return 0L
            return if (date <= 0L) 0L else date
        }
    }
    return 0L
}

suspend fun TrackService.getCompletedDate(
    track: Track,
    allRead: Boolean,
    history: List<History>? = null,
): Long {
    if (allRead && track.manga_id != 0L) {
        val chapters = history ?: historyRepository.getHistoryByMangaId(track.manga_id)
        val date = chapters.filter { it.last_read > 0 }.maxOfOrNull { it.last_read } ?: return 0L
        return if (date <= 0L) 0L else date
    }
    return 0L
}

suspend fun TrackService.getLastChapterRead(
    track: Track,
    chapters: List<Chapter>? = null,
): Float {
    if (track.manga_id == 0L) return 0f
    val mangaChapters = chapters ?: chapterRepository.getChaptersForManga(track.manga_id)
    val lastChapterRead = mangaChapters.filter { it.read }.minByOrNull { it.smart_order }
    return lastChapterRead?.takeIf { it.isRecognizedNumber }?.chapter_number ?: 0f
}
