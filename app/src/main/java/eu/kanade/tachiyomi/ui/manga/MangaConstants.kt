package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.external.ExternalLink
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.ChapterMarkActions
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.Stats
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem

object MangaConstants {

    data class MangaDetailScreenState(
        val isRefreshing: Boolean = false,
        val isSearching: Boolean = false,
        val activeChapters: PersistentList<ChapterItem> = persistentListOf(),
        val searchChapters: PersistentList<ChapterItem> = persistentListOf(),
        val allCategories: PersistentList<CategoryItem> = persistentListOf(),
        val allChapters: PersistentList<ChapterItem> = persistentListOf(),
        val allScanlators: ImmutableSet<String> = persistentSetOf(),
        val allUploaders: ImmutableSet<String> = persistentSetOf(),
        val allSources: ImmutableSet<String> = persistentSetOf(),
        val allLanguages: ImmutableSet<String> = persistentSetOf(),
        val validMergeTypes: PersistentList<MergeType> = persistentListOf(),
        val chapterFilter: ChapterDisplay = ChapterDisplay(),
        val chapterFilterText: String = "",
        val chapterSortFilter: SortFilter = SortFilter(),
        val chapterScanlatorFilter: ScanlatorFilter = ScanlatorFilter(persistentListOf()),
        val chapterSourceFilter: ScanlatorFilter = ScanlatorFilter(persistentListOf()),
        val chapterLanguageFilter: LanguageFilter = LanguageFilter(persistentListOf()),
        val currentCategories: PersistentList<CategoryItem> = persistentListOf(),
        val hasDefaultCategory: Boolean = false,
        val hideButtonText: Boolean = false,
        val backdropSize: BackdropSize = BackdropSize.Default,
        val forcePortrait: Boolean = false,
        val incognitoMode: Boolean = false,
        val nextUnreadChapter: NextUnreadChapter = NextUnreadChapter(),
        val removedChapters: PersistentList<ChapterItem> = persistentListOf(),
        val themeBasedOffCovers: Boolean = false,
        val wrapAltTitles: Boolean = false,
        val trackServiceCount: Int = 0,
        val trackingSuggestedDates: TrackingConstants.TrackingSuggestedDates? = null,
        val vibrantColor: Int? = null,
        val alternativeArtwork: PersistentList<Artwork> = persistentListOf(),
        val alternativeTitles: PersistentList<String> = persistentListOf(),
        val artist: String = "",
        val author: String = "",
        val currentArtwork: Artwork,
        val currentDescription: String = "",
        val currentTitle: String = "",
        val externalLinks: PersistentList<ExternalLink> = persistentListOf(),
        val genres: PersistentList<String> = persistentListOf(),
        val initialized: Boolean = false,
        val inLibrary: Boolean = false,
        val isMerged: MergeConstants.IsMergedManga = MergeConstants.IsMergedManga.No,
        val isPornographic: Boolean = false,
        val langFlag: String? = null,
        val missingChapters: String = "",
        val estimatedMissingChapters: String = "",
        val originalTitle: String = "",
        val stats: Stats? = null,
        val status: Int = 0,
        val lastVolume: Int? = null,
        val lastChapter: Int? = null,
        val loggedInTrackService: PersistentList<TrackServiceItem> = persistentListOf(),
        val tracks: PersistentList<TrackItem> = persistentListOf(),
        val trackSearchResult: TrackingConstants.TrackSearchResult =
            TrackingConstants.TrackSearchResult.Loading,
        val mergeSearchResult: MergeConstants.MergeSearchResult =
            MergeConstants.MergeSearchResult.Loading,
    )

    /** Holds the next unread chapter and the text to display for the quick read button. */
    data class NextUnreadChapter(
        @param:StringRes val id: Int? = null,
        val text: String = "",
        val simpleChapter: SimpleChapter? = null,
    )

    data class SortFilter(
        val sourceOrderSort: SortState = SortState.None,
        val smartOrderSort: SortState = SortState.None,
        val uploadDateSort: SortState = SortState.None,
        val matchesGlobalDefaults: Boolean = true,
    )

    data class SortOption(val sortState: SortState, val sortType: SortType)

    data class ScanlatorFilter(
        val scanlators: PersistentList<ScanlatorOption> = persistentListOf()
    )

    data class ScanlatorOption(val name: String, val disabled: Boolean = false)

    data class LanguageFilter(val languages: PersistentList<LanguageOption> = persistentListOf())

    data class LanguageOption(val name: String, val disabled: Boolean = false)

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
        val mark: (List<ChapterItem>, ChapterMarkActions) -> Unit,
        val clearRemoved: () -> Unit,
        val download: (List<ChapterItem>, DownloadAction) -> Unit,
        val delete: (List<ChapterItem>) -> Unit,
        val open: (ChapterItem) -> Unit,
        val blockScanlator: (BlockType, String) -> Unit,
        val openNext: () -> Unit,
        val openComment: (String) -> Unit,
        val openInBrowser: (ChapterItem) -> Unit,
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
