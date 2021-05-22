package eu.kanade.tachiyomi.data.database.models

data class CachedManga(
    // Manga ID this gallery is linked to
    val title: String,
    val uuid: String,
    val rating: String,
)
