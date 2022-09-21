package eu.kanade.tachiyomi.source.online.models.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MangaPlusResponse(
    val success: MangaPLusSuccessResult? = null,
    val error: MangaPlusErrorResult? = null,
)

@Serializable
data class MangaPlusErrorResult(
    val popups: List<Popup> = emptyList(),
)

@Serializable
data class Popup(
    val subject: String,
    val body: String,
    val language: Language? = Language.ENGLISH,
)

@Serializable
data class MangaPLusSuccessResult(
    val isFeaturedUpdated: Boolean? = false,
    val titleRankingView: TitleRankingView? = null,
    val titleDetailView: TitleDetailView? = null,
    val mangaViewer: MangaViewer? = null,
    val allTitlesViewV2: AllTitlesViewV2? = null,
    val webHomeViewV3: WebHomeViewV3? = null,
)

@Serializable
data class TitleRankingView(val titles: List<Title> = emptyList())

@Serializable
data class AllTitlesViewV2(
    @SerialName("AllTitlesGroup") val allTitlesGroup: List<AllTitlesGroup> = emptyList(),
)

@Serializable
data class AllTitlesGroup(
    val theTitle: String,
    val titles: List<Title> = emptyList(),
)

@Serializable
data class WebHomeViewV3(val groups: List<UpdatedTitleV2Group> = emptyList())

@Serializable
data class TitleDetailView(
    val title: Title,
    val titleImageUrl: String,
    val overview: String,
    val backgroundImageUrl: String,
    val nextTimeStamp: Int = 0,
    val viewingPeriodDescription: String = "",
    val nonAppearanceInfo: String = "",
    val firstChapterList: List<Chapter> = emptyList(),
    val lastChapterList: List<Chapter> = emptyList(),
    val isSimulReleased: Boolean = false,
    val chaptersDescending: Boolean = true,
)

@Serializable
data class MangaViewer(val pages: List<MangaPlusPage> = emptyList())

@Serializable
data class Title(
    val titleId: Int,
    val name: String,
    val author: String,
    val portraitImageUrl: String,
    val landscapeImageUrl: String,
    val viewCount: Int = 0,
    val language: Language? = Language.ENGLISH,
)

enum class Language {
    ENGLISH,
    SPANISH,
    FRENCH,
    INDONESIAN,
    PORTUGUESE_BR,
    RUSSIAN,
    THAI,
}

@Serializable
data class UpdatedTitleV2Group(
    val groupName: String,
    val titleGroups: List<OriginalTitleGroup> = emptyList(),
)

@Serializable
data class OriginalTitleGroup(
    val theTitle: String,
    val titles: List<UpdatedTitle> = emptyList(),
)

@Serializable
data class UpdatedTitle(val title: Title)

@Serializable
data class Chapter(
    val titleId: Int,
    val chapterId: Int,
    val name: String,
    val subTitle: String? = null,
    val startTimeStamp: Int,
    val endTimeStamp: Int,
    val isVerticalOnly: Boolean = false,
)

@Serializable
data class MangaPlusPage(val mangaPage: MangaPage? = null)

@Serializable
data class MangaPage(
    val imageUrl: String,
    val width: Int,
    val height: Int,
    val encryptionKey: String? = null,
)
