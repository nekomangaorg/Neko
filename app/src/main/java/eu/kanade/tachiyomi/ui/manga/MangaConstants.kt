package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.external.ExternalLink
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.MergeManga
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem

object MangaConstants {

    data class MangaScreenGeneralState(
        val activeChapters: ImmutableList<ChapterItem> = persistentListOf(),
        val allCategories: ImmutableList<CategoryItem> = persistentListOf(),
        val allChapters: ImmutableList<ChapterItem> = persistentListOf(),
        val allScanlators: ImmutableSet<String> = persistentSetOf(),
        val allLanguages: ImmutableSet<String> = persistentSetOf(),
        val chapterFilter: Filter = Filter(),
        val chapterFilterText: String = "",
        val chapterSortFilter: SortFilter = SortFilter(),
        val chapterScanlatorFilter: ScanlatorFilter = ScanlatorFilter(persistentListOf()),
        val chapterLanguageFilter: LanguageFilter = LanguageFilter(persistentListOf()),
        val currentCategories: ImmutableList<CategoryItem> = persistentListOf(),
        val hasDefaultCategory: Boolean = false,
        val hideButtonText: Boolean = false,
        val extraLargeBackdrop: Boolean = false,
        val hideChapterTitles: Boolean = false,
        val nextUnreadChapter: NextUnreadChapter = NextUnreadChapter(),
        val removedChapters: ImmutableList<ChapterItem> = persistentListOf(),
        val themeBasedOffCovers: Boolean = false,
        val trackServiceCount: Int = 0,
        val trackingSuggestedDates: TrackingConstants.TrackingSuggestedDates? = null,
        val vibrantColor: Int? = null,
    )

    data class MangaScreenMangaState(
        val alternativeArtwork: ImmutableList<Artwork> = persistentListOf(),
        val alternativeTitles: ImmutableList<String> = persistentListOf(),
        val artist: String = "",
        val author: String = "",
        val currentArtwork: Artwork,
        val currentDescription: String = "",
        val currentTitle: String = "",
        val externalLinks: ImmutableList<ExternalLink> = persistentListOf(),
        val genres: ImmutableList<String> = persistentListOf(),
        val initialized: Boolean = false,
        val inLibrary: Boolean = false,
        val isMerged: MergeConstants.IsMergedManga = MergeConstants.IsMergedManga.No,
        val isPornographic: Boolean = false,
        val langFlag: String? = null,
        val missingChapters: String? = null,
        val originalTitle: String = "",
        val rating: String? = null,
        val status: Int = 0,
        val users: String? = null,
    )

    data class MangaScreenTrackMergeState(
        val loggedInTrackService: ImmutableList<TrackServiceItem> = persistentListOf(),
        val tracks: ImmutableList<TrackItem> = persistentListOf(),
        val trackSearchResult: TrackingConstants.TrackSearchResult = TrackingConstants.TrackSearchResult.Loading,
        val mergeSearchResult: MergeConstants.MergeSearchResult = MergeConstants.MergeSearchResult.Loading,
    )

    /**
     * Holds the next unread chapter and the text to display for the quick read button.
     */
    data class NextUnreadChapter(@StringRes val id: Int? = null, val text: String = "", val simpleChapter: SimpleChapter? = null)

    data class SortFilter(
        val sourceOrderSort: SortState = SortState.None,
        val chapterNumberSort: SortState = SortState.None,
        val uploadDateSort: SortState = SortState.None,
        val matchesGlobalDefaults: Boolean = true,
    )

    data class SortOption(
        val sortState: SortState,
        val sortType: SortType,
    )

    data class ScanlatorFilter(
        val scanlators: ImmutableList<ScanlatorOption>,
    )

    data class ScanlatorOption(
        val name: String,
        val disabled: Boolean = false,
    )

    data class LanguageFilter(
        val languages: ImmutableList<LanguageOption>,
    )

    data class LanguageOption(
        val name: String,
        val disabled: Boolean = false,
    )

    data class Filter(
        val showAll: Boolean = false,
        val unread: ToggleableState = ToggleableState.Off,
        val downloaded: ToggleableState = ToggleableState.Off,
        val bookmarked: ToggleableState = ToggleableState.Off,
        val matchesGlobalDefaults: Boolean = true,
    )

    data class FilterOption(
        val filterType: FilterType,
        val filterState: ToggleableState,
    )

    enum class FilterType {
        All,
        Unread,
        Downloaded,
        Bookmarked,
    }

    enum class SortType {
        SourceOrder,
        ChapterNumber,
        UploadDate,
    }

    enum class SortState {
        Ascending,
        Descending,
        None,
    }

    enum class SetGlobal {
        HideTitles,
        Sort,
        Filter,
    }

    class ChapterFilterActions(
        val changeSort: (sortOptions: SortOption?) -> Unit,
        val changeFilter: (filterOption: FilterOption?) -> Unit,
        val changeScanlator: (scanlatorOption: ScanlatorOption?) -> Unit,
        val changeLanguage: (languageOption: LanguageOption?) -> Unit,
        val hideTitles: (Boolean) -> Unit,
        val setAsGlobal: (SetGlobal) -> Unit,
    )

    sealed class DownloadAction {
        data class DownloadNextUnread(val numberToDownload: Int) : DownloadAction()

        object DownloadAll : DownloadAction()
        object DownloadUnread : DownloadAction()
        object Download : DownloadAction()
        object ImmediateDownload : DownloadAction()
        object Remove : DownloadAction()
        object RemoveRead : DownloadAction()
        object RemoveAll : DownloadAction()
        object Cancel : DownloadAction()
    }

    sealed class MarkAction {
        abstract val canUndo: Boolean

        data class Bookmark(override val canUndo: Boolean = false) : MarkAction()
        data class UnBookmark(override val canUndo: Boolean = false) : MarkAction()
        data class PreviousRead(override val canUndo: Boolean, val altChapters: List<ChapterItem>) : MarkAction()
        data class PreviousUnread(override val canUndo: Boolean, val altChapters: List<ChapterItem>) : MarkAction()
        data class Read(override val canUndo: Boolean = false) : MarkAction()
        data class Unread(override val canUndo: Boolean = false, val lastRead: Int? = null, val pagesLeft: Int? = null) : MarkAction()
    }

    class CategoryActions(
        val set: (List<CategoryItem>) -> Unit = {},
        val addNew: (String) -> Unit = {},
    )

    class DescriptionActions(
        val genreClick: (String) -> Unit,
        val genreLongClick: (String) -> Unit,
        val altTitleClick: (String) -> Unit,
        val altTitleResetClick: () -> Unit,
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
        val remove: () -> Unit,
        val search: (String) -> Unit,
        val add: (MergeManga) -> Unit,
    )

    class ChapterActions(
        val mark: (List<ChapterItem>, MarkAction) -> Unit,
        val clearRemoved: () -> Unit,
        val download: (List<ChapterItem>, DownloadAction) -> Unit,
        val delete: (List<ChapterItem>) -> Unit,
        val open: (Context, ChapterItem) -> Unit,
        val blockScanlator: (String) -> Unit,
        val openNext: (Context) -> Unit,
    )
}
