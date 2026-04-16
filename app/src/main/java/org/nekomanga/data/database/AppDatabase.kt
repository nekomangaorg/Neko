package org.nekomanga.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.nekomanga.data.database.entity.ArtworkEntity
import org.nekomanga.data.database.entity.BrowseFilterEntity
import org.nekomanga.data.database.entity.CategoryEntity
import org.nekomanga.data.database.entity.ChapterEntity
import org.nekomanga.data.database.entity.HistoryEntity
import org.nekomanga.data.database.entity.MangaAggregateEntity
import org.nekomanga.data.database.entity.MangaCategoryEntity
import org.nekomanga.data.database.entity.MangaEntity
import org.nekomanga.data.database.entity.MangaSimilarEntity
import org.nekomanga.data.database.entity.MergeMangaEntity
import org.nekomanga.data.database.entity.ScanlatorGroupEntity
import org.nekomanga.data.database.entity.TrackEntity
import org.nekomanga.data.database.entity.UploaderEntity

@Database(
    entities =
        [
            MangaEntity::class,
            ChapterEntity::class,
            ArtworkEntity::class,
            BrowseFilterEntity::class,
            CategoryEntity::class,
            MangaCategoryEntity::class,
            HistoryEntity::class,
            TrackEntity::class,
            MangaAggregateEntity::class,
            ScanlatorGroupEntity::class,
            MangaSimilarEntity::class,
            UploaderEntity::class,
            MergeMangaEntity::class,
        ],
    version = 46, // Set higher than StorIO version 45
    exportSchema = true,
)
// @TypeConverters(MergeTypeConverter::class, FollowStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    /*  abstract fun mangaDao(): MangaDao

    abstract fun chapterDao(): ChapterDao

    abstract fun historyDao(): HistoryDao

    abstract fun categoryDao(): CategoryDao

    abstract fun trackDao(): TrackDao

    abstract fun artworkDao(): ArtworkDao

    abstract fun browseFilterDao(): BrowseFilterDao

    abstract fun mergeMangaDao(): MergeMangaDao

    abstract fun scanlatorDao(): ScanlatorGroupDao

    abstract fun uploaderDao(): UploaderDao

    abstract fun similarDao(): SimilarDao

    abstract fun mangaAggregateDao(): MangaAggregateDao

    abstract fun mangaCategoryDao(): MangaCategoryDao*/

    companion object {
        const val DATABASE_NAME = "tachiyomi.db"
    }
}
