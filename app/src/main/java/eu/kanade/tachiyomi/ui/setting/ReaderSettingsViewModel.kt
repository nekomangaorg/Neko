package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import kotlin.getValue
import org.nekomanga.domain.reader.ReaderPreferences
import uy.kohesive.injekt.injectLazy

class ReaderSettingsViewModel : ViewModel() {
    val readerPreferences by injectLazy<ReaderPreferences>()
}
