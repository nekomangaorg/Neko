package org.nekomanga

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import android.widget.Toast
import androidx.startup.Initializer
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import java.security.Security
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.conscrypt.Conscrypt

class BackgroundInitializer : Initializer<Unit> {
    @OptIn(DelicateCoroutinesApi::class)
    override fun create(context: Context) {
        GlobalScope.launch(Dispatchers.Default) {
            kotlin
                .runCatching { CookieManager.getInstance() }
                .onFailure {
                    GlobalScope.launch(Dispatchers.Main) {
                        Toast.makeText(
                                context,
                                "Error! App requires WebView to be installed",
                                Toast.LENGTH_LONG,
                            )
                            .show()
                    }
                }

            // TLS 1.3 support for Android < 10
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Security.insertProviderAt(Conscrypt.newProvider(), 1)
            }

            MangaCoverMetadata.load()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
