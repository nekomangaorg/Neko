package eu.kanade.tachiyomi.ui.main

import androidx.lifecycle.ViewModel
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MainActivityViewModel : ViewModel() {
    val appSnackbarManager: AppSnackbarManager = Injekt.get()
}
