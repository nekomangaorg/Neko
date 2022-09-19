package eu.kanade.tachiyomi.ui.more.about

import android.content.Context
import android.os.Build
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.lang.toTimestampString
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nekomanga.domain.snackbar.SnackbarState
import uy.kohesive.injekt.injectLazy

class AboutPresenter : BaseCoroutinePresenter<AboutPresenter>() {
    private val updateChecker by lazy { AppUpdateChecker() }
    private val preferences: PreferencesHelper by injectLazy()

    private val _aboutScreenState = MutableStateFlow(AboutScreenState(buildTime = getFormattedBuildTime()))
    val aboutScreenState: StateFlow<AboutScreenState> = _aboutScreenState.asStateFlow()

    private val _snackbarState = MutableSharedFlow<SnackbarState>()
    val snackBarState: SharedFlow<SnackbarState> = _snackbarState.asSharedFlow()

    private fun getFormattedBuildTime(): String {
        return runCatching {
            val inputDf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            inputDf.timeZone = TimeZone.getTimeZone("UTC")
            inputDf.parse(BuildConfig.BUILD_TIME)!!.toTimestampString(preferences.dateFormat())
        }.getOrDefault(BuildConfig.BUILD_TIME)
    }

    /**
     * Checks version and shows a user prompt if an update is available.
     */
    fun checkForUpdate(context: Context) {
        presenterScope.launch {
            if (aboutScreenState.value.checkingForUpdates) return@launch

            _aboutScreenState.update {
                it.copy(checkingForUpdates = true)
            }
            _snackbarState.emit(SnackbarState(messageRes = R.string.checking_for_updates))

            val update = runCatching {
                updateChecker.checkForUpdate(context, true)
            }.getOrElse { error ->
                AppUpdateResult.CantCheckForUpdate(error.message ?: "Error")
            }
            when (update) {
                is AppUpdateResult.CantCheckForUpdate -> {
                    _snackbarState.emit(SnackbarState(message = update.reason))
                }
                is AppUpdateResult.NoNewUpdate -> {
                    _snackbarState.emit(SnackbarState(messageRes = R.string.no_new_updates_available))
                }
                is AppUpdateResult.NewUpdate -> {
                    _aboutScreenState.update {
                        it.copy(shouldShowUpdateDialog = true, updateResult = update)
                    }
                }
            }
            _aboutScreenState.update {
                it.copy(checkingForUpdates = false)
            }
        }
    }

    fun hideUpdateDialog() {
        presenterScope.launch {
            _aboutScreenState.update {
                it.copy(shouldShowUpdateDialog = false)
            }
        }
    }

    fun copyToClipboard() {
        presenterScope.launch {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                _snackbarState.emit(SnackbarState(messageRes = R.string._copied_to_clipboard, fieldRes = R.string.build_information))
            }
        }
    }
}
