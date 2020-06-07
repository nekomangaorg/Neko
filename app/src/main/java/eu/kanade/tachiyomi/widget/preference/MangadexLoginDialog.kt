package eu.kanade.tachiyomi.widget.preference

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.View
import br.com.simplepass.loadingbutton.animatedDrawables.ProgressType
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.android.synthetic.main.pref_site_login.view.*
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangadexLoginDialog(bundle: Bundle? = null) : LoginDialogPreference(bundle = bundle) {

    val source: Source by lazy { Injekt.get<SourceManager>().getMangadex() }

    constructor(source: Source, activity: Activity? = null) : this(Bundle().apply {
        putLong(
            "key",
            source.id
        )
    })

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!).apply {
            customView(R.layout.pref_site_login, scrollable = false)
        }

        onViewCreated(dialog.view)

        return dialog
    }

    override fun setCredentialsOnView(view: View) = with(view) {
        dialog_title.text = context.getString(R.string.log_in_to_, source.name)
        username.setText(preferences.sourceUsername(source))
        password.setText(preferences.sourcePassword(source))
    }

    override fun checkLogin() {

        v?.apply {

            login.apply {
                progressType = ProgressType.INDETERMINATE
                startAnimation()
            }

            if (username.text.isNullOrBlank() || password.text.isNullOrBlank() || (two_factor_check.isChecked && two_factor_edit.text.isNullOrBlank())) {
                errorResult()
                context.toast(R.string.fields_cannot_be_blank)
                return
            }

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
                        context.toast(R.string.successfully_logged_in)
                    } else {
                        errorResult()
                    }
                } catch (error: Exception) {
                    errorResult()
                    error.message?.let { context.toast(it) }
                }
            }
        }
    }

    private fun errorResult() {
        v?.apply {
            login.revertAnimation {
                login.text = activity!!.getText(R.string.unknown_error)
            }
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
