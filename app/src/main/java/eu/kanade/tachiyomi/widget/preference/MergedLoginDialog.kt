package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MergedServerSource
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import org.nekomanga.R
import org.nekomanga.databinding.PrefAccountLoginBinding
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MergedLoginDialog(
    bundle: Bundle? = null,
    val source: MergedServerSource = Injekt.get<SourceManager>().komga,
) : LoginDialogPreference(bundle = bundle, showUrl = true) {

    constructor(
        source: MergedServerSource
    ) : this(Bundle().apply { putLong("key", source.id) }, source)

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = PrefAccountLoginBinding.inflate(activity!!.layoutInflater)
        val dialog = activity!!.materialAlertDialog().apply { setView(binding.root) }
        onViewCreated(binding.root)

        return dialog.create()
    }

    override fun setCredentialsOnView(view: View) =
        with(view) {
            binding.dialogTitle.text = context.getString(R.string.sign_in_to_, source.name)
            binding.username.setText(preferences.sourceUsername(source).get())
            binding.password.setText(preferences.sourcePassword(source).get())
            binding.url.setText(preferences.sourceUrl(source).get())
        }

    override fun checkLogin() {
        v?.apply {
            binding.progress.visibility = View.VISIBLE
            binding.login.visibility = View.GONE
            val isMissingCredentials =
                source.requiresCredentials() &&
                    (binding.username.text.isNullOrBlank() || binding.password.text.isNullOrBlank())
            if (isMissingCredentials || binding.url.text.isNullOrBlank()) {
                errorResult()
                context.toast(R.string.fields_cannot_be_blank)
                return
            }

            dialog?.setCancelable(false)
            dialog?.setCanceledOnTouchOutside(false)

            scope.launch {
                try {
                    val username = binding.username.text.toString()
                    val password = binding.password.text.toString()
                    val url = binding.url.text.toString()
                    val result = source.loginWithUrl(username, password, url)
                    if (result) {
                        dialog?.dismiss()
                        preferences.setSourceCredentials(source, username, password, url)
                        context.toast(R.string.successfully_signed_in)
                        (targetController as? Listener)?.siteLoginDialogClosed(
                            source,
                            binding.url.text.toString(),
                        )
                    } else {
                        errorResult()
                    }
                } catch (error: Exception) {
                    TimberKt.e(error) { "error logging in" }
                    errorResult()
                    error.message?.let { context.toast(it, Toast.LENGTH_LONG) }
                }
            }
        }
    }

    private fun errorResult() {
        v?.apply {
            dialog?.setCancelable(true)
            dialog?.setCanceledOnTouchOutside(true)
            binding.progress.visibility = View.GONE
            binding.login.visibility = View.VISIBLE
            scope.launch { context.toast(R.string.could_not_sign_in) }
        }
    }

    interface Listener {
        fun siteLoginDialogClosed(source: Source, url: String)
    }
}
