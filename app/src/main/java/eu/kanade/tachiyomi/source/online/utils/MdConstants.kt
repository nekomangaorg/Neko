package eu.kanade.tachiyomi.source.online.utils

import java.util.concurrent.TimeUnit

object MdConstants {
    const val baseUrl = "https://mangadex.org"
    const val cdnUrl = "https://uploads.mangadex.org"
    const val atHomeReportUrl = "https://api.mangadex.network/report"
    const val noCoverUrl = "https://mangadex.org/cover-placeholder.jpg"

    const val currentSeasonalId = "4be9338a-3402-4f98-b467-43fb56663927"

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
        const val followCount = "followedCount"
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
