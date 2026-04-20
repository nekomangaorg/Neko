package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.nekomanga.R
import org.nekomanga.data.database.repository.CategoryRepository
import org.nekomanga.data.database.repository.MangaRepository
import org.nekomanga.data.database.repository.TrackRepository
import org.nekomanga.presentation.components.UiText
import uy.kohesive.injekt.injectLazy

// This class just holds some injects.  If a settings screen requires
class DebugSettingsViewModel : ViewModel() {
    val preferences: PreferencesHelper by injectLazy()
    val downloadManager: DownloadManager by injectLazy()

    val categoryRepository: CategoryRepository by injectLazy()
    val mangaRepository: MangaRepository by injectLazy()
    val followsHandler: FollowsHandler by injectLazy()
    val trackRepository: TrackRepository by injectLazy()
    val trackManager: TrackManager by injectLazy()
    val statusHandler: StatusHandler by injectLazy()

    private val _toastEvent = MutableSharedFlow<UiText>()
    val toastEvent = _toastEvent.asSharedFlow()

    fun unfollowAllLibraryManga() {
        viewModelScope.launchIO {
            _toastEvent.emit(UiText.StringResource(R.string.started))
            mangaRepository.getLibraryList().forEach {
                followsHandler.updateFollowStatus(it.uuid(), FollowStatus.UNFOLLOWED)
                trackRepository
                    .getTrackByMangaIdAndTrackServiceId(it.id!!, TrackManager.MDLIST)
                    ?.let { _ ->
                        trackRepository.deleteTrackByMangaIdAndTrackServiceId(
                            it.id!!,
                            TrackManager.MDLIST,
                        )
                    }
            }
            _toastEvent.emit(UiText.StringResource(R.string.complete))
        }
    }

    fun removeAllMangaWithStatusOnMangaDex() {
        viewModelScope.launchIO {
            _toastEvent.emit(UiText.StringResource(R.string.started))
            val results = statusHandler.fetchReadingStatusForAllManga()

            results.entries.forEach { entry ->
                if (entry.value != null) {
                    followsHandler.updateFollowStatus(entry.key, FollowStatus.UNFOLLOWED)
                }
            }
            _toastEvent.emit(UiText.StringResource(R.string.complete))
        }
    }

    fun clearAllManga() {
        viewModelScope.launchIO {
            _toastEvent.emit(UiText.StringResource(R.string.started))
            mangaRepository.deleteAllManga()
            _toastEvent.emit(UiText.StringResource(R.string.complete))
        }
    }

    fun clearAllCategories() {
        viewModelScope.launchIO {
            _toastEvent.emit(UiText.StringResource(R.string.started))
            val categories = categoryRepository.getCategories()
            categoryRepository.deleteCategories(categories)
            _toastEvent.emit(UiText.StringResource(R.string.complete))
        }
    }

    fun clearAllTrackers() {
        viewModelScope.launchIO {
            _toastEvent.emit(UiText.StringResource(R.string.started))
            trackRepository.deleteAllTracks()
            _toastEvent.emit(UiText.StringResource(R.string.complete))
        }
    }
}
