package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File

class NetworkHelper(context: Context) {

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    val client =
        if(BuildConfig.DEBUG) {
            OkHttpClient.Builder()
                    .cookieJar(cookieManager)
                    .cache(Cache(cacheDir, cacheSize))
                    .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .build()
        }else {
            OkHttpClient.Builder()
                    .cookieJar(cookieManager)
                    .cache(Cache(cacheDir, cacheSize))
                    .build()
        }

        val cloudflareClient = client.newBuilder()
                .addInterceptor(CloudflareInterceptor(context))
                .build()
    }

