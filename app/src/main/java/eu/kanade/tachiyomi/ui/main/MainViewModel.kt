package eu.kanade.tachiyomi.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.updater.AppUpdateChecker
import eu.kanade.tachiyomi.data.updater.AppUpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nekomanga.logging.TimberKt

class MainViewModel(
    private val updateChecker: AppUpdateChecker = AppUpdateChecker(),
) : ViewModel() {

    private val _updateResult = MutableStateFlow<AppUpdateResult>(AppUpdateResult.NoNewUpdate)
    val updateResult: StateFlow<AppUpdateResult> = _updateResult

    fun checkForAppUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = updateChecker.checkForUpdate(null)
                withContext(Dispatchers.Main) {
                    _updateResult.value = result
                }
            } catch (error: Exception) {
                TimberKt.e(error) { "Error checking for app update" }
            }
        }
    }
}
