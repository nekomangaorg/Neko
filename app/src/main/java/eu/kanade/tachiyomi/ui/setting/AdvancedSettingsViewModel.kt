package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchNonCancellable
import eu.kanade.tachiyomi.util.system.launchUI
import kotlin.getValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.nekomanga.R
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.presentation.components.UiText
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

// This class just holds some injects.  If a settings screen requires
class AdvancedSettingsViewModel : ViewModel() {
    val preferences: PreferencesHelper by injectLazy()
    val networkPreference: NetworkPreferences by injectLazy()
    val networkHelper: NetworkHelper by injectLazy()

    val downloadManager: DownloadManager by injectLazy()

    private val _toastEvent = MutableSharedFlow<UiText.StringResource>()
    val toastEvent = _toastEvent.asSharedFlow()

    fun clearNetworkCookies() {
        viewModelScope.launchUI {
            networkHelper.cookieManager.removeAll()
            _toastEvent.emit(UiText.StringResource(R.string.cookies_cleared))
        }
    }

    fun reindexDownloads() {
        viewModelScope.launchNonCancellable {
            launchIO {
                _toastEvent.emit(UiText.StringResource(R.string.reindex_downloads_invalidate))
                downloadManager.refreshCache()
                _toastEvent.emit(UiText.StringResource(R.string.reindex_downloads_complete))
            }
        }
    }
}
