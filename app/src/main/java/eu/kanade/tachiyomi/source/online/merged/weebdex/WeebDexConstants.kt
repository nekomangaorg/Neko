package eu.kanade.tachiyomi.source.online.merged.weebdex

object WeebDexConstants {

    // API Base URLs
    const val API_URL = "https://api.weebdex.org"
    const val CDN_URL = "https://srv.notdelta.xyz"

    // API Endpoints
    const val API_MANGA_URL = "$API_URL/manga"

    // CDN Endpoints
    const val CDN_COVER_URL = "$CDN_URL/covers"
    const val CDN_DATA_URL = "$CDN_URL/data"

    // Rate Limit (API is 5 req/s, using conservative value)
    const val RATE_LIMIT = 3
}
