package eu.kanade.tachiyomi.data.database.models

data class ArtworkImpl(
    val id: Long? = null,
    val mangaId: Long,
    val fileName: String,
    val volume: String,
    val locale: String,
    val description: String,
)
