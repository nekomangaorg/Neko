package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.util.chapter.MissingChapterHolder
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
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
        val allChapters: PersistentList<ChapterItem>,
        val missingChapters: MissingChapterHolder,
        val allScanlators: PersistentSet<String>,
        val allUploaders: PersistentSet<String>,
        val allSources: PersistentSet<String>,
        val allLanguages: PersistentSet<String>,
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
        val searchChapters: PersistentList<ChapterItem> = persistentListOf(),
        val removedChapters: PersistentList<ChapterItem> = persistentListOf(),
    )

    @Immutable
    data class MangaScreenMangaState(
        val initialized: Boolean = false,
        val inLibrary: Boolean = false,
        val isMerged: MergeConstants.IsMergedManga = MergeConstants.IsMergedManga.No,
        val currentTitle: String = "",
        val originalTitle: String = "",
        val alternativeTitles: PersistentList<String> = persistentListOf(),
        val artist: String = "",
        val author: String = "",
        val currentDescription: String = "",
        val genres: PersistentList<String> = persistentListOf(),
        val status: Int = 0,
        val isPornographic: Boolean = false,
        val langFlag: String? = null,
        val missingChapters: String = "",
        val estimatedMissingChapters: String = "",
        val stats: Stats? = null,
        val lastVolume: Int? = null,
        val lastChapter: Int? = null,
        val externalLinks: PersistentList<ExternalLink> = persistentListOf(),
        val currentArtwork: Artwork,
        val alternativeArtwork: PersistentList<Artwork> = persistentListOf(),
        val dynamicCovers: Boolean = false,
    )

    @Immutable
    data class MangaScreenChapterState(
        val activeChapters: PersistentList<ChapterItem> = persistentListOf(),
        val allChapters: PersistentList<ChapterItem> = persistentListOf(),
        val nextUnreadChapter: NextUnreadChapter = NextUnreadChapter(),
        val chapterFilter: ChapterDisplay = ChapterDisplay(),
        val chapterFilterText: String = "",
        val chapterSortFilter: SortFilter = SortFilter(),
        val chapterScanlatorFilter: ScanlatorFilter = ScanlatorFilter(persistentListOf()),
        val chapterSourceFilter: ScanlatorFilter = ScanlatorFilter(persistentListOf()),
        val chapterLanguageFilter: LanguageFilter = LanguageFilter(persistentListOf()),
        val allScanlators: ImmutableSet<String> = persistentSetOf(),
        val allUploaders: ImmutableSet<String> = persistentSetOf(),
        val allSources: ImmutableSet<String> = persistentSetOf(),
        val allLanguages: ImmutableSet<String> = persistentSetOf(),
    )

    @Immutable
    data class MangaScreenTrackState(
        val tracks: PersistentList<TrackItem> = persistentListOf(),
        val loggedInTrackService: PersistentList<TrackServiceItem> = persistentListOf(),
        val trackServiceCount: Int = 0,
        val trackingSuggestedDates: TrackingConstants.TrackingSuggestedDates? = null,
        val trackSearchResult: TrackingConstants.TrackSearchResult =
            TrackingConstants.TrackSearchResult.Loading,
    )

    @Immutable
    data class MangaScreenCategoryState(
        val allCategories: PersistentList<CategoryItem> = persistentListOf(),
        val currentCategories: PersistentList<CategoryItem> = persistentListOf(),
    )

    @Immutable
    data class MangaScreenMergeState(
        val validMergeTypes: PersistentList<MergeType> = persistentListOf(),
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
    data class ScanlatorFilter(val scanlators: PersistentList<ScanlatorOption> = persistentListOf())

    @Immutable data class ScanlatorOption(val name: String, val disabled: Boolean = false)

    @Immutable
    data class LanguageFilter(val languages: PersistentList<LanguageOption> = persistentListOf())

    @Immutable
    data class LanguageOption(val name: String, val disabled: Boolean = false)

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
        val all: PersistentList<CategoryItem>,
        val current: PersistentList<CategoryItem>,
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

    class CategoryActions(
        val set: (List<CategoryItem>) -> Unit = {},
        val addNew: (String) -> Unit = {},
    )

    class DescriptionActions(
        val genreSearch: (String) -> Unit,
        val genreSearchLibrary: (String) -> Unit,
        val altTitleClick: (String) -> Unit,
        val altTitleResetClick: () -> Unit,
    )

    class InformationActions(
        val titleLongClick: (String) -> Unit,
        val creatorCopy: (String) -> Unit,
        val creatorSearch: (String) -> Unit,
    )

    class TrackActions(
        val statusChange: (Int, TrackingConstants.TrackAndService) -> Unit,
        val scoreChange: (Int, TrackingConstants.TrackAndService) -> Unit,
        val chapterChange: (Int, TrackingConstants.TrackAndService) -> Unit,
        val dateChange: (TrackingConstants.TrackDateChange) -> Unit,
        val search: (String, TrackServiceItem) -> Unit,
        val searchItemClick: (TrackingConstants.TrackAndService) -> Unit,
        val remove: (Boolean, TrackServiceItem) -> Unit,
    )

    class CoverActions(
        val share: (Context, Artwork) -> Unit,
        val set: (Artwork) -> Unit,
        val save: (Artwork) -> Unit,
        val reset: () -> Unit,
    )

    class MergeActions(
        val remove: (MergeType) -> Unit,
        val search: (String, MergeType) -> Unit,
        val add: (SourceMergeManga) -> Unit,
    )

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
