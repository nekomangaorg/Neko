package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.system.executeOnIO
import eu.kanade.tachiyomi.util.system.launchIO
import kotlin.getValue
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.site.MangaDexPreferences
import uy.kohesive.injekt.injectLazy

class MangaDexSettingsViewModel : ViewModel() {

    val mangaDexLoginHelper by injectLazy<MangaDexLoginHelper>()

    val mangaDexPreference by injectLazy<MangaDexPreferences>()

    val db: DatabaseHelper by injectLazy()

    private val _state = MutableStateFlow(MangaDexSettingsState())

    val state = _state.asStateFlow()

    init {
        viewModelScope.launchIO {
            mangaDexLoginHelper
                .isLoggedInFlow()
                .onEach { loginCheckResult ->
                    _state.update { it.copy(isLoggedIn = loginCheckResult) }
                }
                .stateIn(viewModelScope)

            mangaDexPreference
                .codeVerifier()
                .changes()
                .distinctUntilChanged()
                .onEach { codeVerifier ->
                    _state.update { it.copy(loginUrl = MdConstants.Login.authUrl(codeVerifier)) }
                }
                .stateIn(viewModelScope)

            mangaDexPreference
                .blockedGroups()
                .changes()
                .distinctUntilChanged()
                .onEach { blockedGroups ->
                    _state.update { it.copy(blockedGroups = blockedGroups.toImmutableSet()) }
                }
                .stateIn(viewModelScope)
        }
    }

    fun deleteAllBrowseFilters() {
        viewModelScope.launchIO { db.deleteAllBrowseFilters().executeOnIO() }
    }

    fun logout() {
        viewModelScope.launchIO { mangaDexLoginHelper.logout() }
    }

    data class MangaDexSettingsState(
        val isLoggedIn: Boolean = false,
        val loginUrl: String = "",
        val blockedGroups: ImmutableSet<String> = persistentSetOf(),
    )
}
