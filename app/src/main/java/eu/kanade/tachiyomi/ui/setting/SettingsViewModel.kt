package eu.kanade.tachiyomi.ui.setting

import androidx.lifecycle.ViewModel
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlin.getValue
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

// This class just holds some injects.  If a settings screen requires
class SettingsViewModel : ViewModel() {
    val preferences: PreferencesHelper by injectLazy()
    val mangaDetailsPreferences: MangaDetailsPreferences by injectLazy()
    val libraryPreferences: LibraryPreferences by injectLazy()
    val securityPreferences: SecurityPreferences by injectLazy()
    val db: DatabaseHelper by injectLazy()
}
