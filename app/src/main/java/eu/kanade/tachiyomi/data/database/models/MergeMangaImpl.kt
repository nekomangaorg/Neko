package eu.kanade.tachiyomi.data.database.models

data class MergeMangaImpl(
    val id: Long? = null,
    val mangaId: Long,
    val coverUrl: String = "",
    val url: String,
    val title: String = "",
    val mergeType: MergeType = MergeType.MangaLife,
)

data class SourceMergeManga(
    val coverUrl: String,
    val url: String,
    val title: String,
    val mergeType: MergeType = MergeType.MangaLife,
) {
    fun toMergeMangaImpl(mangaId: Long): MergeMangaImpl {
        return MergeMangaImpl(
            mangaId = mangaId,
            coverUrl = this.coverUrl,
            url = this.url,
            title = this.title,
            mergeType = this.mergeType,
        )
    }
}

enum class MergeType(val id: Int) {
    MangaLife(0);

    companion object {
        fun getById(id: Int): MergeType {
            return values().firstOrNull { it.id == id } ?: MangaLife
        }
    }
}
