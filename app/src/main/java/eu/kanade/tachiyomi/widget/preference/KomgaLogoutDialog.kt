package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.launchNow
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.databinding.PrefAccountLoginBinding
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class KomgaLogoutDialog(bundle: Bundle? = null) : DialogController(bundle) {

    val source: Source by lazy { Injekt.get<SourceManager>().komga }

    protected lateinit var binding: PrefAccountLoginBinding
    val preferences: PreferencesHelper by injectLazy()

    constructor(source: Source) : this(Bundle().apply { putLong("key", source.id) })

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return activity!!
            .materialAlertDialog()
            .apply {
                setTitle(R.string.log_out)
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(R.string.log_out) { _, _ ->
                    launchNow {
                        runCatching {
                                // val loggedOut = source.logout()

                                // if (loggedOut.loggedOut) {
                                launch { preferences.setKomgaCredentials(source, "", "", "") }
                                activity?.toast(R.string.successfully_logged_out)
                                (targetController as? Listener)?.siteLogoutDialogClosed(source)
                                /* } else {
                                    activity?.toast(loggedOut.error)
                                }*/
                            }
                            .onFailure { e ->
                                TimberKt.e(e) { "error logging out" }
                                activity?.toast(R.string.could_not_log_in)
                            }
                    }
                }
            }
            .create()
    }

    interface Listener {
        fun siteLogoutDialogClosed(source: Source)
    }
}
