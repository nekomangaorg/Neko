package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.android.synthetic.main.pref_account_login.view.*
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackLoginDialog(usernameLabel: String? = null, bundle: Bundle? = null) :
    LoginDialogPreference(usernameLabel, bundle) {

    private val service = Injekt.get<TrackManager>().getService(args.getInt("key"))!!

    override var canLogout = true

    constructor(service: TrackService) : this(service, null)

    constructor(service: TrackService, usernameLabel: String?) :
        this(usernameLabel, Bundle().apply { putInt("key", service.id) })

    override fun onCreateDialog(savedState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!)
                .customView(viewRes = R.layout.pref_tracker_login, scrollable = true)
                .negativeButton(android.R.string.cancel)

        onViewCreated(dialog.view)

        return dialog
    }

    override fun setCredentialsOnView(view: View) = with(view) {
        dialog_title.text = context.getString(R.string.log_in_to_, service.name)
        username.setText(service.getUsername())
        password.setText(service.getPassword())
    }

    override fun checkLogin() {

        v?.apply {
            if (username.text.isEmpty() || password.text.isEmpty())
                return

            login.progress = 1
            val user = username.text.toString()
            val pass = password.text.toString()

            scope.launch {
                try {
                    val result = service.login(user, pass)
                    if (result) {
                        dialog?.dismiss()
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
            login.progress = -1
            login.setText(R.string.unknown_error)
        }
    }

    override fun logout() {
        if (service.isLogged) {
            service.logout()
            activity?.toast(R.string.successfully_logged_out)
        }
    }

    override fun onDialogClosed() {
        super.onDialogClosed()
        (targetController as? Listener)?.trackLoginDialogClosed(service)
    }

    interface Listener {
        fun trackLoginDialogClosed(service: TrackService)
    }
}
