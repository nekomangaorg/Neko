package eu.kanade.tachiyomi.source.online.utils

object MdApi {
    const val baseUrl = "https://api.mangadex.org"
    const val login = "/auth/login"
    const val checkToken = "/auth/check"
    const val refreshToken = "/auth/refresh"
    const val logout = "/auth/logout"
    const val manga = "/manga"
    const val statistics = "/statistics/manga"
    const val chapter = "/chapter"
    const val cover = "/cover"
    const val group = "/group"
    const val author = "/author"
    const val userFollows = "/user/follows/manga"
    const val readingStatusForAllManga = "/manga/status"
    const val rating = "/rating"
    const val atHomeServer = "/at-home/server"

    const val legacyMapping = "/legacy/mapping"
}
