package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import eu.kanade.tachiyomi.source.SourceManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class BackupManga(
    // in 1.x some of these values have different names
    @ProtoNumber(1) var source: Long,
    // url is called key in 1.x
    @ProtoNumber(2) var url: String,
    @ProtoNumber(3) var title: String = "",
    @ProtoNumber(4) var artist: String? = null,
    @ProtoNumber(5) var author: String? = null,
    @ProtoNumber(6) var description: String? = null,
    @ProtoNumber(7) var genre: List<String> = emptyList(),
    @ProtoNumber(8) var status: Int = 0,
    // thumbnailUrl is called cover in 1.x
    @ProtoNumber(9) var thumbnailUrl: String? = null,
    // @ProtoNumber(10) val customCover: String = "", 1.x value, not used in 0.x
    // @ProtoNumber(11) val lastUpdate: Long = 0, 1.x value, not used in 0.x
    // @ProtoNumber(12) val lastInit: Long = 0, 1.x value, not used in 0.x
    @ProtoNumber(13) var dateAdded: Long = 0,
    @ProtoNumber(14) var viewer: Int = 0, // Replaced by viewer_flags
    // @ProtoNumber(15) val flags: Int = 0, 1.x value, not used in 0.x
    @ProtoNumber(16) var chapters: List<BackupChapter> = emptyList(),
    @ProtoNumber(17) var categories: List<Int> = emptyList(),
    @ProtoNumber(18) var tracking: List<BackupTracking> = emptyList(),
    // Bump by 100 for values that are not saved/implemented in 1.x but are used in 0.x
    @ProtoNumber(100) var favorite: Boolean = true,
    @ProtoNumber(101) var chapterFlags: Int = 0,
    @ProtoNumber(102) var brokenHistory: List<BrokenBackupHistory> = emptyList(),
    @ProtoNumber(103) var viewer_flags: Int? = null,
    @ProtoNumber(104) var history: List<BackupHistory> = emptyList(),

    // J2K specific values
    @ProtoNumber(800) var customTitle: String? = null,

    // Neko Values
    @Deprecated("Use mergeMangaList") @ProtoNumber(900) var mergedMangaUrl: String? = null,
    @ProtoNumber(901) var scanlatorFilter: String? = null,
    @Deprecated("Use mergeMangaList") @ProtoNumber(902) var mergedMangaImageUrl: String? = null,
    @ProtoNumber(903) var alternativeArtwork: String? = null,
    @ProtoNumber(904) var mergeMangaList: List<BackupMergeManga> = emptyList(),
) {
    fun getMangaImpl(): MangaImpl {
        return MangaImpl().apply {
            url =
                this@BackupManga.url.replace(
                    "/manga/",
                    "/title/",
                ) // tachiyomi extension uses /manga/
            title = this@BackupManga.title
            artist = this@BackupManga.artist
            author = this@BackupManga.author
            description = this@BackupManga.description
            genre = this@BackupManga.genre.joinToString()
            status = this@BackupManga.status
            thumbnail_url = this@BackupManga.thumbnailUrl
            favorite = this@BackupManga.favorite
            source = this@BackupManga.source
            date_added = this@BackupManga.dateAdded
            merge_manga_url = this@BackupManga.mergedMangaUrl
            viewer_flags =
                (this@BackupManga.viewer_flags ?: this@BackupManga.viewer).takeIf { it != 0 } ?: -1
            chapter_flags = this@BackupManga.chapterFlags
            filtered_scanlators = this@BackupManga.scanlatorFilter
            user_title = this@BackupManga.customTitle
            user_cover = this@BackupManga.alternativeArtwork
        }
    }

    fun getMergeMangaImpl(): List<MergeMangaImpl> {
        return mergeMangaList.mapNotNull { it.toMergeMangaImpl() }
    }

    fun getChaptersImpl(): List<ChapterImpl> {
        return chapters.map { it.toChapterImpl() }
    }

    fun getTrackingImpl(): List<TrackImpl> {
        return tracking.map { it.getTrackingImpl() }
    }

    companion object {
        fun copyFrom(manga: Manga): BackupManga {
            return BackupManga(
                url = manga.url.replace("/title/", "/manga/"), // tachiyomi extension uses /manga/
                title = manga.title,
                artist = manga.artist,
                author = manga.author,
                description = manga.description,
                genre = manga.getGenres() ?: emptyList(),
                status = manga.status,
                thumbnailUrl = manga.thumbnail_url,
                favorite = manga.favorite,
                source = manga.source,
                dateAdded = manga.date_added,
                viewer = manga.readingModeType,
                viewer_flags = manga.viewer_flags.takeIf { it != -1 } ?: 0,
                chapterFlags = manga.chapter_flags,
                scanlatorFilter = manga.filtered_scanlators,
                customTitle = manga.user_title,
                alternativeArtwork = manga.user_cover,
            )
        }
    }
}

/**
 * A backup can list one title on several rows, and restore writes them as one manga. The first row
 * with a source gives the details, since a row that lost its source can have lost them too. Each
 * user setting comes from the first row that has it, and the lists come from every row. The title
 * stays in the library if any row is a favorite.
 */
internal fun List<BackupManga>.combineRows(): BackupManga {
    if (size == 1) return first()
    val base = firstOrNull { it.source != SourceManager.UNSET_SOURCE } ?: first()
    return base.copy(
        dateAdded = combineDateAdded(),
        customTitle = firstNotBlank { it.customTitle },
        scanlatorFilter = firstNotBlank { it.scanlatorFilter },
        alternativeArtwork = firstNotBlank { it.alternativeArtwork },
        viewer_flags =
            map { it.viewer_flags ?: it.viewer }.firstOrNull { it != 0 } ?: base.viewer_flags,
        chapterFlags = map { it.chapterFlags }.firstOrNull { it != 0 } ?: 0,
        favorite = any { it.favorite },
        categories = flatMap { it.categories }.distinct(),
        chapters = flatMap { it.chapters }.groupBy { it.url }.values.map { it.combineProgress() },
        history = flatMap { it.history }.groupBy { it.url }.values.map { it.combineReads() },
        tracking =
            flatMap { it.tracking }.groupBy { it.syncId }.values.map { it.combineTracking(base) },
        mergeMangaList =
            flatMap { it.mergeMangaList }
                .distinctBy { merge -> MergeType.getById(merge.mergeType).mergeKey(merge.url) },
    )
}

private inline fun List<BackupManga>.firstNotBlank(field: (BackupManga) -> String?): String? =
    firstNotNullOfOrNull { row ->
        field(row)?.takeIf { it.isNotBlank() }
    }

/**
 * The earliest date on any row. A row that lost its source has date_added 0, and the copy Pull
 * inserted next to it holds the time of that Pull. Using that time would hide every chapter fetched
 * before it from the update queries (date_fetch > date_added), so when a row has no date the
 * chapter fetch times count too.
 */
private fun List<BackupManga>.combineDateAdded(): Long {
    val dates = map { it.dateAdded }
    val known = if (0L in dates) dates + flatMap { it.chapters }.map { it.dateFetch } else dates
    return known.filter { it > 0L }.minOrNull() ?: 0L
}

/** Restore inserts every chapter it is given, so the rows of one chapter url become one chapter. */
private fun List<BackupChapter>.combineProgress(): BackupChapter {
    val isRead = any { it.read }
    val furthest = maxBy { it.lastPageRead }
    return first()
        .copy(
            read = isRead,
            bookmark = any { it.bookmark },
            lastPageRead = furthest.lastPageRead,
            pagesLeft = if (isRead) 0 else furthest.pagesLeft,
        )
}

/**
 * A chapter holds one history entry, so the entries of one chapter become one with the latest read
 * and the longest reading time.
 */
private fun List<BackupHistory>.combineReads(): BackupHistory =
    first().copy(lastRead = maxOf { it.lastRead }, readDuration = maxOf { it.readDuration })

/**
 * A manga holds one track per service, so the tracks of one service become one: the track of the
 * row that gives the details, or the first one when that row has none, with the furthest chapter
 * any row reached.
 */
private fun List<BackupTracking>.combineTracking(base: BackupManga): BackupTracking {
    val service = first().syncId
    val details = base.tracking.firstOrNull { it.syncId == service } ?: first()
    return details.copy(lastChapterRead = maxOf { it.lastChapterRead })
}
