package eu.kanade.tachiyomi.ui.main

import android.os.Bundle
import androidx.biometric.BiometricPrompt
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.activity.BaseActivity
import java.util.*
import java.util.concurrent.Executors

class BiometricActivity : BaseActivity() {
    val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt
        .AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                finishAffinity()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                MainActivity.unlocked = true
                preferences.lastUnlock().set(Date().time)
                finish()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                //  TODO("Called when a biometric is valid but not recognized.")
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.unlock_library))
                .setNegativeButtonText(getString(android.R.string.cancel))
                .build()

        biometricPrompt.authenticate(promptInfo)
    }

}