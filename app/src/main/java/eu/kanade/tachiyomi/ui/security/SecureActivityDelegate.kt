package eu.kanade.tachiyomi.ui.security

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.biometric.BiometricManager
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import java.util.Date
import org.nekomanga.core.security.SecurityPreferences
import uy.kohesive.injekt.injectLazy

object SecureActivityDelegate {

    private val securityPreferences by injectLazy<SecurityPreferences>()

    var locked: Boolean = true

    fun setSecure(activity: Activity?) {
        val incognitoMode = securityPreferences.incognitoMode().get()
        val enabled =
            when (securityPreferences.secureScreen().get()) {
                SecurityPreferences.SecureScreenMode.ALWAYS -> true
                SecurityPreferences.SecureScreenMode.INCOGNITO -> incognitoMode
                else -> false
            }
        activity?.window?.setSecureScreen(enabled)
    }

    private fun Window.setSecureScreen(enabled: Boolean) {
        if (enabled) {
            setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    fun promptLockIfNeeded(activity: Activity?, requireSuccess: Boolean = false) {
        if (activity == null || AuthenticatorUtil.isAuthenticating) return
        val lockApp = securityPreferences.useBiometrics().get()
        if (lockApp &&
            BiometricManager.from(activity)
                .canAuthenticate(
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS) {
            if (isAppLocked()) {
                val intent = Intent(activity, BiometricActivity::class.java)
                intent.putExtra("fromSearch", (activity is SearchActivity) && !requireSuccess)
                activity.startActivity(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                } else {
                    activity.overridePendingTransition(0, 0)
                }
            }
        } else if (lockApp) {
            securityPreferences.useBiometrics().set(false)
        }
    }

    fun shouldBeLocked(): Boolean {
        val lockApp = securityPreferences.useBiometrics().get()
        if (lockApp && isAppLocked()) return true
        return false
    }

    private fun isAppLocked(): Boolean {
        return locked &&
            (securityPreferences.lockAfter().get() <= 0 ||
                Date().time >=
                    securityPreferences.lastUnlock().get() +
                        60 * 1000 * securityPreferences.lockAfter().get())
    }
}
