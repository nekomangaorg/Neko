package eu.kanade.tachiyomi.widget.preference

import android.os.Bundle
import android.view.View
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

    override fun setCredentialsOnView(view: View) = with(view) {
        dialog_title.text = context.getString(R.string.login_title, service.name)
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

    override fun logout() {
        if (service.isLogged) {
            service.logout()
            activity?.toast(R.string.logout_success)
        }
    }

    override fun onDialogClosed() {
        super.onDialogClosed()
        (targetController as? Listener)?.trackDialogClosed(service)
    }

    interface Listener {
        fun trackDialogClosed(service: TrackService)
    }
}
