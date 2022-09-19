package eu.kanade.tachiyomi.data.track.anilist

import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

class AnilistApi(val client: OkHttpClient, interceptor: AnilistInterceptor) {

    private val authClient = client.newBuilder()
        .addInterceptor(interceptor)
        .rateLimit(permits = 85, period = 1, unit = TimeUnit.MINUTES)
        .build()

    suspend fun addLibManga(track: Track): Track {
        return withIOContext {
            val payload = buildJsonObject {
                put("query", addToLibraryQuery())
                putJsonObject("variables") {
                    put("mangaId", track.media_id)
                    put("progress", track.last_chapter_read.toInt())
                    put("status", track.toAnilistStatus())
                    createDate(track.started_reading_date)?.let { date ->
                        put("startedAt", Json.encodeToJsonElement(date))
                    }
                    createDate(track.finished_reading_date)?.let { date ->
                        put("completedAt", Json.encodeToJsonElement(date))
                    }
                }
            }
            authClient.newCall(POST(apiUrl, body = payload.toString().toRequestBody(jsonMime)))
                .await()
                .parseAs<JsonObject>()
                .let {
                    track.library_id =
                        it["data"]!!.jsonObject["SaveMediaListEntry"]!!.jsonObject["id"]!!.jsonPrimitive.long
                    track
                }
        }
    }

    suspend fun updateLibraryManga(track: Track): Track {
        return withIOContext {
            val payload = buildJsonObject {
                put("query", updateInLibraryQuery())
                putJsonObject("variables") {
                    put("listId", track.library_id)
                    put("progress", track.last_chapter_read.toInt())
                    put("status", track.toAnilistStatus())
                    put("score", track.score.toInt())
                    createDate(track.started_reading_date)?.let { date ->
                        put("startedAt", Json.encodeToJsonElement(date))
                    }
                    createDate(track.finished_reading_date)?.let { date ->
                        put("completedAt", Json.encodeToJsonElement(date))
                    }
                }
            }
            authClient.newCall(POST(apiUrl, body = payload.toString().toRequestBody(jsonMime)))
                .await()
                .parseAs<JsonObject>()
                .let { response ->
                    val media = response["data"]!!.jsonObject["SaveMediaListEntry"]!!.jsonObject
                    val startedDate = parseDate(media, "startedAt")
                    if (track.started_reading_date <= 0L || startedDate > 0) {
                        track.started_reading_date = startedDate
                    }
                    val finishedDate = parseDate(media, "completedAt")
                    if (track.finished_reading_date <= 0L || finishedDate > 0) {
                        track.finished_reading_date = finishedDate
                    }
                    track
                }
        }
    }

    suspend fun search(
        search: String,
        manga: Manga,
        wasPreviouslyTracked: Boolean,
    ): List<TrackSearch> {
        return withIOContext {
            val payload = buildJsonObject {
                put("query", if (manga.anilist_id != null && !wasPreviouslyTracked) findQuery() else searchQuery())
                putJsonObject("variables") {
                    put("query", if (manga.anilist_id != null && !wasPreviouslyTracked) manga.anilist_id else search)
                }
            }
            authClient.newCall(POST(apiUrl, body = payload.toString().toRequestBody(jsonMime)))
                .await()
                .parseAs<JsonObject>()
                .let { response ->
                    val data = response["data"]!!.jsonObject
                    val page = data["Page"]!!.jsonObject
                    val media = page["media"]!!.jsonArray
                    val entries = media.map { jsonToALManga(it.jsonObject) }
                    entries.map { it.toTrack() }
                }
        }
    }

    suspend fun findLibManga(track: Track, userid: Int): Track? {
        return withIOContext {
            val payload = buildJsonObject {
                put("query", findLibraryMangaQuery())
                putJsonObject("variables") {
                    put("id", userid)
                    put("manga_id", track.media_id)
                }
            }
            authClient.newCall(POST(apiUrl, body = payload.toString().toRequestBody(jsonMime)))
                .await()
                .parseAs<JsonObject>()
                .let { response ->
                    val data = response["data"]!!.jsonObject
                    val page = data["Page"]!!.jsonObject
                    val media = page["mediaList"]!!.jsonArray
                    val entries = media.map { jsonToALUserManga(it.jsonObject) }
                    entries.firstOrNull()?.toTrack()
                }
        }
    }

    suspend fun getLibManga(track: Track, userid: Int): Track {
        return findLibManga(track, userid) ?: throw Exception("Could not find manga")
    }

    suspend fun remove(track: Track): Boolean {
        return withIOContext {
            val payload = buildJsonObject {
                put("query", deleteFromLibraryQuery())
                putJsonObject("variables") {
                    put("listId", track.library_id)
                }
            }
            authClient.newCall(POST(apiUrl, body = payload.toString().toRequestBody(jsonMime)))
                .await()
                .isSuccessful
        }
    }

    fun createOAuth(token: String): OAuth {
        val yearToMS = TimeUnit.DAYS.toMillis(365)
        return OAuth(token, "Bearer", System.currentTimeMillis() + yearToMS, yearToMS)
    }

    suspend fun getCurrentUser(): Pair<Int, String> {
        return withIOContext {
            val payload = buildJsonObject {
                put("query", currentUserQuery())
            }
            authClient.newCall(
                POST(
                    apiUrl,
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            )
                .await()
                .parseAs<JsonObject>()
                .let {
                    val data = it["data"]!!.jsonObject
                    val viewer = data["Viewer"]!!.jsonObject
                    Pair(
                        viewer["id"]!!.jsonPrimitive.int,
                        viewer["mediaListOptions"]!!.jsonObject["scoreFormat"]!!.jsonPrimitive.content,
                    )
                }
        }
    }

    private fun jsonToALManga(struct: JsonObject): ALManga {
        return ALManga(
            struct["id"]!!.jsonPrimitive.long,
            struct["title"]!!.jsonObject["userPreferred"]!!.jsonPrimitive.content,
            struct["coverImage"]!!.jsonObject["large"]!!.jsonPrimitive.content,
            struct["description"]!!.jsonPrimitive.contentOrNull,
            struct["format"]!!.jsonPrimitive.content.replace("_", "-"),
            struct["status"]!!.jsonPrimitive.contentOrNull ?: "",
            parseDate(struct, "startDate"),
            struct["chapters"]!!.jsonPrimitive.intOrNull ?: 0,
        )
    }

    private fun jsonToALUserManga(struct: JsonObject): ALUserManga {
        return ALUserManga(
            struct["id"]!!.jsonPrimitive.long,
            struct["status"]!!.jsonPrimitive.content,
            struct["scoreRaw"]!!.jsonPrimitive.int,
            struct["progress"]!!.jsonPrimitive.int,
            parseDate(struct, "startedAt"),
            parseDate(struct, "completedAt"),
            jsonToALManga(struct["media"]!!.jsonObject),
        )
    }

    private fun parseDate(struct: JsonObject, dateKey: String): Long {
        return try {
            val date = Calendar.getInstance()
            val year = struct[dateKey]!!.jsonObject["year"]!!.jsonPrimitive.int
            val month = struct[dateKey]!!.jsonObject["month"]!!.jsonPrimitive.int - 1
            val day = struct[dateKey]!!.jsonObject["day"]!!.jsonPrimitive.int
            date.set(year, month, day)
            date.timeInMillis
        } catch (_: Exception) {
            0L
        }
    }

    private fun createDate(dateValue: Long): AniListDate? {
        if (dateValue == -1L) return AniListDate()
        if (dateValue == 0L) return null
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateValue
        return calendar.toAniList()
    }

    private fun Calendar.toAniList(): AniListDate {
        return AniListDate(get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH))
    }

    @Serializable
    private data class AniListDate(
        val year: Int? = null,
        val month: Int? = null,
        val day: Int? = null,
    )

    companion object {
        private const val clientId = "1984"
        private const val apiUrl = "https://graphql.anilist.co/"
        private const val baseUrl = "https://anilist.co/api/v2/"
        private const val baseMangaUrl = "https://anilist.co/manga/"

        fun mangaUrl(mediaId: Long): String {
            return baseMangaUrl + mediaId
        }

        fun authUrl() = "${baseUrl.toUri()}oauth/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "token")
            .build()!!

        fun addToLibraryQuery() =
            """
            |mutation AddManga(${'$'}mangaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}startedAt: FuzzyDateInput, ${'$'}completedAt: FuzzyDateInput) {
                |SaveMediaListEntry (mediaId: ${'$'}mangaId, progress: ${'$'}progress, status: ${'$'}status, startedAt: ${'$'}startedAt, completedAt: ${'$'}completedAt) { 
                |   id 
                |   status 
                |} 
            |}
            |
            """.trimMargin()

        fun deleteFromLibraryQuery() =
            """
                |mutation DeleteManga(${'$'}listId: Int) {
                |DeleteMediaListEntry (id: ${'$'}listId) {
                    |deleted

                |}
            |}
            """.trimMargin()

        fun updateInLibraryQuery() =
            """
            |mutation UpdateManga(${'$'}listId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus, ${'$'}score: Int, ${'$'}startedAt: FuzzyDateInput, ${'$'}completedAt: FuzzyDateInput) {
                |SaveMediaListEntry (id: ${'$'}listId, progress: ${'$'}progress, status: ${'$'}status, scoreRaw: ${'$'}score, startedAt: ${'$'}startedAt, completedAt: ${'$'}completedAt) {
                    |id
                    |status
                    |progress
                    |startedAt {
                        |year
                        |month
                        |day
                    |}
                    |completedAt {
                        |year
                        |month
                        |day
                    |}
                |}
            |}
            |
            """.trimMargin()

        fun searchQuery() =
            """
            |query Search(${'$'}query: String) {
                |Page (perPage: 50) {
                    |media(search: ${'$'}query, type: MANGA, format_not_in: [NOVEL]) {
                        |id
                        |title {
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |status
                        |chapters
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()

        fun findQuery() =
            """
            |query Media(${'$'}query: Int) {
                |Page (perPage: 50) {
                    |media(id: ${'$'}query, type: MANGA, format_not_in: [NOVEL]) {
                        |id
                        |title {
                            |userPreferred
                        |}
                        |coverImage {
                            |large
                        |}
                        |format
                        |status
                        |chapters
                        |description
                        |startDate {
                            |year
                            |month
                            |day
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()

        fun findLibraryMangaQuery() =
            """
            |query (${'$'}id: Int!, ${'$'}manga_id: Int!) {
                |Page {
                    |mediaList(userId: ${'$'}id, type: MANGA, mediaId: ${'$'}manga_id) {
                        |id
                        |status
                        |scoreRaw: score(format: POINT_100)
                        |progress
                        |startedAt {
                            |year
                            |month
                            |day
                        |}
                        |completedAt {
                            |year
                            |month
                            |day
                        |}
                        |media {
                            |id
                            |title {
                                |userPreferred
                            |}
                            |coverImage {
                                |large
                            |}
                            |format
                            |status
                            |chapters
                            |description
                            |startDate {
                                |year
                                |month
                                |day
                            |}
                        |}
                    |}
                |}
            |}
            |
            """.trimMargin()

        fun currentUserQuery() =
            """
            |query User {
                |Viewer {
                    |id
                    |mediaListOptions {
                        |scoreFormat
                    |}
                |}
            |}
            |
            """.trimMargin()
    }
}
