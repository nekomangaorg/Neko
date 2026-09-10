package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.ReducedHttpSource
import eu.kanade.tachiyomi.source.online.merged.atsumaru.Atsumaru as AtsumaruSource
import eu.kanade.tachiyomi.source.online.merged.comix.Comix as ComixSource
import eu.kanade.tachiyomi.source.online.merged.kagane.Kagane as KaganeSource
import eu.kanade.tachiyomi.source.online.merged.komga.Komga as KomgaSource
import eu.kanade.tachiyomi.source.online.merged.mangaball.MangaBall as MangaBallSource
import eu.kanade.tachiyomi.source.online.merged.projectsuki.ProjectSuki as ProjectSukiSource
import eu.kanade.tachiyomi.source.online.merged.suwayomi.Suwayomi as SuwayomiSource
import eu.kanade.tachiyomi.source.online.merged.toonily.Toonily as ToonilySource
import eu.kanade.tachiyomi.source.online.merged.weebcentral.WeebCentral as WeebCentralSource
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
    val language: String? = null,
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
enum class MergeType(
    val id: Int,
    val scanlatorName: String,
    val baseUrl: String = "",
    val multiMerge: Boolean = false,
) {
    Invalid(id = -1, scanlatorName = "Invalid Merge source"),
    Komga(
        id = 1,
        scanlatorName = KomgaSource.name,
        multiMerge = true,
    ),
    Toonily(
        id = 2,
        scanlatorName = ToonilySource.name,
        baseUrl = ToonilySource.baseUrl,
    ),
    WeebCentral(
        id = 3,
        scanlatorName = WeebCentralSource.name,
        baseUrl = WeebCentralSource.baseUrl,
    ),
    Suwayomi(
        id = 5,
        scanlatorName = SuwayomiSource.name,
        multiMerge = true,
    ),
    MangaBall(
        id = 7,
        scanlatorName = MangaBallSource.name,
        baseUrl = MangaBallSource.baseUrl,
    ),
    ProjectSuki(
        id = 9,
        scanlatorName = ProjectSukiSource.name,
        baseUrl = ProjectSukiSource.baseUrl,
    ),
    Comix(
        id = 10,
        scanlatorName = ComixSource.name,
        baseUrl = ComixSource.baseUrl,
    ),
    Atsumaru(
        id = 11,
        scanlatorName = AtsumaruSource.name,
        baseUrl = AtsumaruSource.baseUrl,
    ),
    Kagane(
        id = 12,
        scanlatorName = KaganeSource.name,
        baseUrl = KaganeSource.baseUrl,
        multiMerge = true,
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
                ProjectSuki.scanlatorName -> ProjectSuki
                Comix.scanlatorName -> Comix
                Atsumaru.scanlatorName -> Atsumaru
                Kagane.scanlatorName -> Kagane
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
                ProjectSuki -> sourceManager.projectSuki
                Comix -> sourceManager.comix
                Atsumaru -> sourceManager.atsumaru
                Kagane -> sourceManager.kagane
                Invalid -> sourceManager.invalidMergeSource
            }
        }

        fun containsMergeSourceName(name: String?): Boolean {
            name ?: return false
            return entries.any { name.contains(MergeType.getMergeTypeName(it)) }
        }
    }
}
