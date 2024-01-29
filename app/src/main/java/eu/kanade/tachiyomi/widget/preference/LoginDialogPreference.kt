package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.nekomanga.databinding.PrefAccountLoginBinding
import rx.Subscription
import uy.kohesive.injekt.injectLazy

abstract class LoginDialogPreference(
    @StringRes private val usernameLabelRes: Int? = null,
    bundle: Bundle? = null,
    val showUrl: Boolean = false,
) : DialogController(bundle) {

    var v: View? = null
        private set

    protected lateinit var binding: PrefAccountLoginBinding
    val preferences: PreferencesHelper by injectLazy()

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    var requestSubscription: Subscription? = null

    open var canLogout = false

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        binding = PrefAccountLoginBinding.inflate(activity!!.layoutInflater)
        val dialog = activity!!.materialAlertDialog().apply { setView(binding.root) }
        onViewCreated(binding.root)

        return dialog.create()
    }

    fun onViewCreated(view: View) {
        v =
            view.apply {
                if (usernameLabelRes != null) {
                    binding.usernameInput.hint = view.context.getString(usernameLabelRes)
                }

                binding.login.setOnClickListener { checkLogin() }

                if (showUrl) {
                    binding.urlHolder.isVisible = true
                }

                setCredentialsOnView(this)
            }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (!type.isEnter) {
            onDialogClosed()
        }
    }

    open fun onDialogClosed() {
        scope.cancel()
        requestSubscription?.unsubscribe()
    }

    protected abstract fun checkLogin()

    protected abstract fun setCredentialsOnView(view: View)
}
