package eu.kanade.tachiyomi.data.similar

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface SimilarHttpService {
    companion object {
        fun create(): SimilarHttpService {

            // unzip interceptor which will add the correct headers
            val unzipInterceptor = Interceptor {
                val res = it.proceed(it.request())
                res.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .header("Content-Type", "application/json")
                    .build()
            }

            // actual builder, which will parse the underlying json file
            val contentType = "application/json".toMediaType()
            val restAdapter = Retrofit.Builder()
                .baseUrl("https://github.com")
                .addConverterFactory(Json.asConverterFactory(contentType))
                .client(
                    Injekt.get<NetworkHelper>().client
                    .newBuilder()
                    .addNetworkInterceptor(unzipInterceptor)
                    .build()
                )
                .build()
            return restAdapter.create(SimilarHttpService::class.java)
        }
    }

    @GET("/goldbattle/MangadexRecomendations/releases/download/v1.0.0/mangas_compressed.json.gz")
    fun getSimilarResults(): Call<JsonObject>
}
