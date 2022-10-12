package eu.kanade.tachiyomi.source.online.utils

import java.util.concurrent.TimeUnit

object MdConstants {
    const val baseUrl = "https://mangadex.org"
    const val cdnUrl = "https://uploads.mangadex.org"
    const val atHomeReportUrl = "https://api.mangadex.network/report"
    const val noCoverUrl = "https://mangadex.org/cover-placeholder.jpg"

    const val currentSeasonalId = "4be9338a-3402-4f98-b467-43fb56663927"

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

    object DeepLinkPrefix {
        const val manga = "id:"
        const val group = "grp:"
    }

    object SearchParameters {
        const val titleParam = "title"
        const val contentRatingParam = "contentRating[]"
        const val originalLanguageParam = "originalLanguage[]"
        const val publicationDemographicParam = "publicationDemographic[]"
        const val includedTagModeParam = "includedTagsMode"
        const val excludedTagModeParam = "excludedTagsMode"

        object TagMode {
            const val and = "AND"
            const val or = "OR"
        }
    }

    val mdAtHomeTokenLifespan = TimeUnit.MINUTES.toMillis(5)
}
