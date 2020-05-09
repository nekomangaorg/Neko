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
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.widget.SimpleTextWatcher
import kotlinx.android.synthetic.main.pref_account_login.view.*
import kotlinx.android.synthetic.main.pref_account_login.view.login
import kotlinx.android.synthetic.main.pref_account_login.view.password
import kotlinx.android.synthetic.main.pref_account_login.view.show_password
import kotlinx.android.synthetic.main.pref_account_login.view.username
import kotlinx.android.synthetic.main.pref_account_login.view.username_label
import kotlinx.android.synthetic.main.pref_site_login.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import rx.Subscription
import uy.kohesive.injekt.injectLazy

abstract class LoginDialogPreference(
    private val usernameLabel: String? = null,
    bundle: Bundle? = null
) :
    DialogController(bundle) {

    var v: View? = null
        private set

    val preferences: PreferencesHelper by injectLazy()

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    var requestSubscription: Subscription? = null

    open var canLogout = false

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!).apply {
            customView(R.layout.pref_account_login, scrollable = false)
            positiveButton(android.R.string.cancel)
        }

        onViewCreated(dialog.view)

        return dialog
    }

    open fun logout() {}

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

            two_factor_check?.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    two_factor_edit.visibility = View.VISIBLE
                    two_factor_static.visibility = View.VISIBLE
                } else {
                    two_factor_edit.visibility = View.GONE
                    two_factor_static.visibility = View.GONE
                }
            }

            setCredentialsOnView(this)

            if (canLogout && !username.text.isNullOrEmpty()) {
                logout.visible()
                logout.setOnClickListener { logout() }
            }

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
        scope.cancel()
        requestSubscription?.unsubscribe()
    }

    protected abstract fun checkLogin()

    protected abstract fun setCredentialsOnView(view: View)
}
