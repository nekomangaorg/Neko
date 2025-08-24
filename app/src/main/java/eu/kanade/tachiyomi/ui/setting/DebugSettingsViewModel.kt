package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.uuid
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.online.handlers.FollowsHandler
import eu.kanade.tachiyomi.source.online.handlers.StatusHandler
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import kotlin.getValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.nekomanga.R
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.presentation.components.UiText
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

// This class just holds some injects.  If a settings screen requires
class DebugSettingsViewModel : ViewModel() {
    val preferences: PreferencesHelper by injectLazy()
    val networkPreference: NetworkPreferences by injectLazy()
    val networkHelper: NetworkHelper by injectLazy()

    val downloadManager: DownloadManager by injectLazy()

    val db: DatabaseHelper by injectLazy()
    val followsHandler: FollowsHandler by injectLazy()
    val trackManager: TrackManager by injectLazy()
    val statusHandler: StatusHandler by injectLazy()

    private val _toastEvent = MutableSharedFlow<UiText>()
    val toastEvent = _toastEvent.asSharedFlow()

    fun unfollowAllLibraryManga() {
        viewModelScope.launchIO {
            _toastEvent.emit(UiText.StringResource(R.string.started))
            db.getLibraryMangaList().executeAsBlocking().forEach {
                followsHandler.updateFollowStatus(it.uuid(), FollowStatus.UNFOLLOWED)
                db.getMDList(it).executeOnIO()?.let { _ ->
                    db.deleteTrackForManga(it, trackManager.mdList).executeAsBlocking()
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
            db.deleteAllManga().executeOnIO()
            _toastEvent.emit(UiText.StringResource(R.string.complete))
        }
    }

    fun clearAllCategories() {
        viewModelScope.launchIO {
            _toastEvent.emit(UiText.StringResource(R.string.started))
            val categories = db.getCategories().executeAsBlocking()
            db.deleteCategories(categories).executeOnIO()
            _toastEvent.emit(UiText.StringResource(R.string.complete))
        }
    }

    fun clearAllTrackers() {
        viewModelScope.launchIO {
            _toastEvent.emit(UiText.StringResource(R.string.started))
            db.deleteTracks().executeOnIO()
            _toastEvent.emit(UiText.StringResource(R.string.complete))
        }
    }
}
