package org.nekomanga.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import eu.kanade.tachiyomi.source.online.utils.FollowStatus

@Entity(tableName = "manga", indices = [Index(value = ["favorite"]), Index(value = ["url"])])
data class MangaEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "source") val source: Long,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "artist") val artist: String?,
    @ColumnInfo(name = "author") val author: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "genre") val genre: String?,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "status") val status: Int,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String?,
    @ColumnInfo(name = "favorite") val favorite: Boolean,
    @ColumnInfo(name = "last_update") val lastUpdate: Long?,
    @ColumnInfo(name = "next_update") val nextUpdate: Long?,
    @ColumnInfo(name = "date_added") val dateAdded: Long?,
    @ColumnInfo(name = "initialized") val initialized: Boolean,
    @ColumnInfo(name = "viewer") val viewerFlags: Int,
    @ColumnInfo(name = "chapter_flags") val chapterFlags: Int,
    @ColumnInfo(name = "lang_flag") val langFlag: String?,
    @ColumnInfo(name = "follow_status") val followStatus: FollowStatus,
    @ColumnInfo(name = "anilist_id") val anilistId: String?,
    @ColumnInfo(name = "kitsu_id") val kitsuId: String?,
    @ColumnInfo(name = "my_anime_list_id") val myAnimeListId: String?,
    @ColumnInfo(name = "manga_updates_id") val mangaUpdatesId: String?,
    @ColumnInfo(name = "anime_planet_id") val animePlanetId: String?,
    @ColumnInfo(name = "other_urls") val otherUrls: String?,
    @ColumnInfo(name = "scanlator_filter_flag") val filteredScanlators: String?,
    @ColumnInfo(name = "missing_chapters") val missingChapters: String?,
    @ColumnInfo(name = "rating") val rating: String?,
    @ColumnInfo(name = "users") val users: String?,
    @ColumnInfo(name = "thread_id") val threadId: String?,
    @ColumnInfo(name = "replies_count") val repliesCount: String?,
    @ColumnInfo(name = "merge_manga_url") val mergeMangaUrl: String?,
    @ColumnInfo(name = "manga_last_volume") val lastVolumeNumber: Int?,
    @ColumnInfo(name = "manga_last_chapter") val lastChapterNumber: Int?,
    @ColumnInfo(name = "merge_manga_image_url") val mergeMangaImageUrl: String?,
    @ColumnInfo(name = "alt_titles") val altTitles: String?,
    @ColumnInfo(name = "user_cover") val userCover: String?,
    @ColumnInfo(name = "user_title") val userTitle: String?,
    @ColumnInfo(name = "language_filter_flag") val filteredLanguage: String?,
    @ColumnInfo(name = "dynamic_cover") val dynamicCover: String?,
) {
    fun displayTitle(): String {
        return userTitle ?: title
    }
}
