package eu.kanade.tachiyomi.ui.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
 import uy.kohesive.injekt.injectLazy

abstract class BaseActivity : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()

    init {
    }

}
