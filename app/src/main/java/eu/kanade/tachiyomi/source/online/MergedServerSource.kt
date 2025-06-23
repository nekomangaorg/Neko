package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.SChapter

abstract class MergedServerSource : ReducedHttpSource() {

    open fun requiresCredentials(): Boolean = true

    abstract fun hostUrl(): String

    open fun getMangaUrl(url: String): String = hostUrl() + url

    abstract suspend fun loginWithUrl(username: String, password: String, url: String): Boolean

    abstract suspend fun isLoggedIn(): Boolean

    abstract fun isConfigured(): Boolean

    abstract suspend fun updateStatusChapters(chapters: List<SChapter>, read: Boolean)
}
