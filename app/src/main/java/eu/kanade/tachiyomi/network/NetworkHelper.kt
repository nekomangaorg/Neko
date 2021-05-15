package eu.kanade.tachiyomi.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.log.XLogLevel
import okhttp3.Cache
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.logging.HttpLoggingInterceptor
import org.isomorphism.util.TokenBuckets
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.net.InetAddress
import java.util.concurrent.TimeUnit

class NetworkHelper(val context: Context) {

    private val preferences: PreferencesHelper by injectLazy()

    private val mangaDexLoginHelper: MangaDexLoginHelper by injectLazy()

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val bucket = TokenBuckets.builder().withCapacity(2)
        .withFixedIntervalRefillStrategy(5, 1, TimeUnit.SECONDS).build()

    private val rateLimitInterceptor = Interceptor {
        bucket.consume()
        it.proceed(it.request())
    }

    private fun buildNonRateLimitedClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cache(Cache(cacheDir, cacheSize))
            .cookieJar(cookieManager)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(ChuckerInterceptor(context))
                }
                if (preferences.enableDoh()) {
                    dns(
                        DnsOverHttps.Builder().client(build())
                            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                            .bootstrapDnsHosts(
                                listOf(
                                    InetAddress.getByName("162.159.36.1"),
                                    InetAddress.getByName("162.159.46.1"),
                                    InetAddress.getByName("1.1.1.1"),
                                    InetAddress.getByName("1.0.0.1"),
                                    InetAddress.getByName("162.159.132.53"),
                                    InetAddress.getByName("2606:4700:4700::1111"),
                                    InetAddress.getByName("2606:4700:4700::1001"),
                                    InetAddress.getByName("2606:4700:4700::0064"),
                                    InetAddress.getByName("2606:4700:4700::6400")
                                )
                            ).build()
                    )
                }
                if (XLogLevel.shouldLog(XLogLevel.EXTREME)) {
                    val logger: HttpLoggingInterceptor.Logger = object : HttpLoggingInterceptor.Logger {
                        override fun log(message: String) {
                            try {
                                Gson().fromJson(message, Any::class.java)
                                XLog.tag("||NEKO-NETWORK-JSON").disableStackTrace().json(message)
                            } catch (ex: Exception) {
                                XLog.tag("||NEKO-NETWORK").disableBorder().disableStackTrace().d(message)
                            }
                        }
                    }
                    addInterceptor(HttpLoggingInterceptor(logger).apply { level = HttpLoggingInterceptor.Level.BODY })
                }
            }.build()
    }

    private fun buildRateLimitedClient(): OkHttpClient {
        return nonRateLimitedClient.newBuilder().addNetworkInterceptor(rateLimitInterceptor).build()
    }

    private fun buildRateLimitedAuthenticatedClient(): OkHttpClient {
        return buildRateLimitedClient().newBuilder().authenticator(TokenAuthenticator(mangaDexLoginHelper)).build()
    }

    fun buildCloudFlareClient(): OkHttpClient {
        return nonRateLimitedClient.newBuilder()
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(CloudflareInterceptor(context))
            .build()
    }

    val nonRateLimitedClient = buildNonRateLimitedClient()

    val cloudFlareClient = buildCloudFlareClient()

    val client = buildRateLimitedClient()

    val authClient = buildRateLimitedAuthenticatedClient()

    val headers = Headers.Builder().apply {
        add("User-Agent", "Neko " + System.getProperty("http.agent"))
        add("Referer", MdUtil.baseUrl)
    }.build()
}
