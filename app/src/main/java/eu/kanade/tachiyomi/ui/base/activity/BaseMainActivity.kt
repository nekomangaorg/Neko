package eu.kanade.tachiyomi.ui.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate

open class BaseMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SecureActivityDelegate.setSecure(this)
    }

    override fun onResume() {
        super.onResume()
        SecureActivityDelegate.promptLockIfNeeded(this)
    }

    override fun finish() {
        super.finish()
        SecureActivityDelegate.locked = true
    }
}
