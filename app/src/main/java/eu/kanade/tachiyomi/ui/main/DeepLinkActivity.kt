package eu.kanade.tachiyomi.ui.main

import android.app.Activity
import android.os.Bundle

class DeepLinkActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /* intent.apply {
            flags = flags or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            setClass(applicationContext, SearchActivity::class.java)
            action = MainActivity.INTENT_SEARCH
        }
        startActivity(intent)
        finish()*/
    }
}
