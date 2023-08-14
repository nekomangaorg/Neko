package org.nekomanga.constants

import android.util.Base64
import androidx.core.net.toUri
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object MdConstants {
    const val currentSeasonalId = "1b9f88f8-9880-464d-9ed9-59b7e36392e2" //Summer 2023
    const val oldSeasonalId = "a153b4e6-1fcc-4f45-a990-f37f989c0d74"

    const val baseUrl = "https://mangadex.org"
    const val cdnUrl = "https://uploads.mangadex.dev"
    const val forumUrl = "https://forums.mangadex.org/threads/"
    const val atHomeReportUrl = "https://api.mangadex.network/report"
    const val noCoverUrl = "https://mangadex.org/cover-placeholder.jpg"
    const val imageUrlCacheNotFound =
        "https://cdn.statically.io/img/raw.githubusercontent.com/CarlosEsco/Neko/master/.github/manga_cover_not_found.png"

    const val chapterSuffix = "/chapter/"

    object Api {
        const val baseUrl = "https://api.mangadex.dev"
        const val manga = "/manga"
        const val list = "/list"
        const val statistics = "/statistics"
        const val chapter = "/chapter"
        const val cover = "/cover"
        const val group = "/group"
        const val author = "/author"
        const val subscriptionFeed = "/subscription/feed"
        const val userFollows = "/user/follows/manga"
        const val readingStatusForAllManga = "/manga/status"
        const val rating = "/rating"
        const val atHomeServer = "/at-home/server"
        const val user = "/user"
        const val userInfo = "$user/me"
        const val listMigrated = "$list/migrated"
        const val legacyMapping = "/legacy/mapping"

        const val baseAuthUrl = "https://auth.mangadex.dev"
        private const val auth = "/realms/mangadex/protocol/openid-connect"
        const val login = "$auth/auth"
        const val logout = "$auth/logout"
        const val token = "$auth/token"
    }

    object Login {
        val redirectUri = if (BuildConfig.DEBUG) "neko://mangadex-auth-debug" else "neko://mangadex-auth"
        const val clientId = "neko"
        const val authorizationCode = "authorization_code"
        const val refreshToken = "refresh_token"

        fun authUrl(codeVerifier: String): String {
            val bytes = codeVerifier.toByteArray()
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(bytes)
            val digest = messageDigest.digest()
            val encoding = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            val codeChallenge = Base64.encodeToString(digest, encoding)

            return Api.baseAuthUrl + Api.login.toUri().buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .build().toString()
        }
    }

    object Limits {
        const val manga = 20
        const val artwork = 100
        const val author = 100
        const val group = 100
        const val latest = 100
        const val latestSmaller = 50
    }

    object Sort {
        const val latest = "latestUploadedChapter"
        const val relevance = "relevance"
        const val subscribedCount = "subscribedCount"
        const val createdAt = "createdAt"
        const val updatedAt = "updatedAt"
        const val title = "title"
        const val rating = "rating"
        const val year = "year"
        const val ascending = "asc"
        const val descending = "desc"
    }

    object Types {
        const val author = "author"
        const val artist = "artist"
        const val coverArt = "cover_art"
        const val manga = "manga"
        const val scanlator = "scanlation_group"
    }

    object Visibility {
        const val public = "public"
        const val private = "private"
    }

    enum class FeedType {
        Subscription,
        Latest
    }

    object ContentRating {
        const val safe = "safe"
        const val suggestive = "suggestive"
        const val erotica = "erotica"
        const val pornographic = "pornographic"
        const val unknown = "unknown"
    }

    object Demographic {
        const val none = "none"
        const val shounen = "shounen"
        const val shoujo = "shoujo"
        const val seinen = "seinen"
        const val josei = "josei"
    }

    object Status {
        const val cancelled = "cancelled"
        const val ongoing = "ongoing"
        const val completed = "completed"
        const val hiatus = "hiatus"
    }

    object DeepLinkPrefix {
        const val author = "author:"
        const val group = "group:"
        const val list = "list:"
        const val manga = "manga:"
        const val error = "error:"
    }

    object SearchParameters {
        const val titleParam = "title"
        const val availableTranslatedLanguage = "availableTranslatedLanguage[]"
        const val contentRatingParam = "contentRating[]"
        const val originalLanguageParam = "originalLanguage[]"
        const val publicationDemographicParam = "publicationDemographic[]"
        const val limit = "limit"
        const val offset = "offset"
        const val statusParam = "status[]"
        const val authorOrArtist = "authorOrArtist"
        const val group = "group"
        const val includedTagsParam = "includedTags[]"
        fun sortParam(sort: String) = "order[$sort]"
        const val excludedTagsParam = "excludedTags[]"
        const val includedTagModeParam = "includedTagsMode"
        const val excludedTagModeParam = "excludedTagsMode"

        object TagMode {
            const val and = "AND"
            const val or = "OR"
        }
    }

    val mdAtHomeTokenLifespan = TimeUnit.MINUTES.toMillis(5)
}
