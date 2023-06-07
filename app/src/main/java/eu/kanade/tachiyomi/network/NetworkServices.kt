package eu.kanade.tachiyomi.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.skydoves.sandwich.adapters.ApiResponseCallAdapterFactory
import eu.kanade.tachiyomi.network.services.MangaDexAtHomeService
import eu.kanade.tachiyomi.network.services.MangaDexAuthorizedUserService
import eu.kanade.tachiyomi.network.services.MangaDexService
import eu.kanade.tachiyomi.network.services.SimilarService
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import uy.kohesive.injekt.injectLazy

class NetworkServices() {
    private val networkHelper: NetworkHelper by injectLazy()
    val json: Json by injectLazy()

    private val jsonRetrofitClient = Retrofit.Builder().addConverterFactory(
        json.asConverterFactory("application/json".toMediaType()),
    )
        .baseUrl(MdConstants.baseUrl)
        .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .client(networkHelper.client)

    val service: MangaDexService =
        jsonRetrofitClient.baseUrl(MdConstants.Api.baseUrl)
            .client(networkHelper.mangadexClient).build()
            .create(MangaDexService::class.java)

    val atHomeService: MangaDexAtHomeService =
        jsonRetrofitClient.baseUrl(MdConstants.Api.baseUrl)
            .client(networkHelper.atHomeClient).build()
            .create(MangaDexAtHomeService::class.java)

    val authService: MangaDexAuthorizedUserService = jsonRetrofitClient.baseUrl(MdConstants.Api.baseUrl)
        .client(networkHelper.authClient).build()
        .create(MangaDexAuthorizedUserService::class.java)

    val similarService: SimilarService =
        jsonRetrofitClient.client(
            networkHelper.client.newBuilder().connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS).build(),
        )
            .build()
            .create(SimilarService::class.java)
}
