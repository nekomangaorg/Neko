package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.external.ExternalLink
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
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

import org.nekomanga.presentation.screens.mangadetails.BlockType
import org.nekomanga.presentation.screens.mangadetails.ChapterDisplayOptions
import org.nekomanga.presentation.screens.mangadetails.LanguageOption
import org.nekomanga.presentation.screens.mangadetails.ScanlatorOption
import org.nekomanga.presentation.screens.mangadetails.SetGlobal
import org.nekomanga.presentation.screens.mangadetails.SortOption

object MangaConstants {

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
}
