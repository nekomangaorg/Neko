package eu.kanade.tachiyomi.data.similar

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get


interface MangaCacheHttpService {
    companion object {
        fun create(): MangaCacheHttpService {
            // actual builder, which will parse the underlying json file
            val contentType = "application/json".toMediaType()
            val restAdapter = Retrofit.Builder()
                .baseUrl("https://raw.githubusercontent.com")
                .addConverterFactory(Json {}.asConverterFactory(contentType))
                .client(
                    Injekt.get<NetworkHelper>().client
                        .newBuilder()
                        .build()
                )
                .build()
            return restAdapter.create(MangaCacheHttpService::class.java)
        }
    }

    @Streaming
    @GET("/goldbattle/MangadexRecomendations/master/output/mangas_compressed.json.gz")
    fun getSimilarResults(): Call<ResponseBody>

    @Streaming
    @GET("/goldbattle/MangadexRecomendations/master/output/md2external.json.gz")
    fun getCachedManga(): Call<ResponseBody>
}