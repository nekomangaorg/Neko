package eu.kanade.tachiyomi.data.database.models

data class SourceArtwork(
    val fileName: String,
    val volume: String,
    val locale: String,
    val description: String,
) {
    fun toArtworkImpl(mangaId: Long): ArtworkImpl {
        return ArtworkImpl(
            mangaId = mangaId,
            fileName = this.fileName,
            volume = this.volume,
            locale = this.locale,
            description = this.description,
        )
    }
}

data class ArtworkImpl(
    val id: Long? = null,
    val mangaId: Long,
    val fileName: String,
    val volume: String,
    val locale: String,
    val description: String,
)
