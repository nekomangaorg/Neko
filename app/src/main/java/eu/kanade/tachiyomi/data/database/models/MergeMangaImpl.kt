package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.ReducedHttpSource

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
    Komga(1),
    Toonily(2),
    ;

    companion object {
        fun getById(id: Int): MergeType {
            return values().firstOrNull { it.id == id } ?: MangaLife
        }

        fun getMergeTypeFromName(name: String?): MergeType? {
            return when (name) {
                MangaLife.name -> MangaLife
                Komga.name -> Komga
                Toonily.name -> Toonily
                else -> null
            }
        }

        fun getMergeTypeName(mergeType: MergeType): String {
            return when (mergeType) {
                MangaLife -> MangaLife.name
                Komga -> Komga.name
                Toonily -> Toonily.name
            }
        }

        fun getSource(mergeType: MergeType, sourceManager: SourceManager): ReducedHttpSource {
            return when (mergeType) {
                MangaLife -> sourceManager.mangaLife
                Komga -> sourceManager.komga
                Toonily -> sourceManager.toonily
            }
        }

        fun containsMergeSourceName(name: String?): Boolean {
            name ?: false
            return MergeType.values().any { name!!.contains(MergeType.getMergeTypeName(it)) }
        }
    }
}
