package eu.kanade.tachiyomi.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.services.MangaDexAuthService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.SimilarService
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.MdApi
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.log.XLogLevel
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.isomorphism.util.TokenBuckets
import retrofit2.Retrofit
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper(val context: Context) {

    private val preferences: PreferencesHelper by injectLazy()

    private val mangaDexLoginHelper: MangaDexLoginHelper by injectLazy()

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val bucket = TokenBuckets.builder().withCapacity(5)
        .withFixedIntervalRefillStrategy(5, 1, TimeUnit.SECONDS).build()

    private val rateLimitInterceptor = Interceptor {
        bucket.consume()
        it.proceed(it.request())
    }

    private fun authInterceptor() = Interceptor { chain ->
        val newRequest = when {
            preferences.sessionToken().isNullOrBlank() -> chain.request()
            else -> {
                val originalRequest = chain.request()
                originalRequest
                    .newBuilder()
                    .header("Authorization", "Bearer ${preferences.sessionToken()}")
                    .method(originalRequest.method, originalRequest.body)
                    .build()
            }
        }
        chain.proceed(newRequest)
    }

    private fun buildNonRateLimitedClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .cache(Cache(cacheDir, cacheSize))
            .cookieJar(cookieManager)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(ChuckerInterceptor(context))
                }
                when (preferences.dohProvider()) {
                    PREF_DOH_CLOUDFLARE -> dohCloudflare()
                    PREF_DOH_GOOGLE -> dohGoogle()
                }
                if (XLogLevel.shouldLog(XLogLevel.EXTREME)) {
                    val logger: HttpLoggingInterceptor.Logger =
                        HttpLoggingInterceptor.Logger { message ->
                            try {
                                Gson().fromJson(message, Any::class.java)
                                XLog.tag("||NEKO-NETWORK-JSON").disableStackTrace()
                                    .json(message)
                            } catch (ex: Exception) {
                                XLog.tag("||NEKO-NETWORK").disableBorder().disableStackTrace()
                                    .d(message)
                            }
                        }
                    addInterceptor(
                        HttpLoggingInterceptor(logger).apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }.build()
    }

    private fun buildRateLimitedClient(): OkHttpClient {
        return nonRateLimitedClient.newBuilder().addNetworkInterceptor(rateLimitInterceptor).build()
    }

    private fun buildRateLimitedAuthenticatedClient(): OkHttpClient {
        return buildRateLimitedClient().newBuilder()
            .addNetworkInterceptor(authInterceptor())
            .authenticator(TokenAuthenticator(mangaDexLoginHelper)).build()
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
        add("X-Requested-With", "XMLHttpRequest")
        add("Content-Type", "application/json")
        add("Referer", MdUtil.baseUrl)
    }.build()

    private val jsonRetrofitClient = Retrofit.Builder().addConverterFactory(
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }.asConverterFactory("application/json".toMediaType())
    )
        .baseUrl(MdConstants.baseUrl)
        .client(client)

    val service: MangaDexService = jsonRetrofitClient.baseUrl(MdApi.baseUrl).client(client).build()
        .create(MangaDexService::class.java)

    val authService = jsonRetrofitClient.baseUrl(MdApi.baseUrl).client(authClient).build()
        .create(MangaDexAuthService::class.java)

    val similarService =
        jsonRetrofitClient.baseUrl("https://api.similarmanga.com/similar/").client(authClient)
            .build()
            .create(SimilarService::class.java)
}
