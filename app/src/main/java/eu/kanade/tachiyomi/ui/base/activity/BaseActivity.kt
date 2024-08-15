package eu.kanade.tachiyomi.ui.base.activity

import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.setThemeByPref
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.domain.details.MangaDetailsPreferences
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.domain.reader.ReaderPreferences
import uy.kohesive.injekt.injectLazy

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()
    val readerPreferences: ReaderPreferences by injectLazy()
    val securityPreferences: SecurityPreferences by injectLazy()
    val networkPreferences: NetworkPreferences by injectLazy()
    val libraryPreferences: LibraryPreferences by injectLazy()
    val mangaDetailsPreferences: MangaDetailsPreferences by injectLazy()

    lateinit var binding: VB
    val isBindingInitialized
        get() = this::binding.isInitialized

    private var updatedTheme: Resources.Theme? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        updatedTheme = null
        setThemeByPref(preferences)
        super.onCreate(savedInstanceState)
        SecureActivityDelegate.setSecure(this)
    }

    override fun onResume() {
        super.onResume()
        if (this !is SearchActivity) {
            SecureActivityDelegate.promptLockIfNeeded(this)
        }
    }
}
