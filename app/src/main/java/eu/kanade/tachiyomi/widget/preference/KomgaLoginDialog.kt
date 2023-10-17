package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import br.com.simplepass.loadingbutton.animatedDrawables.ProgressType
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PrefAccountLoginBinding
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.merged.komga.Komga
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class KomgaLoginDialog(bundle: Bundle? = null) : LoginDialogPreference(bundle = bundle, showUrl = true) {

    val source: Komga by lazy { Injekt.get<SourceManager>().komga }

    constructor(source: Komga) : this(
        Bundle().apply {
            putLong(
                "key",
                source.id,
            )
        },
    )

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = PrefAccountLoginBinding.inflate(activity!!.layoutInflater)
        val dialog = activity!!.materialAlertDialog().apply {
            setView(binding.root)
        }
        onViewCreated(binding.root)

        return dialog.create()
    }

    override fun setCredentialsOnView(view: View) = with(view) {
        binding.dialogTitle.text = context.getString(R.string.log_in_to_, source.name)
        binding.username.setText(preferences.sourceUsername(source).get())
        binding.password.setText(preferences.sourcePassword(source).get())
        binding.url.setText(preferences.sourceUrl(source).get())
    }

    override fun checkLogin() {
        v?.apply {
            binding.login.apply {
                progressType = ProgressType.INDETERMINATE
                startAnimation()
            }

            if (binding.username.text.isNullOrBlank() || binding.password.text.isNullOrBlank() || binding.url.text.isNullOrBlank()) {
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
                        preferences.setKomgaCredentials(
                            source,
                            username,
                            password,
                            url,
                        )
                        context.toast(R.string.successfully_logged_in)
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
            binding.login.revertAnimation {
                binding.login.text = activity!!.getText(R.string.unknown_error)
            }
        }
    }

    interface Listener {
        fun siteLoginDialogClosed(source: Source, url: String)
    }
}
