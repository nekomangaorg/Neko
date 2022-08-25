package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.track.TrackService
import kotlinx.collections.immutable.ImmutableList
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.MergeManga

object MangaConstants {

    data class MangaScreenState(
        val alternativeArtwork: ImmutableList<Artwork>,
        val currentArtwork: Artwork,
        val currentDescription: String,
        val currentTitle: String,
        val hasDefaultCategory: Boolean,
        val hideButtonText: Boolean,
        val trackServiceCount: Int,
        val vibrantColor: Int?,
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
        val scanlators: List<ScanlatorOption>,
    )

    data class ScanlatorOption(
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
        Bookmarked
    }

    enum class SortType {
        SourceOrder,
        ChapterNumber,
        UploadDate
    }

    enum class SortState {
        Ascending,
        Descending,
        None
    }

    enum class SetGlobal {
        HideTitles,
        Sort,
        Filter
    }

    data class SnackbarState(
        val message: String? = null,
        @StringRes val messageRes: Int? = null,
        @StringRes val fieldRes: Int? = null,
        val actionLabel: String? = null,
        @StringRes val actionLabelRes: Int? = null,
        val action: (() -> Unit)? = null,
        val dismissAction: (() -> Unit)? = null,
    )

    class ChapterFilterActions(
        val changeSort: (sortOptions: SortOption?) -> Unit,
        val changeFilter: (filterOption: FilterOption?) -> Unit,
        val changeScanlator: (scanlatorOption: ScanlatorOption?) -> Unit,
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

    data class DownloadActionHolder(val chapters: List<ChapterItem>, val downloadAction: DownloadAction)

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
        val set: (List<Category>) -> Unit = {},
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
        val search: (String, TrackService) -> Unit,
        val searchItemClick: (TrackingConstants.TrackAndService) -> Unit,
        val remove: (Boolean, TrackService) -> Unit,
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
        val openNext: (Context) -> Unit,
    )
}
