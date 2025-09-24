package org.nekomanga.presentation.screens.mangadetails

import androidx.compose.ui.state.ToggleableState
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.SourceMergeManga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MergedServerSource
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.manga.MergeConstants
import eu.kanade.tachiyomi.ui.manga.TrackingConstants
import eu.kanade.tachiyomi.util.chapter.ChapterItemSort
import eu.kanade.tachiyomi.util.getMissingChapters
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.nekomanga.R
import org.nekomanga.constants.Constants
import org.nekomanga.constants.MdConstants
import org.nekomanga.domain.category.CategoryItem
import org.nekomanga.domain.chapter.ChapterItem
import org.nekomanga.domain.chapter.SimpleChapter
import org.nekomanga.domain.chapter.toSimpleChapter
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.domain.manga.Stats
import org.nekomanga.domain.track.TrackItem
import org.nekomanga.domain.track.TrackServiceItem
import org.nekomanga.domain.track.toTrackItem
import eu.kanade.tachiyomi.data.database.models.Category as DbCategory

data class MangaDetailsState(
    val manga: Manga? = null,
    // Manga Information
    val artwork: ImmutableList<Artwork> = persistentListOf(),
    val alternativeTitles: ImmutableList<String> = persistentListOf(),
    val artist: String = "",
    val author: String = "",
    val currentArtwork: Artwork,
    val description: String = "",
    val title: String = "",
    val externalLinks: ImmutableList<ExternalLink> = persistentListOf(),
    val genres: ImmutableList<String> = persistentListOf(),
    val initialized: Boolean = false,
    val inLibrary: Boolean = false,
    val isMerged: MergeConstants.IsMergedManga = MergeConstants.IsMergedManga.No,
    val isPornographic: Boolean = false,
    val langFlag: String? = null,
    val missingChapters: String? = null,
    val estimatedMissingChapters: String? = null,
    val originalTitle: String = "",
    val stats: Stats? = null,
    val status: Int = 0,
    val lastVolume: Int? = null,
    val lastChapter: Int? = null,

    // Chapters
    val chapters: ImmutableList<ChapterItem> = persistentListOf(),
    val searchChapters: ImmutableList<ChapterItem> = persistentListOf(),
    val chapterFilter: ChapterDisplay = ChapterDisplay(),
    val chapterFilterText: String = "",
    val chapterSortFilter: SortFilter = SortFilter(),
    val chapterScanlatorFilter: ScanlatorFilter = ScanlatorFilter(persistentListOf()),
    val chapterSourceFilter: ScanlatorFilter = ScanlatorFilter(persistentListOf()),
    val chapterLanguageFilter: LanguageFilter = LanguageFilter(persistentListOf()),
    val nextUnreadChapter: NextUnreadChapter = NextUnreadChapter(),
    val removedChapters: ImmutableList<ChapterItem> = persistentListOf(),

    // Tracking and Merging
    val loggedInTrackService: ImmutableList<TrackServiceItem> = persistentListOf(),
    val tracks: ImmutableList<TrackItem> = persistentListOf(),
    val trackSearchResult: TrackingConstants.TrackSearchResult = TrackingConstants.TrackSearchResult.Loading,
    val mergeSearchResult: MergeConstants.MergeSearchResult = MergeConstants.MergeSearchResult.Loading,
    val trackServiceCount: Int = 0,
    val trackingSuggestedDates: TrackingConstants.TrackingSuggestedDates? = null,

    // Categories
    val allCategories: ImmutableList<CategoryItem> = persistentListOf(),
    val currentCategories: ImmutableList<CategoryItem> = persistentListOf(),
    val hasDefaultCategory: Boolean = false,

    // Other UI state
    val allScanlators: ImmutableSet<String> = persistentSetOf(),
    val allUploaders: ImmutableSet<String> = persistentSetOf(),
    val allSources: ImmutableSet<String> = persistentSetOf(),
    val allLanguages: ImmutableSet<String> = persistentSetOf(),
    val validMergeTypes: ImmutableList<MergeType> = persistentListOf(),
    val hideButtonText: Boolean = false,
    val extraLargeBackdrop: Boolean = false,
    val forcePortrait: Boolean = false,
    val themeBasedOffCovers: Boolean = false,
    val wrapAltTitles: Boolean = false,
    val vibrantColor: Int? = null,
) {
    constructor(
        manga: Manga,
        chapters: List<Chapter>,
        artwork: List<Artwork>,
        tracks: List<Track>,
        mangaCategories: List<DbCategory>,
        allCategories: List<DbCategory>,
        mergeManga: List<SourceMergeManga>,
        hideButtonText: Boolean,
        extraLargeBackdrop: Boolean,
        forcePortrait: Boolean,
        themeByCover: Boolean,
        wrapAltTitles: Boolean,
        coverQuality: Int,
        blockedGroups: Set<String>,
        blockedUploaders: Set<String>,
        vibrantColor: Int?,
        sourceManager: SourceManager,
        downloadManager: DownloadManager,
        chapterItemSort: ChapterItemSort,
    ) : this(
        manga = manga,
        artwork = artwork.map { aw ->
            Artwork(
                mangaId = aw.mangaId,
                url = MdUtil.cdnCoverUrl(manga.uuid(), aw.fileName, coverQuality),
                volume = aw.volume,
                description = aw.description,
                active = manga.user_cover?.contains(aw.fileName) ?: false || (manga.user_cover.isNullOrBlank() && manga.thumbnail_url?.contains(aw.fileName) ?: false),
            )
        }.toImmutableList(),
        alternativeTitles = manga.getAltTitles().toImmutableList(),
        artist = manga.artist ?: "",
        author = manga.author ?: "",
        currentArtwork = Artwork(
            url = manga.user_cover ?: "",
            inLibrary = manga.favorite,
            originalArtwork = manga.thumbnail_url ?: "",
            mangaId = manga.id!!
        ),
        description = manga.description ?: "",
        title = manga.title,
        externalLinks = manga.getExternalLinks().toImmutableList(),
        genres = (manga.getGenres(true) ?: emptyList()).toImmutableList(),
        initialized = manga.initialized,
        inLibrary = manga.favorite,
        isMerged = when (mergeManga.isNotEmpty()) {
            true -> {
                val mergeManga = mergeManga.first()
                val source = MergeType.getSource(mergeManga.mergeType, sourceManager)
                val url =
                    when (source) {
                        is MergedServerSource -> source.getMangaUrl(mergeManga.url)
                        else -> source.baseUrl + mergeManga.url
                    }
                MergeConstants.IsMergedManga.Yes(url, title = mergeManga.title, mergeType = mergeManga.mergeType)
            }
            false -> MergeConstants.IsMergedManga.No
        },
        isPornographic = manga.getContentRating()?.equals(MdConstants.ContentRating.pornographic, ignoreCase = true) ?: false,
        langFlag = manga.lang_flag,
        missingChapters = manga.missing_chapters,
        estimatedMissingChapters = chapters.getMissingChapters().estimatedChapters,
        originalTitle = manga.originalTitle,
        stats = Stats(
            rating = manga.rating,
            follows = manga.users,
            threadId = manga.thread_id,
            repliesCount = manga.replies_count,
        ),
        status = manga.status,
        lastVolume = manga.last_volume_number,
        lastChapter = manga.last_chapter_number,
        chapters = chapters.mapNotNull { it.toSimpleChapter() }.filter {
            val scanlators = it.scanlatorList()
            scanlators.none { scanlator -> blockedGroups.contains(scanlator) } && (Constants.NO_GROUP !in scanlators || it.uploader !in blockedUploaders)
        }.map { chapter ->
            val download = downloadManager.getQueuedDownloadOrNull(chapter.id)
            ChapterItem(
                chapter = chapter,
                downloadState = when {
                    downloadManager.isChapterDownloaded(chapter.toDbChapter(), manga) -> Download.State.DOWNLOADED
                    download != null -> download.status
                    else -> Download.State.NOT_DOWNLOADED
                },
                downloadProgress = download?.progress ?: 0,
            )
        }.toImmutableList(),
        nextUnreadChapter = chapterItemSort.getNextUnreadChapter(manga, chapters.mapNotNull { it.toSimpleChapter() }.map { ChapterItem(it) })?.let {
            val text = if (it.chapter.isMergedChapter() || (it.chapter.volume.isEmpty() && it.chapter.chapterText.isEmpty())) {
                it.chapter.name
            } else if (it.chapter.volume.isNotEmpty()) {
                "Vol. ${it.chapter.volume} ${it.chapter.chapterText}"
            } else {
                it.chapter.chapterText
            }
            NextUnreadChapter(
                id = when (it.chapter.lastPageRead > 0) {
                    true -> R.string.continue_reading_
                    false -> R.string.start_reading_
                },
                text = text,
                simpleChapter = it.chapter
            )
        } ?: NextUnreadChapter(),
        tracks = tracks.map { it.toTrackItem() }.toImmutableList(),
        hideButtonText = hideButtonText,
        extraLargeBackdrop = extraLargeBackdrop,
        forcePortrait = forcePortrait,
        themeBasedOffCovers = themeByCover,
        wrapAltTitles = wrapAltTitles,
        vibrantColor = vibrantColor,
    )
}


/**
 * Holds the next unread chapter and the text to display for the quick read button.
 */
data class NextUnreadChapter(
    val id: Int? = null,
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

data class ScanlatorFilter(val scanlators: ImmutableList<ScanlatorOption>)

data class ScanlatorOption(val name: String, val disabled: Boolean = false)

data class LanguageFilter(val languages: ImmutableList<LanguageOption>)

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

enum class BlockType {
    Group,
    Uploader,
}
