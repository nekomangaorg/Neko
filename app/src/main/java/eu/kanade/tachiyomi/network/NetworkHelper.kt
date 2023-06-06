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
import eu.kanade.tachiyomi.network.services.MangaDexAtHomeService
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.SimilarService
import eu.kanade.tachiyomi.source.online.MangaDexLoginHelper
import eu.kanade.tachiyomi.source.online.utils.MdConstants
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
import org.nekomanga.core.network.NetworkPreferences
import org.nekomanga.core.network.PREF_DOH_360
import org.nekomanga.core.network.PREF_DOH_ADGUARD
import org.nekomanga.core.network.PREF_DOH_ALIDNS
import org.nekomanga.core.network.PREF_DOH_CLOUDFLARE
import org.nekomanga.core.network.PREF_DOH_CONTROLD
import org.nekomanga.core.network.PREF_DOH_DNSPOD
import org.nekomanga.core.network.PREF_DOH_GOOGLE
import org.nekomanga.core.network.PREF_DOH_MULLVAD
import org.nekomanga.core.network.PREF_DOH_NJALLA
import org.nekomanga.core.network.PREF_DOH_QUAD101
import org.nekomanga.core.network.PREF_DOH_QUAD9
import org.nekomanga.core.network.PREF_DOH_SHECAN
import org.nekomanga.core.network.doh360
import org.nekomanga.core.network.dohAdGuard
import org.nekomanga.core.network.dohAliDNS
import org.nekomanga.core.network.dohCloudflare
import org.nekomanga.core.network.dohControlD
import org.nekomanga.core.network.dohDNSPod
import org.nekomanga.core.network.dohGoogle
import org.nekomanga.core.network.dohMullvad
import org.nekomanga.core.network.dohNajalla
import org.nekomanga.core.network.dohQuad101
import org.nekomanga.core.network.dohQuad9
import org.nekomanga.core.network.dohShecan
import retrofit2.Retrofit
import uy.kohesive.injekt.injectLazy

class NetworkHelper(val context: Context) {

    private val preferences: PreferencesHelper by injectLazy()
    private val networkPreferences: NetworkPreferences by injectLazy()

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
            preferences.sessionToken().get().isNullOrBlank() -> chain.request()
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

    private val baseClientBuilder: OkHttpClient.Builder
        get() {
            val builder = OkHttpClient.Builder()
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
            level = when (networkPreferences.verboseLogging().get()) {
                true -> HttpLoggingInterceptor.Level.BODY
                false -> HttpLoggingInterceptor.Level.BASIC
            }
            redactHeader("Authorization")

        }
    }

    private fun buildRateLimitedClient(): OkHttpClient {
        return baseClientBuilder.rateLimit(permits = 300, period = 1, unit = TimeUnit.MINUTES).addInterceptor(loggingInterceptor()).build()
    }

    private fun buildAtHomeRateLimitedClient(): OkHttpClient {
        return baseClientBuilder.rateLimit(permits = 40, period = 1, unit = TimeUnit.MINUTES).addInterceptor(HeadersInterceptor()).addInterceptor(loggingInterceptor()).build()
    }

    private fun buildCdnRateLimitedClient(): OkHttpClient {
        return baseClientBuilder.rateLimit(20).addInterceptor(HeadersInterceptor()).addInterceptor(loggingInterceptor()).build()
    }

    private fun buildRateLimitedAuthenticatedClient(): OkHttpClient {
        return buildRateLimitedClient().newBuilder()
            .addNetworkInterceptor(authInterceptor())
            .authenticator(MangaDexTokenAuthenticator(mangaDexLoginHelper))
            .addInterceptor(HeadersInterceptor())
            .addInterceptor(loggingInterceptor()).build()
    }

    private fun buildCloudFlareClient(): OkHttpClient {
        return baseClientBuilder
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(CloudflareInterceptor(context))
            .addInterceptor(loggingInterceptor())
            .build()
    }

    val cloudFlareClient = buildCloudFlareClient()

    val client = buildRateLimitedClient()

    val mangadexClient = client.newBuilder().addInterceptor(HeadersInterceptor()).addInterceptor(loggingInterceptor()).build()

    val cdnClient = buildCdnRateLimitedClient()

    private val atHomeClient = buildAtHomeRateLimitedClient()

    private val authClient = buildRateLimitedAuthenticatedClient()

    val headers = Headers.Builder().apply {
        add("User-Agent", "Neko ${BuildConfig.VERSION_NAME}" + System.getProperty("http.agent"))
        add("Referer", MdConstants.baseUrl)
        add("Content-Type", "application/json")
    }.build()

    private val jsonRetrofitClient = Retrofit.Builder().addConverterFactory(
        json.asConverterFactory("application/json".toMediaType()),
    )
        .baseUrl(MdConstants.baseUrl)
        .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .client(client)

    val service: MangaDexService =
        jsonRetrofitClient.baseUrl(MdConstants.Api.baseUrl)
            .client(mangadexClient).build()
            .create(MangaDexService::class.java)

    val atHomeService: MangaDexAtHomeService =
        jsonRetrofitClient.baseUrl(MdConstants.Api.baseUrl)
            .client(atHomeClient).build()
            .create(MangaDexAtHomeService::class.java)

    val authService: MangaDexAuthorizedUserService = jsonRetrofitClient.baseUrl(MdConstants.Api.baseUrl)
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
                .header("Referer", MdConstants.baseUrl)
                .header("Content-Type", "application/json")
                .header("x-request-id", "Neko-" + UUID.randomUUID())
                .build()

            return chain.proceed(request)
        }
    }
}
