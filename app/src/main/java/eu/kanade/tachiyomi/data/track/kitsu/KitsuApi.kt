package eu.kanade.tachiyomi.data.track.kitsu

import com.elvishew.xlog.XLog
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

class KitsuApi(private val client: OkHttpClient, interceptor: KitsuInterceptor) {

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val jsonBuilder = json.asConverterFactory("application/json".toMediaType())

    private val rest = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(authClient)
        .addConverterFactory(jsonBuilder)
        .build()
        .create(Rest::class.java)

    private val searchRest = Retrofit.Builder()
        .baseUrl(algoliaKeyUrl)
        .client(authClient)
        .addConverterFactory(jsonBuilder)
        .build()
        .create(SearchKeyRest::class.java)

    private val algoliaRest = Retrofit.Builder()
        .baseUrl(algoliaUrl)
        .client(client)
        .addConverterFactory(jsonBuilder)
        .build()
        .create(AgoliaSearchRest::class.java)

    suspend fun addLibManga(track: Track, userId: String): Track {
        val data = buildJsonObject {
            putJsonObject("data") {
                put("type", "libraryEntries")
                putJsonObject("attributes") {
                    put("status", track.toKitsuStatus())
                    put("progress", track.last_chapter_read.toInt())
                }
                putJsonObject("relationships") {
                    putJsonObject("user") {
                        putJsonObject("data") {
                            put("id", userId)
                            put("type", "users")
                        }
                    }
                    putJsonObject("media") {
                        putJsonObject("data") {
                            put("id", track.media_id)
                            put("type", "manga")
                        }
                    }
                }
            }
        }

        val result = rest.addLibManga(data)

        track.media_id = result["data"]!!.jsonObject["id"]!!.jsonPrimitive.long
        return track
    }

    suspend fun updateLibManga(track: Track): Track {
        // @formatter:off
        val data = buildJsonObject {
            putJsonObject("data") {
                put("type", "libraryEntries")
                put("id", track.media_id)
                putJsonObject("attributes") {
                    put("status", track.toKitsuStatus())
                    val chapterCount = listOfNotNull(
                        track.total_chapters.takeIf { it > 0 },
                        track.last_chapter_read.toInt(),
                    )
                    put("progress", chapterCount.minOrNull())
                    put("ratingTwenty", track.toKitsuScore())
                    put("startedAt", KitsuDateHelper.convert(track.started_reading_date))
                    put("finishedAt", KitsuDateHelper.convert(track.finished_reading_date))
                }
            }
        }
        // @formatter:on

        rest.updateLibManga(track.media_id, data).let {
            val manga = it["data"]?.jsonObject
            if (manga != null) {
                val startedAt =
                    manga["attributes"]!!.jsonObject["startedAt"]?.jsonPrimitive?.contentOrNull
                val finishedAt =
                    manga["attributes"]!!.jsonObject["finishedAt"]?.jsonPrimitive?.contentOrNull
                val startedDate = KitsuDateHelper.parse(startedAt)
                if (track.started_reading_date <= 0L || startedDate > 0) {
                    track.started_reading_date = startedDate
                }
                val finishedDate = KitsuDateHelper.parse(finishedAt)
                if (track.finished_reading_date <= 0L || finishedDate > 0) {
                    track.finished_reading_date = finishedDate
                }
            }
        }
        return track
    }

    suspend fun remove(track: Track): Boolean {
        try {
            rest.deleteLibManga(track.media_id)
            return true
        } catch (e: Exception) {
            XLog.w(e)
        }
        return false
    }

    suspend fun search(
        query: String,
        manga: Manga,
        wasPreviouslyTracked: Boolean,
    ): List<TrackSearch> {
        if (!wasPreviouslyTracked && !manga.kitsu_id.isNullOrBlank()) {
            authClient.newCall(eu.kanade.tachiyomi.network.GET(apiMangaUrl(manga.kitsu_id!!)))
                .await().parseAs<JsonObject>().let {
                    val id = it["data"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive
                    val map =
                        it["data"]!!.jsonArray[0].jsonObject["attributes"]!!.jsonObject.toMutableMap()
                    map["id"] = id
                    return listOf(KitsuSearchManga(JsonObject(map), true).toTrack())
                }
        } else {
            searchRest.getKey().let {
                val key = it["media"]!!.jsonObject["key"]!!.jsonPrimitive.content
                return algoliaSearch(key, query)
            }
        }
    }

    private suspend fun algoliaSearch(key: String, query: String): List<TrackSearch> {
        val jsonObject = buildJsonObject {
            put("params", "query=$query$algoliaFilter")
        }
        return algoliaRest.getSearchQuery(algoliaAppId, key, jsonObject).let {
            it["hits"]!!.jsonArray
                .map { KitsuSearchManga(it.jsonObject) }
                .filter { it.subType != "novel" }
                .map { it.toTrack() }
        }
    }

    suspend fun findLibManga(track: Track, userId: String): Track? {
        return rest.findLibManga(track.media_id, userId).let {
            val data = it["data"]!!.jsonArray
            if (data.size > 0) {
                val manga = it["included"]!!.jsonArray[0].jsonObject
                KitsuLibManga(data[0].jsonObject, manga).toTrack()
            } else {
                null
            }
        }
    }

    suspend fun getLibManga(track: Track): Track {
        return rest.getLibManga(track.media_id).let {
            val data = it["data"]!!.jsonArray
            if (data.size > 0) {
                val manga = it["included"]!!.jsonArray[0].jsonObject
                KitsuLibManga(data[0].jsonObject, manga).toTrack()
            } else {
                throw Exception("Could not find manga kitsu tracking")
            }
        }
    }

    suspend fun login(username: String, password: String): OAuth {
        return Retrofit.Builder()
            .baseUrl(loginUrl)
            .client(client)
            .addConverterFactory(jsonBuilder)
            .build()
            .create(LoginRest::class.java)
            .requestAccessToken(username, password)
    }

    suspend fun getCurrentUser(): String {
        return rest.getCurrentUser().let {
            it["data"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.content
        }
    }

    private interface Rest {

        @Headers("Content-Type: application/vnd.api+json")
        @POST("library-entries")
        suspend fun addLibManga(
            @Body data: JsonObject,
        ): JsonObject

        @Headers("Content-Type: application/vnd.api+json")
        @DELETE("library-entries/{id}")
        suspend fun deleteLibManga(
            @Path("id") remoteId: Long,
        ): Response<Unit>

        @Headers("Content-Type: application/vnd.api+json")
        @PATCH("library-entries/{id}")
        suspend fun updateLibManga(
            @Path("id") remoteId: Long,
            @Body data: JsonObject,
        ): JsonObject

        @GET("library-entries")
        suspend fun findLibManga(
            @Query("filter[manga_id]", encoded = true) remoteId: Long,
            @Query("filter[user_id]", encoded = true) userId: String,
            @Query("include") includes: String = "manga",
        ): JsonObject

        @GET("library-entries")
        suspend fun getLibManga(
            @Query("filter[id]", encoded = true) remoteId: Long,
            @Query("include") includes: String = "manga",
        ): JsonObject

        @GET("users")
        suspend fun getCurrentUser(
            @Query("filter[self]", encoded = true) self: Boolean = true,
        ): JsonObject
    }

    private interface SearchKeyRest {
        @GET("media/")
        suspend fun getKey(): JsonObject
    }

    private interface AgoliaSearchRest {
        @POST("query/")
        suspend fun getSearchQuery(
            @Header("X-Algolia-Application-Id") appid: String,
            @Header("X-Algolia-API-Key") key: String,
            @Body json: JsonObject,
        ): JsonObject
    }

    private interface LoginRest {

        @FormUrlEncoded
        @POST("oauth/token")
        suspend fun requestAccessToken(
            @Field("username") username: String,
            @Field("password") password: String,
            @Field("grant_type") grantType: String = "password",
            @Field("client_id") client_id: String = clientId,
            @Field("client_secret") client_secret: String = clientSecret,
        ): OAuth
    }

    companion object {
        private const val clientId =
            "dd031b32d2f56c990b1425efe6c42ad847e7fe3ab46bf1299f05ecd856bdb7dd"
        private const val clientSecret =
            "54d7307928f63414defd96399fc31ba847961ceaecef3a5fd93144e960c0e151"
        private const val baseUrl = "https://kitsu.io/api/edge/"
        private const val loginUrl = "https://kitsu.io/api/"
        private const val baseMangaUrl = "https://kitsu.io/manga/"
        private const val algoliaKeyUrl = "https://kitsu.io/api/edge/algolia-keys/"
        private const val algoliaUrl =
            "https://AWQO5J657S-dsn.algolia.net/1/indexes/production_media/"
        private const val algoliaAppId = "AWQO5J657S"
        private const val algoliaFilter =
            "&facetFilters=%5B%22kind%3Amanga%22%5D&attributesToRetrieve=%5B%22synopsis%22%2C%22canonicalTitle%22%2C%22chapterCount%22%2C%22posterImage%22%2C%22startDate%22%2C%22subtype%22%2C%22endDate%22%2C%20%22id%22%5D"

        fun mangaUrl(remoteId: Long): String {
            return baseMangaUrl + remoteId
        }

        fun apiMangaUrl(remoteSlug: String): String {
            return "${baseUrl}manga?filter[slug]=$remoteSlug"
        }

        fun refreshTokenRequest(token: String) = POST(
            "${loginUrl}oauth/token",
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", token)
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .build(),
        )
    }
}
