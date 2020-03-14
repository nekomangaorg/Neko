package eu.kanade.tachiyomi.widget.preference

import android.app.Dialog
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.dd.processbutton.iml.ActionProcessButton
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.widget.SimpleTextWatcher
import kotlinx.android.synthetic.main.pref_account_login.view.login
import kotlinx.android.synthetic.main.pref_account_login.view.password
import kotlinx.android.synthetic.main.pref_account_login.view.show_password
import kotlinx.android.synthetic.main.pref_account_login.view.username_label
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import rx.Subscription
import uy.kohesive.injekt.injectLazy

abstract class LoginDialogPreference(private val usernameLabel: String? = null, bundle: Bundle? = null) :
        DialogController(bundle), CoroutineScope {

    var v: View? = null
        private set

    val preferences: PreferencesHelper by injectLazy()

    var requestSubscription: Subscription? = null

    open var canLogout = false

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!).apply {
            customView(R.layout.pref_account_login, scrollable = false)
            positiveButton(android.R.string.cancel)
            if (canLogout) {
                negativeButton(R.string.logout) { logout() }
            }
        }

        onViewCreated(dialog.view)

        return dialog
    }

    open fun logout() { }

    fun onViewCreated(view: View) {
        v = view.apply {
            show_password.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked)
                    password.transformationMethod = null
                else
                    password.transformationMethod = PasswordTransformationMethod()
            }

            if (!usernameLabel.isNullOrEmpty()) {
                username_label.text = usernameLabel
            }

            login.setMode(ActionProcessButton.Mode.ENDLESS)
            login.setOnClickListener { checkLogin() }

            setCredentialsOnView(this)

            show_password.isEnabled = password.text.isNullOrEmpty()

            password.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (s.isEmpty()) {
                        show_password.isEnabled = true
                    }
                }
            })
        }

    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (!type.isEnter) {
            onDialogClosed()
        }
    }

    open fun onDialogClosed() {
        requestSubscription?.unsubscribe()
    }

    protected abstract fun checkLogin()

    protected abstract fun setCredentialsOnView(view: View)

}
