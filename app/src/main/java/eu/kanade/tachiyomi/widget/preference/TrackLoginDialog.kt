package eu.kanade.tachiyomi.widget.preference

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import br.com.simplepass.loadingbutton.animatedDrawables.ProgressType
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class TrackLoginDialog(@StringRes usernameLabelRes: Int? = null, bundle: Bundle? = null) :
    LoginDialogPreference(usernameLabelRes, bundle) {

    private val service = Injekt.get<TrackManager>().getService(args.getInt("key"))!!

    override var canLogout = true

    constructor(service: TrackService, @StringRes usernameLabelRes: Int?) :
        this(usernameLabelRes, Bundle().apply { putInt("key", service.id) })

    override fun setCredentialsOnView(view: View) = with(view) {
        val serviceName = context.getString(service.nameRes())
        binding.dialogTitle.text = context.getString(R.string.log_in_to_, serviceName)
        binding.username.setText(service.getUsername())
        binding.password.setText(service.getPassword())
    }

    override fun checkLogin() {
        v?.apply {
            binding.login.apply {
                progressType = ProgressType.INDETERMINATE
                startAnimation()
            }
            if (binding.username.text.isNullOrBlank() || binding.password.text.isNullOrBlank()) {
                errorResult()
                context.toast(R.string.username_must_not_be_blank)
                return
            }

            dialog?.setCancelable(false)
            dialog?.setCanceledOnTouchOutside(false)
            val user = binding.username.text.toString()
            val pass = binding.password.text.toString()
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
            dialog?.setCancelable(true)
            dialog?.setCanceledOnTouchOutside(true)
            binding.login.revertAnimation {
                binding.login.text = activity!!.getText(R.string.unknown_error)
            }
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
