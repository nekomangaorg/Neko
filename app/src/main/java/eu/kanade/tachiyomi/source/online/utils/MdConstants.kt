package eu.kanade.tachiyomi.source.online.utils

import java.util.concurrent.TimeUnit

object MdConstants {
    const val baseUrl = "https://mangadex.org"
    const val cdnUrl = "https://uploads.mangadex.org"
    const val atHomeReportUrl = "https://api.mangadex.network/report"

    const val coverArt = "cover_art"
    const val scanlator = "scanlation_group"
    const val author = "author"
    const val artist = "artist"

    val mdAtHomeTokenLifespan = TimeUnit.MINUTES.toMillis(5)
}
