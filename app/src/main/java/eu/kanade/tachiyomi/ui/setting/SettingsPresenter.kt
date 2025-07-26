package eu.kanade.tachiyomi.ui.setting

import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.storage.StoragePreferences
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsPresenter() : BaseCoroutinePresenter<SettingsController>() {
    val preferencesHelper by injectLazy<PreferencesHelper>()
    val mangaDetailsPreferences by injectLazy<MangaDetailsPreferences>()
    val libraryPreferences by injectLazy<LibraryPreferences>()
    val storagePreferences by injectLazy<StoragePreferences>()
    val db by injectLazy<DatabaseHelper>()
}
