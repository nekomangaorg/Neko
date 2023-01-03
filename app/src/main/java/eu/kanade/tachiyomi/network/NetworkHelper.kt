package eu.kanade.tachiyomi.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.skydoves.sandwich.adapters.ApiResponseCallAdapterFactory
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.MangaDexCdnService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.SimilarService
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.MdApi
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.loggycat
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import uy.kohesive.injekt.injectLazy

class NetworkHelper(val context: Context) {

    private val preferences: PreferencesHelper by injectLazy()

    private val mangaDexLoginHelper: MangaDexLoginHelper by injectLazy()

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
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
            .callTimeout(1, TimeUnit.MINUTES)
            .cache(Cache(cacheDir, cacheSize))
            .cookieJar(cookieManager)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        ChuckerInterceptor.Builder(context)
                            .collector(ChuckerCollector(context))
                            .maxContentLength(250000L)
                            .redactHeaders(emptySet())
                            .alwaysReadResponseBody(false)
                            .build(),
                    )
                }
                when (preferences.dohProvider()) {
                    PREF_DOH_CLOUDFLARE -> dohCloudflare()
                    PREF_DOH_GOOGLE -> dohGoogle()
                    PREF_DOH_ADGUARD -> dohAdGuard()
                    PREF_DOH_QUAD9 -> dohQuad9()
                }

            }.build()
    }

    private val logger: HttpLoggingInterceptor.Logger =
        HttpLoggingInterceptor.Logger { message ->
            try {
                if (message.contains("grant_type=") || message.contains("access_token\":")) {
                    loggycat(tag = "|") { "Not logging request because it contained sessionToken || refreshToken" }
                } else {
                    val element = json.parseToJsonElement(message)
                    element.loggycat(tag = "|") { json.encodeToString(element) }
                }
            } catch (ex: Exception) {
                loggycat(tag = "|") { message }
            }
        }

    private fun loggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor(logger).apply {
            level = when (preferences.verboseLogging()) {
                true -> HttpLoggingInterceptor.Level.HEADERS
                false -> HttpLoggingInterceptor.Level.HEADERS
            }
            redactHeader("Authorization")

        }
    }

    private fun buildRateLimitedClient(): OkHttpClient {
        return nonRateLimitedClient.newBuilder().rateLimit(permits = 300, period = 1, unit = TimeUnit.MINUTES).build()
    }

    private fun buildCdnRateLimitedClient(): OkHttpClient {
        return nonRateLimitedClient.newBuilder().rateLimit(permits = 40, period = 1, unit = TimeUnit.MINUTES).addInterceptor(HeadersInterceptor()).addInterceptor(loggingInterceptor()).build()
    }

    private fun buildRateLimitedAuthenticatedClient(): OkHttpClient {
        return buildRateLimitedClient().newBuilder()
            .addNetworkInterceptor(authInterceptor())
            .authenticator(MangaDexTokenAuthenticator(mangaDexLoginHelper))
            .addInterceptor(HeadersInterceptor())
            .addInterceptor(loggingInterceptor()).build()
    }

    private fun buildCloudFlareClient(): OkHttpClient {
        return nonRateLimitedClient.newBuilder()
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(CloudflareInterceptor(context))
            .build()
    }

    val nonRateLimitedClient = buildNonRateLimitedClient()

    val cloudFlareClient = buildCloudFlareClient()

    val client = buildRateLimitedClient()

    val mangadexClient = client.newBuilder().addInterceptor(HeadersInterceptor()).addInterceptor(loggingInterceptor()).build()

    private val cdnClient = buildCdnRateLimitedClient()

    private val authClient = buildRateLimitedAuthenticatedClient()

    val headers = Headers.Builder().apply {
        add("User-Agent", "Neko " + System.getProperty("http.agent"))
        add("Referer", MdUtil.baseUrl)
        add("Content-Type", "application/json")
    }.build()

    private val jsonRetrofitClient = Retrofit.Builder().addConverterFactory(
        json.asConverterFactory("application/json".toMediaType()),
    )
        .baseUrl(MdConstants.baseUrl)
        .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .client(client)

    val service: MangaDexService =
        jsonRetrofitClient.baseUrl(MdApi.baseUrl)
            .client(mangadexClient).build()
            .create(MangaDexService::class.java)

    val cdnService: MangaDexCdnService =
        jsonRetrofitClient.baseUrl(MdApi.baseUrl)
            .client(cdnClient).build()
            .create(MangaDexCdnService::class.java)

    val authService: MangaDexAuthorizedUserService = jsonRetrofitClient.baseUrl(MdApi.baseUrl)
        .client(authClient).build()
        .create(MangaDexAuthorizedUserService::class.java)

    val similarService: SimilarService =
        jsonRetrofitClient.client(
            client.newBuilder().connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS).build(),
        )
            .build()
            .create(SimilarService::class.java)

    class HeadersInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .removeHeader("User-Agent")
                .header("User-Agent", "Neko " + System.getProperty("http.agent"))
                .header("Referer", MdUtil.baseUrl)
                .header("Content-Type", "application/json")
                .header("x-request-id", "Neko-" + UUID.randomUUID())
                .build()

            return chain.proceed(request)
        }
    }
}
