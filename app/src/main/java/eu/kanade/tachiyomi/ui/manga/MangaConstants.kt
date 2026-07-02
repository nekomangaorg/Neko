package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.util.chapter.MissingChapterHolder
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.Stats
import org.nekomanga.domain.snackbar.SnackbarColor
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.presentation.components.UiText

object MangaConstants {

    @Immutable
    data class StaticChapterData(
        val allChapters: List<ChapterItem>,
        val missingChapters: MissingChapterHolder,
        val allScanlators: Set<String>,
        val allUploaders: Set<String>,
        val allSources: Set<String>,
        val allLanguages: Set<String>,
    )

    @Immutable
    data class MangaScreenGeneralState(
        val isRefreshing: Boolean = false,
        val isSearching: Boolean = false,
        val firstLoad: Boolean = true,
        val snackbarColor: SnackbarColor? = null,
        val incognitoMode: Boolean = false,
        val forcePortrait: Boolean = false,
        val themeBasedOffCovers: Boolean = false,
        val vibrantColor: Int? = null,
        val hasDefaultCategory: Boolean = false,
        val hideButtonText: Boolean = false,
        val backdropSize: BackdropSize = BackdropSize.Default,
        val wrapAltTitles: Boolean = false,
        val searchChapters: List<ChapterItem> = listOf(),
        val removedChapters: List<ChapterItem> = listOf(),
    )

    @Immutable
    data class MangaScreenMangaState(
        val initialized: Boolean = false,
        val inLibrary: Boolean = false,
        val isMerged: MergeConstants.IsMergedManga = MergeConstants.IsMergedManga.No,
        val currentTitle: String = "",
        val originalTitle: String = "",
        val alternativeTitles: List<String> = listOf(),
        val artist: String = "",
        val author: String = "",
        val currentDescription: String = "",
        val genres: List<String> = listOf(),
        val status: Int = 0,
        val isPornographic: Boolean = false,
        val langFlag: String? = null,
        val missingChapters: String = "",
        val estimatedMissingChapters: String = "",
        val stats: Stats? = null,
        val lastVolume: Int? = null,
        val lastChapter: Int? = null,
        val externalLinks: List<ExternalLink> = listOf(),
        val currentArtwork: Artwork,
        val alternativeArtwork: List<Artwork> = listOf(),
        val dynamicCovers: Boolean = false,
    )

    @Immutable
    data class MangaScreenChapterState(
        val activeChapters: List<ChapterItem> = listOf(),
        val allChapters: List<ChapterItem> = listOf(),
        val nextUnreadChapter: NextUnreadChapter = NextUnreadChapter(),
        val chapterFilter: ChapterDisplay = ChapterDisplay(),
        val chapterFilterText: String = "",
        val chapterSortFilter: SortFilter = SortFilter(),
        val chapterScanlatorFilter: ScanlatorFilter = ScanlatorFilter(listOf()),
        val chapterSourceFilter: ScanlatorFilter = ScanlatorFilter(listOf()),
        val chapterLanguageFilter: LanguageFilter = LanguageFilter(listOf()),
        val allScanlators: Set<String> = setOf(),
        val allUploaders: Set<String> = setOf(),
        val allSources: Set<String> = setOf(),
        val allLanguages: Set<String> = setOf(),
    )

    @Immutable
    data class MangaScreenTrackState(
        val tracks: List<TrackItem> = listOf(),
        val loggedInTrackService: List<TrackServiceItem> = listOf(),
        val trackServiceCount: Int = 0,
        val trackingSuggestedDates: TrackingConstants.TrackingSuggestedDates? = null,
        val trackSearchResult: TrackingConstants.TrackSearchResult =
            TrackingConstants.TrackSearchResult.Loading,
    )

    @Immutable
    data class MangaScreenCategoryState(
        val allCategories: List<CategoryItem> = listOf(),
        val currentCategories: List<CategoryItem> = listOf(),
    )

    @Immutable
    data class MangaScreenMergeState(
        val validMergeTypes: List<MergeType> = listOf(),
        val mergeSearchResult: MergeConstants.MergeSearchResult =
            MergeConstants.MergeSearchResult.Loading,
    )

    @Immutable
    data class MangaDetailScreenState(
        val general: MangaScreenGeneralState,
        val manga: MangaScreenMangaState,
        val chapters: MangaScreenChapterState = MangaScreenChapterState(),
        val track: MangaScreenTrackState = MangaScreenTrackState(),
        val merge: MangaScreenMergeState = MangaScreenMergeState(),
        val category: MangaScreenCategoryState = MangaScreenCategoryState(),
    )

    /** Holds the next unread chapter and the text to display for the quick read button. */
    @Immutable
    data class NextUnreadChapter(
        val text: UiText = UiText.String(""),
        val simpleChapter: SimpleChapter? = null,
    )

    @Immutable
    data class SortFilter(
        val sourceOrderSort: SortState = SortState.None,
        val smartOrderSort: SortState = SortState.None,
        val uploadDateSort: SortState = SortState.None,
        val matchesGlobalDefaults: Boolean = true,
    )

    @Immutable data class SortOption(val sortState: SortState, val sortType: SortType)

    @Immutable
    data class ScanlatorFilter(val scanlators: List<ScanlatorOption> = listOf())

    @Immutable data class ScanlatorOption(val name: String, val disabled: Boolean = false)

    @Immutable
    data class LanguageFilter(val languages: List<LanguageOption> = listOf())

    @Immutable data class LanguageOption(val name: String, val disabled: Boolean = false)

    @Immutable
    data class ChapterDisplay(
        val showAll: Boolean = false,
        val unread: ToggleableState = ToggleableState.Off,
        val downloaded: ToggleableState = ToggleableState.Off,
        val bookmarked: ToggleableState = ToggleableState.Off,
        val hideChapterTitles: ToggleableState = ToggleableState.Off,
        val available: ToggleableState = ToggleableState.Off,
        val matchesGlobalDefaults: Boolean = true,
    )

    @Immutable
    data class ChapterDisplayOptions(
        val displayType: ChapterDisplayType,
        val displayState: ToggleableState,
    )

    enum class ChapterDisplayType {
        All,
        Unread,
        Downloaded,
        Bookmarked,
        Available,
        HideTitles,
    }

    enum class SortType {
        SourceOrder,
        ChapterNumber,
        UploadDate,
    }

    enum class SortState(val key: String) {
        Ascending(MdConstants.Sort.ascending),
        Descending(MdConstants.Sort.descending),
        None(""),
    }

    enum class SetGlobal {
        Sort,
        Filter,
    }

    @Immutable
    class ChapterFilterActions(
        val changeSort: (sortOptions: SortOption?) -> Unit,
        val changeFilter: (filterOption: ChapterDisplayOptions?) -> Unit,
        val changeScanlator: (scanlatorOption: ScanlatorOption?) -> Unit,
        val changeLanguage: (languageOption: LanguageOption?) -> Unit,
        val setAsGlobal: (SetGlobal) -> Unit,
    )

    fun SortOption.applyToManga(manga: Manga) {
        val sortInt =
            when (this.sortType) {
                SortType.ChapterNumber -> Manga.CHAPTER_SORTING_SMART
                SortType.SourceOrder -> Manga.CHAPTER_SORTING_SOURCE
                SortType.UploadDate -> Manga.CHAPTER_SORTING_UPLOAD_DATE
            }
        val descInt =
            when (this.sortState) {
                MangaConstants.SortState.Ascending -> Manga.CHAPTER_SORT_ASC
                else -> Manga.CHAPTER_SORT_DESC
            }
        manga.setChapterOrder(sortInt, descInt)
    }

    fun ChapterDisplayOptions.applyToManga(manga: Manga) {
        // Helper to map toggle state
        fun getTriState(on: Int, off: Int): Int =
            when (this.displayState) {
                ToggleableState.On -> on
                ToggleableState.Indeterminate -> off
                else -> Manga.SHOW_ALL
            }

        when (this.displayType) {
            ChapterDisplayType.All -> {
                manga.readFilter = Manga.SHOW_ALL
                manga.bookmarkedFilter = Manga.SHOW_ALL
                manga.downloadedFilter = Manga.SHOW_ALL
                manga.availableFilter = Manga.SHOW_ALL
            }
            ChapterDisplayType.Unread -> {
                manga.readFilter = getTriState(Manga.CHAPTER_SHOW_UNREAD, Manga.CHAPTER_SHOW_READ)
            }
            ChapterDisplayType.Bookmarked -> {
                manga.bookmarkedFilter =
                    getTriState(Manga.CHAPTER_SHOW_BOOKMARKED, Manga.CHAPTER_SHOW_NOT_BOOKMARKED)
            }
            ChapterDisplayType.Downloaded -> {
                manga.downloadedFilter =
                    getTriState(Manga.CHAPTER_SHOW_DOWNLOADED, Manga.CHAPTER_SHOW_NOT_DOWNLOADED)
            }
            ChapterDisplayType.Available -> {
                manga.availableFilter =
                    getTriState(Manga.CHAPTER_SHOW_AVAILABLE, Manga.CHAPTER_SHOW_UNAVAILABLE)
            }
            ChapterDisplayType.HideTitles -> {
                manga.displayMode =
                    if (this.displayState == ToggleableState.On) Manga.CHAPTER_DISPLAY_NUMBER
                    else Manga.CHAPTER_DISPLAY_NAME
            }
        }
    }

    @Immutable
    data class CategoriesData(
        val all: List<CategoryItem>,
        val current: List<CategoryItem>,
    )

    sealed class DownloadAction {
        data class DownloadNextUnread(val numberToDownload: Int) : DownloadAction()

        data object DownloadAll : DownloadAction()

        data object DownloadUnread : DownloadAction()

        data object Download : DownloadAction()

        data object ImmediateDownload : DownloadAction()

        data object Remove : DownloadAction()

        data object RemoveRead : DownloadAction()

        data object RemoveAll : DownloadAction()

        data object Cancel : DownloadAction()
    }

    @Immutable
    class CategoryActions(
        val set: (List<CategoryItem>) -> Unit = {},
        val addNew: (String) -> Unit = {},
    )

    @Immutable
    class DescriptionActions(
        val genreSearch: (String) -> Unit,
        val genreSearchLibrary: (String) -> Unit,
        val altTitleClick: (String) -> Unit,
        val altTitleResetClick: () -> Unit,
    )

    @Immutable
    class InformationActions(
        val titleLongClick: (String) -> Unit,
        val creatorCopy: (String) -> Unit,
        val creatorSearch: (String) -> Unit,
    )

    @Immutable
    class TrackActions(
        val statusChange: (Int, TrackingConstants.TrackAndService) -> Unit,
        val scoreChange: (Int, TrackingConstants.TrackAndService) -> Unit,
        val chapterChange: (Int, TrackingConstants.TrackAndService) -> Unit,
        val dateChange: (TrackingConstants.TrackDateChange) -> Unit,
        val search: (String, TrackServiceItem) -> Unit,
        val searchItemClick: (TrackingConstants.TrackAndService) -> Unit,
        val remove: (Boolean, TrackServiceItem) -> Unit,
    )

    @Immutable
    class CoverActions(
        val share: (Context, Artwork) -> Unit,
        val set: (Artwork) -> Unit,
        val save: (Artwork) -> Unit,
        val reset: () -> Unit,
    )

    @Immutable
    class MergeActions(
        val remove: (MergeMangaImpl) -> Unit,
        val search: (String, MergeType, List<String>) -> Unit,
        val add: (SourceMergeManga) -> Unit,
    )

    @Immutable
    class ChapterActions(
        val createMangaFolder: () -> Unit,
        val mark: (List<ChapterItem>, ChapterMarkActions) -> Unit,
        val clearRemoved: () -> Unit,
        val download: (List<ChapterItem>, DownloadAction) -> Unit,
        val delete: (List<ChapterItem>) -> Unit,
        val open: (ChapterItem) -> Unit,
        val blockScanlator: (BlockType, String) -> Unit,
        val openNext: () -> Unit,
        val openComment: (String) -> Unit,
        val openInBrowser: (ChapterItem) -> Unit,
        val markPrevious: (ChapterItem, Boolean) -> Unit,
    )

    enum class BlockType {
        Group,
        Uploader,
    }

    enum class BackdropSize {
        Small,
        Default,
        Large,
    }
}
