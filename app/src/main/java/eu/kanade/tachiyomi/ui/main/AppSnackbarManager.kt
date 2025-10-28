package eu.kanade.tachiyomi.ui.main

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.nekomanga.domain.snackbar.SnackbarState

class AppSnackbarManager {
    private val _events = MutableSharedFlow<SnackbarState>(extraBufferCapacity = 5)
    val events = _events.asSharedFlow()

    suspend fun showSnackbar(event: SnackbarState) {
        _events.emit(event)
    }
}
