package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import org.nekomanga.constants.Constants

@Entity(
    tableName = "chapters",
    foreignKeys =
        [
            ForeignKey(
                entity = MangaEntity::class,
                parentColumns = ["_id"],
                childColumns = ["manga_id"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index(value = ["manga_id"])],
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long? = null,
    @ColumnInfo(name = "manga_id") val mangaId: Long,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "chapter_txt") val chapterTxt: String,
    @ColumnInfo(name = "chapter_title") val chapterTitle: String,
    @ColumnInfo(name = "vol") val vol: String,
    @ColumnInfo(name = "scanlator") val scanlator: String?,
    @ColumnInfo(name = "uploader") val uploader: String?,
    @ColumnInfo(name = "unavailable") val isUnavailable: Boolean,
    @ColumnInfo(name = "read") val read: Boolean,
    @ColumnInfo(name = "bookmark") val bookmark: Boolean,
    @ColumnInfo(name = "last_page_read") val lastPageRead: Int,
    @ColumnInfo(name = "pages_left") val pagesLeft: Int,
    @ColumnInfo(name = "chapter_number") val chapterNumber: Float,
    @ColumnInfo(name = "source_order") val sourceOrder: Int,
    @ColumnInfo(name = "smart_order") val smartOrder: Int,
    @ColumnInfo(name = "date_fetch") val dateFetch: Long,
    @ColumnInfo(name = "date_upload") val dateUpload: Long,
    @ColumnInfo(name = "mangadex_chapter_id") val mangadexChapterId: String?,
    @ColumnInfo(name = "old_mangadex_chapter_id") val oldMangadexId: String?,
    @ColumnInfo(name = "language") val language: String?,
)

fun ChapterEntity.canDeleteChapter() =
    !this.isLocalSource() && !this.bookmark && !this.isUnavailable

fun ChapterEntity.isLocalSource() =
    this.scanlator?.equals(Constants.LOCAL_SOURCE) == true && this.isUnavailable

fun ChapterEntity.isMergedChapter() = MergeType.containsMergeSourceName(this.scanlator)

fun ChapterEntity.scanlatorList(): List<String> {
    this.scanlator ?: return emptyList()
    return ChapterUtil.getScanlators(this.scanlator)
}

fun ChapterEntity.uuid(): String {
    return MdUtil.getChapterUUID(this.url)
}

fun ChapterEntity.isAvailable(downloadManager: DownloadManager, manga: Manga): Boolean {
    return !this.isUnavailable ||
        this.isLocalSource() ||
        downloadManager.isChapterDownloaded(this, manga)
}
