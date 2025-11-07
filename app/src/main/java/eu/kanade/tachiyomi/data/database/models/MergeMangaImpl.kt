package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import org.nekomanga.constants.Constants

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

enum class MergeType(val id: Int, val scanlatorName: String, val baseUrl: String = "") {
    MangaLife(
        0,
        eu.kanade.tachiyomi.source.online.merged.mangalife.MangaLife.name,
        eu.kanade.tachiyomi.source.online.merged.mangalife.MangaLife.baseUrl,
    ),
    Komga(1, eu.kanade.tachiyomi.source.online.merged.komga.Komga.name),
    Toonily(
        2,
        eu.kanade.tachiyomi.source.online.merged.toonily.Toonily.name,
        eu.kanade.tachiyomi.source.online.merged.toonily.Toonily.baseUrl,
    ),
    WeebCentral(
        3,
        eu.kanade.tachiyomi.source.online.merged.weebcentral.WeebCentral.name,
        eu.kanade.tachiyomi.source.online.merged.weebcentral.WeebCentral.baseUrl,
    ),
    Comick(
        4,
        eu.kanade.tachiyomi.source.online.merged.comick.Comick.name,
        eu.kanade.tachiyomi.source.online.merged.comick.Comick.baseUrl,
    ),
    Suwayomi(5, eu.kanade.tachiyomi.source.online.merged.suwayomi.Suwayomi.name);

    companion object {
        fun getById(id: Int): MergeType {
            return entries.firstOrNull { it.id == id } ?: MangaLife
        }

        fun getMergeTypeFromName(name: String?): MergeType? {
            val splitName = name?.split(Constants.SCANLATOR_SEPARATOR)?.firstOrNull()
            return when (splitName) {
                MangaLife.scanlatorName -> MangaLife
                Komga.scanlatorName -> Komga
                Toonily.scanlatorName -> Toonily
                WeebCentral.scanlatorName -> WeebCentral
                Comick.scanlatorName -> Comick
                Suwayomi.scanlatorName -> Suwayomi
                else -> null
            }
        }

        fun getMergeTypeName(mergeType: MergeType): String {
            return mergeType.scanlatorName
        }

        fun getSource(mergeType: MergeType, sourceManager: SourceManager): ReducedHttpSource {
            return when (mergeType) {
                MangaLife -> sourceManager.mangaLife
                Komga -> sourceManager.komga
                Toonily -> sourceManager.toonily
                WeebCentral -> sourceManager.weebCentral
                Comick -> sourceManager.comick
                Suwayomi -> sourceManager.suwayomi
            }
        }

        fun containsMergeSourceName(name: String?): Boolean {
            name ?: false
            return entries.any { name!!.contains(MergeType.getMergeTypeName(it)) }
        }
    }
}
