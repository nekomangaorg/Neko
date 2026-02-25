package eu.kanade.tachiyomi.ui.more.about

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.ui.main.AppSnackbarManager
import eu.kanade.tachiyomi.util.lang.toTimestampString
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.snackbar.SnackbarState
import uy.kohesive.injekt.injectLazy

class AboutViewModel : ViewModel() {
    private val updateChecker by lazy { AppUpdateChecker() }
    private val preferences: PreferencesHelper by injectLazy()

    private val securityPreferences: SecurityPreferences by injectLazy()

    val appSnackbarManager: AppSnackbarManager by injectLazy()

    private val _aboutScreenState =
        MutableStateFlow(
            AboutScreenState(
                buildTime = getFormattedBuildTime(),
                incognitoMode = securityPreferences.incognitoMode().get(),
            )
        )
    val aboutScreenState: StateFlow<AboutScreenState> = _aboutScreenState.asStateFlow()

    private fun getFormattedBuildTime(): String {
        return runCatching {
                val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                inputDf.timeZone = TimeZone.getTimeZone("UTC")
                inputDf.parse(BuildConfig.BUILD_TIME)!!.toTimestampString(preferences.dateFormat())
            }
            .getOrDefault(BuildConfig.BUILD_TIME)
    }

    fun notOnlineSnackbar() {
        viewModelScope.launch {
            appSnackbarManager.showSnackbar(
                SnackbarState(messageRes = R.string.no_network_connection)
            )
        }
    }

    /** Checks version and shows a user prompt if an update is available. */
    fun checkForUpdate() {
        viewModelScope.launch {
            if (aboutScreenState.value.checkingForUpdates) return@launch

            _aboutScreenState.update { it.copy(checkingForUpdates = true) }

            appSnackbarManager.showSnackbar(
                SnackbarState(messageRes = R.string.checking_for_updates)
            )

            val update =
                runCatching { updateChecker.checkForUpdate(isUserPrompt = true) }
                    .getOrElse { error ->
                        AppUpdateResult.CantCheckForUpdate(error.message ?: "Error")
                    }
            when (update) {
                is AppUpdateResult.CantCheckForUpdate -> {
                    appSnackbarManager.showSnackbar(SnackbarState(message = update.reason))
                }
                is AppUpdateResult.NoNewUpdate -> {
                    appSnackbarManager.showSnackbar(
                        SnackbarState(messageRes = R.string.no_new_updates_available)
                    )
                }
                is AppUpdateResult.NewUpdate -> {
                    _aboutScreenState.update {
                        it.copy(shouldShowUpdateDialog = true, updateResult = update)
                    }
                }
            }
            _aboutScreenState.update { it.copy(checkingForUpdates = false) }
        }
    }

    fun hideUpdateDialog() {
        viewModelScope.launch {
            _aboutScreenState.update { it.copy(shouldShowUpdateDialog = false) }
        }
    }

    private var versionClickCount = 0
    private var lastVersionClickTime = 0L

    fun onVersionClicked() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastVersionClickTime > 500) {
            versionClickCount = 0
        }
        lastVersionClickTime = currentTime
        versionClickCount++

        if (versionClickCount == 7) {
            versionClickCount = 0
            val newValue = !preferences.developerMode().get()
            preferences.developerMode().set(newValue)
            viewModelScope.launch {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes =
                            if (newValue) R.string.developer_mode_enabled
                            else R.string.developer_mode_disabled
                    )
                )
            }
        }

        viewModelScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                appSnackbarManager.showSnackbar(
                    SnackbarState(
                        messageRes = R.string._copied_to_clipboard,
                        fieldRes = R.string.build_information,
                    )
                )
            }
        }
    }
}
