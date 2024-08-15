package eu.kanade.tachiyomi.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.setThemeByPref
import org.nekomanga.presentation.screens.CrashScreen
import org.nekomanga.presentation.theme.NekoTheme
import uy.kohesive.injekt.injectLazy

class CrashActivity : AppCompatActivity() {

    private val preferences: PreferencesHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setThemeByPref(preferences)

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        setContent {
            NekoTheme {
                CrashScreen(
                    exception = exception,
                    onRestartClick = {
                        finishAffinity()
                        startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                    },
                )
            }
        }
    }
}
