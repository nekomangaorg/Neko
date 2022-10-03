package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.CACHE_CONTROL_NO_STORE
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.Page
import java.util.concurrent.TimeUnit
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response

abstract class ReducedHttpSource : HttpSource() {

    override val client: OkHttpClient = network.cloudFlareClient.newBuilder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .rateLimit(2)
        .build()

    override val headers = Headers.Builder()
        .add("Referer", "https://manga4life.com/")
        .add(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36",
        )
        .build()

    override suspend fun fetchImage(page: Page): Response {
        val request = imageRequest(page).newBuilder()
            // images will be cached or saved manually, so don't take up network cache
            .cacheControl(CACHE_CONTROL_NO_STORE)
            .build()
        return client.newCallWithProgress(request, page).await()
    }

    override fun isLogged(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun login(username: String, password: String, twoFactorCode: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun logout(): Logout {
        TODO("Not yet implemented")
    }
}
