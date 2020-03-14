package eu.kanade.tachiyomi.data.similar

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface SimilarHttpService {
    companion object {
        fun create(): SimilarHttpService {
            val contentType = "application/json".toMediaType()
            val restAdapter = Retrofit.Builder()
                .baseUrl("https://github.com")
                .addConverterFactory(Json.asConverterFactory(contentType))
                .client(Injekt.get<NetworkHelper>().client)
                .build()

            return restAdapter.create(SimilarHttpService::class.java)
        }
    }

    @GET("/goldbattle/MangadexRecomendations/releases/download/v1.0.0/mangas_compressed.json")
    fun getSimilarResults(): Call<JsonObject>
}
