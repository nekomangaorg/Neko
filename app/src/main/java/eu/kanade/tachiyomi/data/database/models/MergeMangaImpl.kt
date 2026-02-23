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

// id 0 was MangaLife, 4 was Comick,
enum class MergeType(val id: Int, val scanlatorName: String, val baseUrl: String = "") {
    Invalid(id = -1, scanlatorName = "Invalid Merge source"),
    Komga(id = 1, scanlatorName = eu.kanade.tachiyomi.source.online.merged.komga.Komga.name),
    Toonily(
        id = 2,
        scanlatorName = eu.kanade.tachiyomi.source.online.merged.toonily.Toonily.name,
        baseUrl = eu.kanade.tachiyomi.source.online.merged.toonily.Toonily.baseUrl,
    ),
    WeebCentral(
        id = 3,
        scanlatorName = eu.kanade.tachiyomi.source.online.merged.weebcentral.WeebCentral.name,
        baseUrl = eu.kanade.tachiyomi.source.online.merged.weebcentral.WeebCentral.baseUrl,
    ),
    Suwayomi(
        id = 5,
        scanlatorName = eu.kanade.tachiyomi.source.online.merged.suwayomi.Suwayomi.name,
    ),
    MangaBall(
        id = 7,
        scanlatorName = eu.kanade.tachiyomi.source.online.merged.mangaball.MangaBall.name,
        baseUrl = eu.kanade.tachiyomi.source.online.merged.mangaball.MangaBall.baseUrl,
    ),
    WeebDex(
        id = 8,
        scanlatorName = eu.kanade.tachiyomi.source.online.merged.weebdex.WeebDex.name,
        baseUrl = eu.kanade.tachiyomi.source.online.merged.weebdex.WeebDex.baseUrl,
    ),
    ProjectSuki(
        id = 9,
        scanlatorName = eu.kanade.tachiyomi.source.online.merged.projectsuki.ProjectSuki.name,
        baseUrl = eu.kanade.tachiyomi.source.online.merged.projectsuki.ProjectSuki.baseUrl,
    ),
    Comix(
        id = 10,
        scanlatorName = eu.kanade.tachiyomi.source.online.merged.comix.Comix.name,
        baseUrl = eu.kanade.tachiyomi.source.online.merged.comix.Comix.baseUrl,
    );

    companion object {
        fun getById(id: Int): MergeType {
            return entries.firstOrNull { it.id == id } ?: Invalid
        }

        fun getMergeTypeFromName(name: String?): MergeType? {
            val splitName = name?.split(Constants.SCANLATOR_SEPARATOR)?.firstOrNull()
            return when (splitName) {
                Komga.scanlatorName -> Komga
                Toonily.scanlatorName -> Toonily
                WeebCentral.scanlatorName -> WeebCentral
                Suwayomi.scanlatorName -> Suwayomi
                MangaBall.scanlatorName -> MangaBall
                WeebDex.scanlatorName -> WeebDex
                ProjectSuki.scanlatorName -> ProjectSuki
                Comix.scanlatorName -> Comix
                else -> null
            }
        }

        fun getMergeTypeName(mergeType: MergeType): String {
            return mergeType.scanlatorName
        }

        fun getSource(mergeType: MergeType, sourceManager: SourceManager): ReducedHttpSource {
            return when (mergeType) {
                Komga -> sourceManager.komga
                Toonily -> sourceManager.toonily
                WeebCentral -> sourceManager.weebCentral
                Suwayomi -> sourceManager.suwayomi
                MangaBall -> sourceManager.mangaBall
                WeebDex -> sourceManager.weebDex
                ProjectSuki -> sourceManager.projectSuki
                Comix -> sourceManager.comix
                Invalid -> sourceManager.invalidMergeSource
            }
        }

        fun containsMergeSourceName(name: String?): Boolean {
            name ?: false
            return entries.any { name!!.contains(MergeType.getMergeTypeName(it)) }
        }
    }
}
