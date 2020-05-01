package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.launchNow
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MangadexLogoutDialog(bundle: Bundle? = null) : DialogController(bundle) {

    val source: Source by lazy { Injekt.get<SourceManager>().getMangadex() }

    val preferences: PreferencesHelper by injectLazy()

    constructor(source: Source) : this(Bundle().apply { putLong("key", source.id) })

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
            .title(R.string.logout)
            .positiveButton(R.string.logout) {
                launchNow {

                    val loggedOut = withContext(Dispatchers.IO) { source.logout() }

                    if (loggedOut) {
                        preferences.setSourceCredentials(source, "", "")
                        activity?.toast(R.string.successfully_logged_out)
                        (targetController as? Listener)?.siteLogoutDialogClosed(source)
                    } else {
                        activity?.toast(R.string.unknown_error)
                    }
                }
            }
            .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun siteLogoutDialogClosed(source: Source)
    }
}
