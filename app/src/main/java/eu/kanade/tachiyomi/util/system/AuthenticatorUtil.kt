package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.annotation.CallSuper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

object AuthenticatorUtil {

    /**
     * A check to avoid double authentication on older APIs when confirming settings changes since
     * the biometric prompt is launched in a separate activity outside of the app.
     */
    var isAuthenticating = false

    /**
     * Launches biometric prompt.
     *
     * @param title String title that will be shown on the prompt
     * @param subtitle Optional string subtitle that will be shown on the prompt
     * @param confirmationRequired Whether require explicit user confirmation after passive biometric is recognized
     * @param callback Callback object to handle the authentication events
     */
    fun FragmentActivity.startAuthentication(
        title: String,
        subtitle: String? = null,
        callback: AuthenticationCallback,
    ) {
        isAuthenticating = true
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            callback,
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Returns true if Class 2 biometric or credential lock is set and available to use
     */
    fun Context.isAuthenticationSupported(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * [AuthenticationCallback] with extra check
     *
     * @see isAuthenticating
     */
    abstract class AuthenticationCallback : BiometricPrompt.AuthenticationCallback() {
        @CallSuper
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            isAuthenticating = false
        }

        @CallSuper
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            isAuthenticating = false
        }

        @CallSuper
        override fun onAuthenticationFailed() {
            isAuthenticating = false
        }
    }
}
