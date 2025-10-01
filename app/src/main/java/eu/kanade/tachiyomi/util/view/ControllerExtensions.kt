package eu.kanade.tachiyomi.util.view

import android.content.Intent
import androidx.annotation.MainThread
import androidx.core.net.toUri
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import eu.kanade.tachiyomi.ui.base.controller.OneWayFadeChangeHandler
import eu.kanade.tachiyomi.util.system.toast

fun Controller.withFadeTransaction(): RouterTransaction {
    return RouterTransaction.with(this)
        .pushChangeHandler(OneWayFadeChangeHandler())
        .popChangeHandler(OneWayFadeChangeHandler())
}

fun Controller.withFadeInTransaction(): RouterTransaction {
    return RouterTransaction.with(this)
        .pushChangeHandler(OneWayFadeChangeHandler().apply { fadeOut = false })
        .popChangeHandler(OneWayFadeChangeHandler())
}

fun Controller.openInBrowser(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    } catch (e: Throwable) {
        activity?.toast(e.message)
    }
}

@MainThread
fun Router.canStillGoBack(): Boolean {
    return backstack.size > 1
}
