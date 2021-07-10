package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import android.view.View
import br.com.simplepass.loadingbutton.animatedDrawables.ProgressType
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.PrefAccountLoginBinding
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangadexLoginDialog(bundle: Bundle? = null) : LoginDialogPreference(bundle = bundle) {

    val source: Source by lazy { Injekt.get<SourceManager>().getMangadex() }

    constructor(source: Source) : this(
        Bundle().apply {
            putLong(
                "key",
                source.id
            )
        }
    )

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!).apply {
            customView(R.layout.pref_account_login, scrollable = false)
        }
        binding = PrefAccountLoginBinding.bind(dialog.getCustomView())

        onViewCreated(dialog.view)

        return dialog
    }

    override fun setCredentialsOnView(view: View) = with(view) {
        binding.dialogTitle.text = context.getString(R.string.log_in_to_, source.name)
        binding.username.setText(preferences.sourceUsername(source))
        binding.password.setText(preferences.sourcePassword(source))
    }

    override fun checkLogin() {
        v?.apply {
            binding.login.apply {
                progressType = ProgressType.INDETERMINATE
                startAnimation()
            }

            if (binding.username.text.isNullOrBlank() || binding.password.text.isNullOrBlank() || (binding.twoFactorCheck.isChecked && binding.twoFactorEdit.text.isNullOrBlank())) {
                errorResult()
                context.toast(R.string.fields_cannot_be_blank)
                return
            }

            dialog?.setCancelable(false)
            dialog?.setCanceledOnTouchOutside(false)

            scope.launch {
                try {
                    val result = source.login(
                        binding.username.text.toString(),
                        binding.password.text.toString(),
                        binding.twoFactorEdit.text.toString()
                    )
                    if (result) {
                        dialog?.dismiss()
                        launch {
                            preferences.setSourceCredentials(
                                source,
                                binding.username.text.toString(),
                                binding.password.text.toString()
                            )
                        }
                        context.toast(R.string.successfully_logged_in)
                        (targetController as? Listener)?.siteLoginDialogClosed(source)
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

    interface Listener {
        fun siteLoginDialogClosed(source: Source)
    }
}
