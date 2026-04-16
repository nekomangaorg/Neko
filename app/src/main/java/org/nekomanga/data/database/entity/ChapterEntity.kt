package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices =
        [
            Index(value = ["manga_id"], name = "chapters_manga_id_index"),
            Index(value = ["manga_id", "read"], name = "chapters_unread_by_manga_index"),
        ],
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0L,
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
