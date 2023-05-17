package eu.kanade.tachiyomi.source.online.utils

object MdApi {
    const val baseUrl = "https://api.mangadex.dev"
    const val manga = "/manga"
    const val list = "/list"
    const val statistics = "/statistics"
    const val chapter = "/chapter"
    const val cover = "/cover"
    const val group = "/group"
    const val author = "/author"
    const val user = "/user"
    const val userInfo = "$user/me"
    const val listMigrated = "$list/migrated"
    const val userFollows = "$user/follows/manga"
    const val readingStatusForAllManga = "/manga/status"
    const val rating = "/rating"
    const val atHomeServer = "/at-home/server"
    const val legacyMapping = "/legacy/mapping"

    const val baseAuthUrl = "https://auth.mangadex.dev"
    private const val auth = "/realms/mangadex/protocol/openid-connect"
    const val login = "$auth/auth"
    const val logout = "$auth/logout"
    const val token = "$auth/token"
}

