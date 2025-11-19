package eu.kanade.tachiyomi.network

import android.content.Context
import com.google.common.net.HttpHeaders
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.util.lang.isUUID
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.nekomanga.BuildConfig
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.core.network.interceptor.HeadersInterceptor
import org.nekomanga.core.network.interceptor.PriorityRateLimitInterceptor
import org.nekomanga.core.network.interceptor.UserAgentInterceptor
import org.nekomanga.core.network.interceptor.authInterceptor
import org.nekomanga.core.network.interceptor.loggingInterceptor
import org.nekomanga.core.network.interceptor.rateLimit
import org.nekomanga.domain.site.MangaDexPreferences
import tachiyomi.core.network.AndroidCookieJar
import tachiyomi.core.network.PREF_DOH_360
import tachiyomi.core.network.PREF_DOH_ADGUARD
import tachiyomi.core.network.PREF_DOH_ALIDNS
import tachiyomi.core.network.PREF_DOH_CLOUDFLARE
import tachiyomi.core.network.PREF_DOH_CONTROLD
import tachiyomi.core.network.PREF_DOH_DNSPOD
import tachiyomi.core.network.PREF_DOH_GOOGLE
import tachiyomi.core.network.PREF_DOH_MULLVAD
import tachiyomi.core.network.PREF_DOH_NJALLA
import tachiyomi.core.network.PREF_DOH_QUAD101
import tachiyomi.core.network.PREF_DOH_QUAD9
import tachiyomi.core.network.PREF_DOH_SHECAN
import tachiyomi.core.network.doh360
import tachiyomi.core.network.dohAdGuard
import tachiyomi.core.network.dohAliDNS
import tachiyomi.core.network.dohCloudflare
import tachiyomi.core.network.dohControlD
import tachiyomi.core.network.dohDNSPod
import tachiyomi.core.network.dohGoogle
import tachiyomi.core.network.dohMullvad
import tachiyomi.core.network.dohNajalla
import tachiyomi.core.network.dohQuad101
import tachiyomi.core.network.dohQuad9
import tachiyomi.core.network.dohShecan
import tachiyomi.core.network.interceptors.CloudflareInterceptor
import uy.kohesive.injekt.injectLazy

class NetworkHelper(val context: Context) {
    private val networkPreferences: NetworkPreferences by injectLazy()

    private val mangadexPreferences: MangaDexPreferences by injectLazy()
    private val json: Json by injectLazy()
    private val mangaDexLoginHelper: MangaDexLoginHelper by injectLazy()

    val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val sharedPriorityInterceptor =
        PriorityRateLimitInterceptor(
            permits = 5,
            period = 1,
            unit = TimeUnit.SECONDS,
            prioritySelector = { url ->
                val string = url.toString()
                val isReaderChapter =
                    url.pathSegments.isNotEmpty() &&
                        url.pathSegments[0] == MdConstants.Api.chapter.substringAfterLast("/") &&
                        string.substringAfterLast("/").isUUID()
                val isHomePageOrSearch =
                    url.pathSegments.size == 1 &&
                        url.pathSegments[0] == MdConstants.Api.manga.substringAfterLast("/")

                when {
                    // High Priority (User Actions)
                    isReaderChapter ||
                        isHomePageOrSearch ||
                        url.pathSegments.contains("chapter") ||
                        url.pathSegments.contains("user") ||
                        url.pathSegments.contains("list") ||
                        url.pathSegments.contains("mangadex-seasonal") -> 10

                    // Medium Priority
                    url.host == "uploads.mangadex.org" -> 5

                    // Low Priority (Background Jobs usually fall here)
                    else -> 1
                }
            },
        )

    private val baseClientBuilder: OkHttpClient.Builder
        get() {
            val builder =
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .callTimeout(1, TimeUnit.MINUTES)
                    .cache(Cache(cacheDir, cacheSize))
                    .cookieJar(cookieManager)
                    .apply {
                        when (networkPreferences.dohProvider().get()) {
                            PREF_DOH_CLOUDFLARE -> dohCloudflare()
                            PREF_DOH_GOOGLE -> dohGoogle()
                            PREF_DOH_ADGUARD -> dohAdGuard()
                            PREF_DOH_QUAD9 -> dohQuad9()
                            PREF_DOH_ALIDNS -> dohAliDNS()
                            PREF_DOH_DNSPOD -> dohDNSPod()
                            PREF_DOH_360 -> doh360()
                            PREF_DOH_QUAD101 -> dohQuad101()
                            PREF_DOH_MULLVAD -> dohMullvad()
                            PREF_DOH_CONTROLD -> dohControlD()
                            PREF_DOH_NJALLA -> dohNajalla()
                            PREF_DOH_SHECAN -> dohShecan()
                        }
                    }
            return builder
        }

    private fun buildPriorityRateLimitedClient(): OkHttpClient {

        val dispatcher =
            Dispatcher().apply {
                maxRequestsPerHost = 20
                maxRequests = 30
            }
        return baseClientBuilder
            .dispatcher(dispatcher) // Apply the wider dispatcher
            .addInterceptor(sharedPriorityInterceptor) // Use the SINGLE shared instance
            .addInterceptor(loggingInterceptor({ networkPreferences.verboseLogging().get() }, json))
            .build()
    }

    private fun buildAtHomeRateLimitedClient(): OkHttpClient {
        return baseClientBuilder
            .rateLimit(permits = 40, period = 1, unit = TimeUnit.MINUTES)
            .addInterceptor(HeadersInterceptor(MdConstants.baseUrl))
            .addInterceptor(loggingInterceptor({ networkPreferences.verboseLogging().get() }, json))
            .build()
    }

    private fun buildCdnRateLimitedClient(): OkHttpClient {
        return baseClientBuilder
            .rateLimit(20)
            .addInterceptor(HeadersInterceptor(MdConstants.baseUrl))
            .addInterceptor(loggingInterceptor({ networkPreferences.verboseLogging().get() }, json))
            .build()
    }

    private fun buildRateLimitedAuthenticatedClient(): OkHttpClient {
        return buildPriorityRateLimitedClient()
            .newBuilder()
            .addNetworkInterceptor(authInterceptor { mangadexPreferences.sessionToken().get() })
            .authenticator(MangaDexTokenAuthenticator(mangaDexLoginHelper))
            .addInterceptor(HeadersInterceptor(MdConstants.baseUrl))
            .addInterceptor(loggingInterceptor({ networkPreferences.verboseLogging().get() }, json))
            .build()
    }

    private fun buildCloudFlareClient(): OkHttpClient {
        return baseClientBuilder
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(CloudflareInterceptor(context, cookieManager) { Constants.USER_AGENT })
            .addInterceptor(loggingInterceptor({ networkPreferences.verboseLogging().get() }, json))
            .build()
    }

    val cloudFlareClient = buildCloudFlareClient()

    val client = buildPriorityRateLimitedClient()

    val mangadexClient =
        client
            .newBuilder()
            .addInterceptor(HeadersInterceptor(MdConstants.baseUrl))
            .addInterceptor(loggingInterceptor({ networkPreferences.verboseLogging().get() }, json))
            .build()

    val cdnClient = buildCdnRateLimitedClient()

    val atHomeClient = buildAtHomeRateLimitedClient()

    val authClient = buildRateLimitedAuthenticatedClient()

    val headers =
        Headers.Builder()
            .apply {
                add(
                    HttpHeaders.USER_AGENT,
                    "Neko ${BuildConfig.VERSION_NAME}" + System.getProperty("http.agent"),
                )
                add(HttpHeaders.REFERER, MdConstants.baseUrl)
                add(HttpHeaders.CONTENT_TYPE, "application/json")
            }
            .build()
}
