import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.protobuf.ProtoId

@Serializer(forClass = MangaPlusResponse::class)
object MangaPlusSerializer

@Serializable
data class MangaPlusResponse(
    @ProtoId(1) val success: SuccessResult? = null,
    @ProtoId(2) val error: ErrorResult? = null
)

@Serializable
data class ErrorResult(
    @ProtoId(1) val action: Action,
    @ProtoId(2) val englishPopup: Popup,
    @ProtoId(3) val spanishPopup: Popup
)

enum class Action { DEFAULT, UNAUTHORIZED, MAINTAINENCE, GEOIP_BLOCKING }

@Serializable
data class Popup(
    @ProtoId(1) val subject: String,
    @ProtoId(2) val body: String
)

@Serializable
data class SuccessResult(
    @ProtoId(1) val isFeaturedUpdated: Boolean? = false,
    @ProtoId(5) val allTitlesView: AllTitlesView? = null,
    @ProtoId(6) val titleRankingView: TitleRankingView? = null,
    @ProtoId(8) val titleDetailView: TitleDetailView? = null,
    @ProtoId(10) val mangaViewer: MangaViewer? = null,
    @ProtoId(11) val webHomeView: WebHomeView? = null
)

@Serializable
data class TitleRankingView(@ProtoId(1) val titles: List<Title> = emptyList())

@Serializable
data class AllTitlesView(@ProtoId(1) val titles: List<Title> = emptyList())

@Serializable
data class WebHomeView(@ProtoId(2) val groups: List<UpdatedTitleGroup> = emptyList())

@Serializable
data class TitleDetailView(
    @ProtoId(1) val title: Title,
    @ProtoId(2) val titleImageUrl: String,
    @ProtoId(3) val overview: String,
    @ProtoId(4) val backgroundImageUrl: String,
    @ProtoId(5) val nextTimeStamp: Int = 0,
    @ProtoId(6) val updateTiming: UpdateTiming? = UpdateTiming.DAY,
    @ProtoId(7) val viewingPeriodDescription: String = "",
    @ProtoId(9) val firstChapterList: List<Chapter> = emptyList(),
    @ProtoId(10) val lastChapterList: List<Chapter> = emptyList(),
    @ProtoId(14) val isSimulReleased: Boolean = true,
    @ProtoId(17) val chaptersDescending: Boolean = true
)

enum class UpdateTiming { NOT_REGULARLY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, DAY }

@Serializable
data class MangaViewer(@ProtoId(1) val pages: List<MangaPlusPage> = emptyList())

@Serializable
data class Title(
    @ProtoId(1) val titleId: Int,
    @ProtoId(2) val name: String,
    @ProtoId(3) val author: String,
    @ProtoId(4) val portraitImageUrl: String,
    @ProtoId(5) val landscapeImageUrl: String,
    @ProtoId(6) val viewCount: Int,
    @ProtoId(7) val language: Language? = Language.ENGLISH
)

enum class Language(val id: Int) {
    @ProtoId(0)
    ENGLISH(0),

    @ProtoId(1)
    SPANISH(1)
}

@Serializable
data class UpdatedTitleGroup(
    @ProtoId(1) val groupName: String,
    @ProtoId(2) val titles: List<UpdatedTitle> = emptyList()
)

@Serializable
data class UpdatedTitle(
    @ProtoId(1) val title: Title? = null
)

@Serializable
data class Chapter(
    @ProtoId(1) val titleId: Int,
    @ProtoId(2) val chapterId: Int,
    @ProtoId(3) val name: String,
    @ProtoId(4) val subTitle: String? = null,
    @ProtoId(6) val startTimeStamp: Int,
    @ProtoId(7) val endTimeStamp: Int
)

@Serializable
data class MangaPlusPage(@ProtoId(1) val page: MangaPage? = null)

@Serializable
data class MangaPage(
    @ProtoId(1) val imageUrl: String,
    @ProtoId(2) val width: Int,
    @ProtoId(3) val height: Int,
    @ProtoId(5) val encryptionKey: String? = null
)
