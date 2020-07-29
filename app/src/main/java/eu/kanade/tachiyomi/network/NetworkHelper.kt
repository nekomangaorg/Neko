package eu.kanade.tachiyomi.network

import android.content.Context
import android.os.SystemClock
import android.util.Log.VERBOSE
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper(val context: Context) {

    private val preferencesHelper: PreferencesHelper by injectLazy()

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val requestsPerSecond = 3
    private val lastRequests = ArrayList<Long>(requestsPerSecond)
    private val rateLimitInterceptor = Interceptor {
        synchronized(this) {
            val now = SystemClock.elapsedRealtime()
            val waitTime = if (lastRequests.size < requestsPerSecond) {
                0
            } else {
                val oldestReq = lastRequests[0]
                val newestReq = lastRequests[requestsPerSecond - 1]

                if (newestReq - oldestReq > 1000) {
                    0
                } else {
                    oldestReq + 1000 - now // Remaining time for the next second
                }
            }

            if (lastRequests.size == requestsPerSecond) {
                lastRequests.removeAt(0)
            }
            if (waitTime > 0) {
                lastRequests.add(now + waitTime)
                Thread.sleep(waitTime) // Sleep inside synchronized to pause queued requests
            } else {
                lastRequests.add(now)
            }
        }

        it.proceed(it.request())
    }

    private fun buildNonRateLimitedClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .cache(Cache(cacheDir, cacheSize))
            .addInterceptor(ChuckerInterceptor(context))
            .cookieJar(cookieManager)
        if (BuildConfig.DEBUG || preferencesHelper.debugLogEnabled()) {
            val httpLoggingInterceptor = LoggingInterceptor.Builder().setLevel(Level.BODY).log(VERBOSE).build()
            builder.addInterceptor(httpLoggingInterceptor)
        }
        return builder.build()
    }

    private fun buildRateLimitedClient(): OkHttpClient {
        return nonRateLimitedClient.newBuilder().addNetworkInterceptor(rateLimitInterceptor).build()
    }

    fun rebuildClients() {
        nonRateLimitedClient = buildNonRateLimitedClient()
        client = buildRateLimitedClient()
        cloudFlareClient = buildCloudFlareClient()
    }

    fun buildCloudFlareClient(): OkHttpClient {
        return nonRateLimitedClient.newBuilder()
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(CloudflareInterceptor(context))
            .build()
    }

    var nonRateLimitedClient = buildNonRateLimitedClient()

    var cloudFlareClient = buildCloudFlareClient()

    var client = buildRateLimitedClient()
}
