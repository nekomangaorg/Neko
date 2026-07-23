package eu.kanade.tachiyomi.ui.setting

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nekomanga.core.preferences.observeAndUpdate
import org.nekomanga.constants.MdConstants
import org.nekomanga.data.database.repository.BrowseFilterRepository
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.injectLazy

class MangaDexSettingsViewModel : ViewModel() {

    val mangaDexLoginHelper by injectLazy<MangaDexLoginHelper>()

    val mangaDexPreference by injectLazy<MangaDexPreferences>()
    val browseFilterRepository: BrowseFilterRepository by injectLazy()

    private val _state = MutableStateFlow(MangaDexSettingsState())

    val state = _state.asStateFlow()

    init {
        mangaDexLoginHelper
            .isLoggedInFlow()
            .observeAndUpdate(viewModelScope) { loginCheckResult ->
                _state.update { it.copy(isLoggedIn = loginCheckResult) }
            }

        mangaDexPreference
            .codeVerifier()
            .changes()
            .observeAndUpdate(viewModelScope) { codeVerifier ->
                _state.update { it.copy(loginUrl = MdConstants.Login.authUrl(codeVerifier)) }
            }

        mangaDexPreference
            .blockedGroups()
            .changes()
            .observeAndUpdate(viewModelScope) { blockedGroups ->
                _state.update { it.copy(blockedGroups = blockedGroups.toSet()) }
            }

        mangaDexPreference
            .blockedUploaders()
            .changes()
            .observeAndUpdate(viewModelScope) { blockedUploaders ->
                _state.update { it.copy(blockedUploaders = blockedUploaders.toSet()) }
            }
    }

    fun deleteAllBrowseFilters() {

        viewModelScope.launchIO { browseFilterRepository.deleteAllBrowseFilters() }
    }

    fun logout() {
        viewModelScope.launchIO { mangaDexLoginHelper.logout() }
    }

    @Immutable
    data class MangaDexSettingsState(
        val isLoggedIn: Boolean = false,
        val loginUrl: String = "",
        val blockedGroups: Set<String> = setOf(),
        val blockedUploaders: Set<String> = setOf(),
    )
}
