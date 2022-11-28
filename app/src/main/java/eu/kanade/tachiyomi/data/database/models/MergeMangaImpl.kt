package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource

data class MergeMangaImpl(
    val id: Long? = null,
    val mangaId: Long,
    val coverUrl: String = "",
    val url: String,
    val title: String = "",
    val mergeType: MergeType,
)

data class SourceMergeManga(
    val coverUrl: String,
    val url: String,
    val title: String,
    val mergeType: MergeType,
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
    MangaLife(0),
    Komga(1);

    companion object {
        fun getById(id: Int): MergeType {
            return values().firstOrNull { it.id == id } ?: MangaLife
        }

        fun getMergeTypeName(mergeType: MergeType): String {
            return when (mergeType) {
                MangaLife -> MangaLife.name
                Komga -> Komga.name
            }
        }

        fun getSource(mergeType: MergeType, sourceManager: SourceManager): HttpSource {
            return when (mergeType) {
                MangaLife -> sourceManager.mangaLife
                Komga -> sourceManager.komga
            }
        }
    }
}
