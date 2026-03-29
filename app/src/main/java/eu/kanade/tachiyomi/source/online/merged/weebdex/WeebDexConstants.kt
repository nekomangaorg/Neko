package eu.kanade.tachiyomi.source.online.merged.weebdex

object WeebDexConstants {

    // API Base URLs
    const val DOMAIN = "weebdex.org"
    const val BASE_URL = "https://$DOMAIN"
    const val API_URL = "https://api.$DOMAIN"

    // API Endpoints
    const val API_MANGA_URL = "$API_URL/manga"
    const val API_CHAPTER_URL = "$API_URL/chapter"

    // Rate Limit (API is 5 req/s, using conservative value)
    const val RATE_LIMIT = 3
}
