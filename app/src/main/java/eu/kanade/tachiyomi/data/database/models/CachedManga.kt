package eu.kanade.tachiyomi.data.database.models

data class CachedManga(
    // Manga ID this gallery is linked to
    val mangaId: Long,
    val title: String,
)