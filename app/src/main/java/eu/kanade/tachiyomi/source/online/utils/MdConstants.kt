package eu.kanade.tachiyomi.source.online.utils

import java.util.concurrent.TimeUnit

object MdConstants {
    const val baseUrl = "https://mangadex.org"
    const val cdnUrl = "https://uploads.mangadex.org"
    const val atHomeReportUrl = "https://api.mangadex.network/report"
    const val noCoverUrl = "https://mangadex.org/_nuxt/img/cover-placeholder.d12c3c5.jpg"

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
    }

    val mdAtHomeTokenLifespan = TimeUnit.MINUTES.toMillis(5)
}
