package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.track.TrackService
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.MergeManga

object MangaConstants {

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
        Sort,
        Filter
    }

    class ChapterFilterActions(
        val changeSort: (sortOptions: SortOption?) -> Unit,
        val changeFilter: (filterOption: FilterOption?) -> Unit,
        val changeScanlator: (scanlatorOption: ScanlatorOption?) -> Unit,
        val setAsGlobal: (SetGlobal) -> Unit,
    )

    sealed class DownloadAction {
        data class DownloadNextUnread(val numberToDownload: Int) : DownloadAction()
        object DownloadAll : DownloadAction()
        object DownloadUnread : DownloadAction()
        object Download : DownloadAction()
        object Remove : DownloadAction()
        object RemoveRead : DownloadAction()
        object RemoveAll : DownloadAction()
        object Cancel : DownloadAction()
    }

    data class DownloadActionHolder(val chapters: List<ChapterItem>, val downloadAction: DownloadAction)

    sealed class MarkAction {
        class Bookmark(chapterItems: List<ChapterItem>) : MarkAction()
        class Read(chapterItems: List<ChapterItem>) : MarkAction()
        class Unread(chapterItems: List<ChapterItem>) : MarkAction()
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
        val bookmark: (ChapterItem) -> Unit,
        val clearRemoved: () -> Unit,
        val download: (List<ChapterItem>, DownloadAction) -> Unit,
        val delete: (List<ChapterItem>) -> Unit,
        val markRead: (List<ChapterItem>, Boolean) -> Unit,
        val open: (Context, ChapterItem) -> Unit,
        val openNext: (Context) -> Unit,
    )
}
