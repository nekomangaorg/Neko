package eu.kanade.tachiyomi.ui.setting

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.launchIO
import kotlin.getValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nekomanga.core.preferences.observeAndUpdate
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MergeSettingsViewModel : ViewModel() {

    val preferences by injectLazy<PreferencesHelper>()

    private val komga by lazy { Injekt.get<SourceManager>().komga }
    private val suwayomi by lazy { Injekt.get<SourceManager>().suwayomi }

    private val _loginEvent = MutableSharedFlow<MergeLoginEvent>()
    val loginEvent = _loginEvent.asSharedFlow()

    private val _komgaMergeScreenState = MutableStateFlow(MergeScreenState(MergeScreenType.KOMGA))
    val komgaMergeScreenState = _komgaMergeScreenState.asStateFlow()

    private val _suwayomiMergeScreenState =
        MutableStateFlow(MergeScreenState(MergeScreenType.SUWAYOMI))
    val suwayomiMergeScreenState = _suwayomiMergeScreenState.asStateFlow()

    init {
        preferences
            .sourceUrl(komga)
            .changes()
            .observeAndUpdate(viewModelScope) { url ->
                _komgaMergeScreenState.update {
                    it.copy(isLoggedIn = url.isNotBlank(), currentUrl = url)
                }
            }

        preferences
            .sourceUrl(suwayomi)
            .changes()
            .observeAndUpdate(viewModelScope) { url ->
                _suwayomiMergeScreenState.update {
                    it.copy(isLoggedIn = url.isNotBlank(), currentUrl = url)
                }
            }
    }

    fun logout(mergeScreenType: MergeScreenType) {
        val source =
            when (mergeScreenType) {
                MergeScreenType.KOMGA -> komga
                MergeScreenType.SUWAYOMI -> suwayomi
            }
        preferences.setSourceCredentials(source, "", "", "")
    }

    fun login(mergeScreenType: MergeScreenType, username: String, password: String, url: String) {
        viewModelScope.launchIO {
            val source =
                when (mergeScreenType) {
                    MergeScreenType.KOMGA -> komga
                    MergeScreenType.SUWAYOMI -> suwayomi
                }

            val loginSuccessful = source.loginWithUrl(username, password, url)

            when (loginSuccessful) {
                true -> {
                    preferences.setSourceCredentials(source, username, password, url)
                    _loginEvent.emit(MergeLoginEvent.Success)
                }
                false -> {
                    _loginEvent.emit(MergeLoginEvent.Error)
                }
            }
        }
    }
}

enum class MergeScreenType {
    KOMGA,
    SUWAYOMI,
}

sealed class MergeLoginEvent {
    object Success : MergeLoginEvent()

    object Error : MergeLoginEvent()
}

@Immutable
data class MergeScreenState(
    val mergeScreenType: MergeScreenType,
    val isLoggedIn: Boolean = false,
    val currentUrl: String = "",
)
