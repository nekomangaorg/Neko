package eu.kanade.tachiyomi.widget.preference

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.android.synthetic.main.pref_account_login.view.dialog_title
import kotlinx.android.synthetic.main.pref_account_login.view.login
import kotlinx.android.synthetic.main.pref_account_login.view.password
import kotlinx.android.synthetic.main.pref_account_login.view.username
import kotlinx.android.synthetic.main.pref_site_login.view.*
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangadexLoginDialog(bundle: Bundle? = null) : LoginDialogPreference(bundle = bundle) {

    val source: Source by lazy { Injekt.get<SourceManager>().getMangadex() }

    constructor(source: Source, activity: Activity? = null) : this(Bundle().apply { putLong("key", source.id) })

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!).apply {
            customView(R.layout.pref_site_login, scrollable = false)
            positiveButton(android.R.string.cancel)
            if (canLogout) {
                negativeButton(R.string.logout) { logout() }
            }
        }

        onViewCreated(dialog.view)

        return dialog
    }

    override fun setCredentialsOnView(view: View) = with(view) {
        dialog_title.text = context.getString(R.string.login_title, source.name)
        username.setText(preferences.sourceUsername(source))
        password.setText(preferences.sourcePassword(source))
    }

    override fun checkLogin() {

        v?.apply {
            if (username.text.isEmpty() || password.text.isEmpty())
                return

            login.progress = 1
            source
            scope.launch {
                try {
                    val result = source.login(
                        username.text.toString(),
                        password.text.toString(),
                        two_factor_edit.text.toString()
                    )
                    if (result) {
                        dialog?.dismiss()
                        preferences.setSourceCredentials(
                            source,
                            username.text.toString(),
                            password.text.toString()
                        )
                        context.toast(R.string.login_success)
                    } else {
                        errorResult(this@apply)
                    }
                } catch (error: Exception) {
                    errorResult(this@apply)
                    error.message?.let { context.toast(it) }
                }
            }
        }
    }

    fun errorResult(view: View?) {
        v?.apply {
            login.progress = -1
            login.setText(R.string.unknown_error)
        }
    }

    override fun onDialogClosed() {
        super.onDialogClosed()
        if (activity != null) {
            (activity as? Listener)?.siteLoginDialogClosed(source)
        } else {
        (targetController as? Listener)?.siteLoginDialogClosed(source)
        }
    }

    interface Listener {
        fun siteLoginDialogClosed(source: Source)
    }
}
