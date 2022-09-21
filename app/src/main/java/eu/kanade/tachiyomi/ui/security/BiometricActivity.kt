package eu.kanade.tachiyomi.ui.security

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseThemedActivity
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BiometricActivity : BaseThemedActivity() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fromSearch = intent.getBooleanExtra("fromSearch", false)
        AuthenticatorUtil.isAuthenticating = true
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt
            .AuthenticationCallback() {

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    AuthenticatorUtil.isAuthenticating = false
                    if (fromSearch) {
                        finish()
                    } else {
                        finishAffinity()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    SecureActivityDelegate.locked = false
                    AuthenticatorUtil.isAuthenticating = false
                    preferences.lastUnlock().set(Date().time)
                    finish()
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.unlock_library))
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
