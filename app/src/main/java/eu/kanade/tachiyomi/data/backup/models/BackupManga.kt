package eu.kanade.tachiyomi.data.backup.models

import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.TrackImpl
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
        return mergeMangaList.map { it.toMergeMangaImpl() }
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
