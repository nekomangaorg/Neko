package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.track.TrackService
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
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
    )

    data class SortOption(
        val sortState: SortState,
        val sortType: SortType,
    )

    data class Filter(
        val showAll: Boolean = false,
        val unread: ToggleableState = ToggleableState.Off,
        val downloaded: ToggleableState = ToggleableState.Off,
        val bookmarked: ToggleableState = ToggleableState.Off,
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

    class ChapterFilterActions(
        val changeSort: (sortOptions: SortOption) -> Unit,
        val changeFilter: (filterOption: FilterOption) -> Unit,
    )

    sealed class DownloadAction {
        object Download : DownloadAction()
        object Remove : DownloadAction()
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
        val share: (Context, String) -> Unit,
        val set: (String) -> Unit,
        val save: (String) -> Unit,
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
