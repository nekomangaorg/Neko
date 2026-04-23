package eu.kanade.tachiyomi.data.track.mangabaka

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.PkceCodes
import eu.kanade.tachiyomi.util.PkceUtil
import eu.kanade.tachiyomi.util.system.toLocalDate
import eu.kanade.tachiyomi.util.system.withIOContext
import java.math.RoundingMode
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.Headers.Companion.headersOf
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.nekomanga.BuildConfig
import org.nekomanga.core.network.DELETE
import org.nekomanga.core.network.GET
import org.nekomanga.core.network.POST
import org.nekomanga.core.network.PUT
import org.nekomanga.data.network.mangabaka.dto.MangaBakaLibraryListResult
import org.nekomanga.data.network.mangabaka.dto.MangaBakaOAuth
import org.nekomanga.data.network.mangabaka.dto.MangaBakaProfile
import org.nekomanga.data.network.mangabaka.dto.MangaBakaSeries
import org.nekomanga.data.network.mangabaka.dto.MangaBakaSeriesResult
import org.nekomanga.data.network.mangabaka.dto.MangaBakaSeriesSearchResult
import org.nekomanga.data.network.mangabaka.dto.MangaBakaUserProfileResponse
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.HttpException
import tachiyomi.core.network.awaitSuccess
import tachiyomi.core.network.parseAs
import uy.kohesive.injekt.injectLazy

class MangaBakaApi(
    private val trackId: Int,
    baseClient: OkHttpClient,
    interceptor: MangaBakaInterceptor,
) {
    private val client =
        baseClient
            .newBuilder()
            .addInterceptor {
                it.request()
                    .newBuilder()
                    .header(
                        "User-Agent",
                        buildString {
                            append("Neko/v${BuildConfig.VERSION_NAME} ")
                            append("(${BuildConfig.APPLICATION_ID} ${BuildConfig.COMMIT_SHA}) ")
                            append("(Android) (https://github.com/nekomangaorg/Neko)")
                        },
                    )
                    .build()
                    .let(it::proceed)
            }
            .build()

    private val json: Json by injectLazy()

    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun addLibManga(track: Track): Track {
        return withIOContext {
            val url = "$LIBRARY_API_URL/${track.media_id}"
            val body =
                buildJsonObject {
                        //  put("is_private", track.private)
                        put("state", track.toApiStatus())
                        if (track.last_chapter_read > 0.0) {
                            put("progress_chapter", track.last_chapter_read)
                        }
                        if (track.score > 0) {
                            put("rating", track.score.toInt().coerceIn(0, 100))
                        }
                        if (track.started_reading_date > 0) {
                            put("start_date", track.started_reading_date.toLocalDate().toString())
                        }
                        if (track.finished_reading_date > 0) {
                            put("finish_date", track.finished_reading_date.toLocalDate().toString())
                        }
                    }
                    .toString()
                    .toRequestBody()

            authClient
                .newCall(POST(url, body = body, headers = headersOf("Content-Type", APP_JSON)))
                .awaitSuccess()

            // only returns 201 with the body { "status": 201, "data": true }, so no library ID for
            // us
            track
        }
    }

    suspend fun findLibManga(track: Track): Track? {
        return withIOContext {
            with(json) {
                try {
                    val url = "$LIBRARY_API_URL/${track.media_id}"
                    val userData =
                        authClient
                            .newCall(GET(url))
                            .awaitSuccess()
                            .parseAs<MangaBakaLibraryListResult>()
                            .data

                    val additionalData =
                        authClient
                            .newCall(GET("$API_BASE_URL/v1/series/${track.media_id}"))
                            .awaitSuccess()
                            .parseAs<MangaBakaSeriesResult>()
                            .data

                    Track.create(TrackManager.MANGABAKA).apply {
                        media_id = track.media_id
                        title = additionalData.parseTitle()
                        status = userData.getStatus()
                        score = userData.rating?.toFloat() ?: 0.0f
                        started_reading_date =
                            userData.startDate?.let { Instant.parse(it).toEpochMilliseconds() } ?: 0
                        finished_reading_date =
                            userData.finishDate?.let { Instant.parse(it).toEpochMilliseconds() }
                                ?: 0
                        last_chapter_read = userData.progressChapter?.toFloat() ?: 0.0f
                        // private = userData.isPrivate
                    }
                } catch (e: Exception) {
                    if (e is HttpException && e.code == 404) {
                        null
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    suspend fun updateLibManga(track: Track): Track {
        return withIOContext {
            val url = "$LIBRARY_API_URL/${track.media_id}"
            val body =
                buildJsonObject {
                        put("state", track.toApiStatus())
                        // put("is_private", track.private)
                        if (track.last_chapter_read > 0.0) {
                            put("progress_chapter", track.last_chapter_read)
                        } else {
                            put("progress_chapter", null)
                        }
                        if (track.score > 0) {
                            put("rating", track.score.toInt().coerceIn(0, 100))
                        } else {
                            put("rating", null)
                        }
                        if (track.started_reading_date > 0) {
                            put("start_date", track.started_reading_date.toLocalDate().toString())
                        } else {
                            put("start_date", null)
                        }
                        if (track.finished_reading_date > 0) {
                            put("finish_date", track.finished_reading_date.toLocalDate().toString())
                        } else {
                            put("finish_date", null)
                        }
                    }
                    .toString()
                    .toRequestBody()

            authClient
                .newCall(PUT(url, body = body, headers = headersOf("Content-Type", APP_JSON)))
                .awaitSuccess()

            track
        }
    }

    suspend fun search(search: String): List<TrackSearch> {
        return withIOContext {
            val url =
                "$API_BASE_URL/v1/series/search"
                    .toUri()
                    .buildUpon()
                    .appendQueryParameter("q", search)
                    .appendQueryParameter("type_not", "novel")
                    .build()
            with(json) {
                client
                    .newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MangaBakaSeriesSearchResult>()
                    .data
                    .map { item -> parseSearchItem(item) }
            }
        }
    }

    suspend fun remove(track: Track): Boolean {
        try {
            val url = "$LIBRARY_API_URL/${track.media_id}"
            authClient.newCall(DELETE(url)).awaitSuccess()
            return true
        } catch (e: Exception) {
            TimberKt.e(e) { "Error trying to remove from MangaBaka" }
        }
        return false
    }

    private fun parseSearchItem(item: MangaBakaSeries): TrackSearch {
        return TrackSearch.create(trackId).apply {
            media_id = item.id
            title = item.parseTitle()
            summary = item.description?.trim().orEmpty()
            score =
                item.rating?.toBigDecimal()?.setScale(2, RoundingMode.HALF_UP)?.toFloat() ?: -1.0f
            cover_url = item.cover.x250.x1.orEmpty()
            tracking_url = "$BASE_URL/${item.id}"
            start_date = item.published?.startDate.orEmpty()
            publishing_status = item.status.toString().lowercase()
            publishing_type = ""
        }
    }

    suspend fun getMangaDetails(id: Int): TrackSearch? {
        return withIOContext {
            val url =
                "$API_BASE_URL/v1/series".toUri().buildUpon().appendPath(id.toString()).build()
            with(json) {
                try {
                    authClient
                        .newCall(GET(url.toString()))
                        .awaitSuccess()
                        .parseAs<MangaBakaSeriesResult>()
                        .data
                        .let { parseSearchItem(it) }
                } catch (e: HttpException) {
                    if (e.code == 404) {
                        return@with null
                    }
                    throw e
                }
            }
        }
    }

    suspend fun getUserProfile(): MangaBakaProfile {
        return withIOContext {
            with(json) {
                authClient
                    .newCall(GET("$API_BASE_URL/v1/my/profile"))
                    .awaitSuccess()
                    .parseAs<MangaBakaUserProfileResponse>()
                    .data
            }
        }
    }

    suspend fun getAccessToken(code: String, codeVerifier: String): MangaBakaOAuth {
        return withIOContext {
            val formBody =
                FormBody.Builder()
                    .add("client_id", CLIENT_ID)
                    .add("code", code)
                    .add("code_verifier", codeVerifier)
                    .add("grant_type", "authorization_code")
                    .add("redirect_uri", REDIRECT_URI)
                    .add("scope", SCOPES)
                    .build()

            with(json) {
                client.newCall(POST("${OAUTH_URL}/token", body = formBody)).awaitSuccess().parseAs()
            }
        }
    }

    fun verifyOAuthState(state: String): Boolean = state == oauthStateParam

    companion object {
        private const val CLIENT_ID = "UZqzTejmOGFldPOSpDHjiTYZKxPthHUa"

        private var oauthStateParam: String = ""

        private const val BASE_URL = "https://mangabaka.org"
        private const val API_BASE_URL = "https://api.mangabaka.dev"
        private const val LIBRARY_API_URL = "$API_BASE_URL/v1/my/library"
        private const val OAUTH_URL = "$BASE_URL/auth/oauth2"
        private const val SCOPES = "library.read library.write offline_access openid"

        private const val REDIRECT_URI = "neko://mangabaka-auth"

        private const val APP_JSON = "application/json"

        fun authUrl(codeChallenge: String): Uri =
            "$OAUTH_URL/authorize"
                .toUri()
                .buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", SCOPES)
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("state", getOAuthStateParam())
                .build()

        fun refreshTokenRequest(token: String) =
            POST(
                "$OAUTH_URL/token",
                body =
                    FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("client_id", CLIENT_ID)
                        .add("refresh_token", token)
                        .add("redirect_uri", REDIRECT_URI)
                        .build(),
            )

        private fun getOAuthStateParam(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            oauthStateParam = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

            return oauthStateParam
        }

        fun getPkceS256ChallengeCode(): PkceCodes {
            // MangaBaka requires an actually conformant PKCE process, unlike MAL
            // 1. create verifier
            // 2. create challenge from verifier (S256 hash -> base64 URL encode)
            // 3. send challenge to /authorize
            // 4. send verifier for access tokens to /token
            val codes = PkceUtil.generateS256Codes()
            return codes
        }
    }
}
