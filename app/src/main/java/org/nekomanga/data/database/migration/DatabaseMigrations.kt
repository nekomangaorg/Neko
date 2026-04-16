package org.nekomanga.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_45_46 =
        object : Migration(45, 46) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Core Tables
                migrateMangasTable(db)
                migrateChaptersTable(db)
                migrateCategoriesTable(db) // Must be migrated before mangas_categories

                // Relational Tables (Foreign Keys added)
                migrateArtworkTable(db)
                migrateMangasCategoriesTable(db)
                migrateMangaAggregateTable(db)
                migrateMergeMangaTable(db)
                migrateHistoryTable(db)
                migrateTrackTable(db)

                // Standalone / Supporting Tables
                migrateBrowseFilterTable(db)
                migrateScanlatorGroupsTable(db)
                migrateMangaRelatedTable(db)
                migrateUploaderTable(db)

                // Create all performance indices at the end
                createAllIndices(db)
            }
        }

    private fun migrateMangasTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `mangas_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `source` INTEGER NOT NULL,
                `url` TEXT NOT NULL,
                `artist` TEXT,
                `author` TEXT,
                `description` TEXT,
                `genre` TEXT,
                `title` TEXT NOT NULL,
                `status` INTEGER NOT NULL,
                `thumbnail_url` TEXT,
                `favorite` INTEGER NOT NULL,
                `last_update` INTEGER,
                `next_update` INTEGER,
                `date_added` INTEGER,
                `initialized` INTEGER NOT NULL,
                `viewer` INTEGER NOT NULL,
                `chapter_flags` INTEGER NOT NULL,
                `lang_flag` TEXT,
                `follow_status` INTEGER NOT NULL,
                `anilist_id` TEXT,
                `kitsu_id` TEXT,
                `my_anime_list_id` TEXT,
                `manga_updates_id` TEXT,
                `anime_planet_id` TEXT,
                `other_urls` TEXT,
                `scanlator_filter_flag` TEXT,
                `missing_chapters` TEXT,
                `rating` TEXT,
                `users` TEXT,
                `thread_id` TEXT,
                `replies_count` TEXT,
                `merge_manga_url` TEXT,
                `manga_last_volume` INTEGER,
                `manga_last_chapter` INTEGER,
                `merge_manga_image_url` TEXT,
                `alt_titles` TEXT,
                `user_cover` TEXT,
                `user_title` TEXT,
                `language_filter_flag` TEXT,
                `dynamic_cover` TEXT
            )
            """
                .trimIndent()
        )

        // Using COALESCE for all columns that are NOT NULL in Room but may be nullable in legacy DB
        db.execSQL(
            """
            INSERT INTO `mangas_new` (`_id`, `source`, `url`, `artist`, `author`, `description`, `genre`, `title`, `status`, `thumbnail_url`, `favorite`, `last_update`, `next_update`, `date_added`, `initialized`, `viewer`, `chapter_flags`, `lang_flag`, `follow_status`, `anilist_id`, `kitsu_id`, `my_anime_list_id`, `manga_updates_id`, `anime_planet_id`, `other_urls`, `scanlator_filter_flag`, `missing_chapters`, `rating`, `users`, `thread_id`, `replies_count`, `merge_manga_url`, `manga_last_volume`, `manga_last_chapter`, `merge_manga_image_url`, `alt_titles`, `user_cover`, `user_title`, `language_filter_flag`, `dynamic_cover`)
            SELECT _id, COALESCE(source, 0), COALESCE(url, ''), artist, author, description, genre, COALESCE(title, ''), COALESCE(status, 0), thumbnail_url, COALESCE(favorite, 0), last_update, next_update, date_added, COALESCE(initialized, 0), COALESCE(viewer, 0), COALESCE(chapter_flags, 0), lang_flag, COALESCE(follow_status, 0), anilist_id, kitsu_id, my_anime_list_id, manga_updates_id, anime_planet_id, other_urls, scanlator_filter_flag, missing_chapters, rating, users, thread_id, replies_count, merge_manga_url, manga_last_volume, manga_last_chapter, merge_manga_image_url, alt_titles, user_cover, user_title, language_filter_flag, dynamic_cover FROM mangas 
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `mangas`")
        db.execSQL("ALTER TABLE `mangas_new` RENAME TO `mangas`")
    }

    private fun migrateChaptersTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chapters_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `manga_id` INTEGER NOT NULL,
                `url` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `chapter_txt` TEXT NOT NULL,
                `chapter_title` TEXT NOT NULL,
                `vol` TEXT NOT NULL,
                `scanlator` TEXT,
                `uploader` TEXT,
                `unavailable` INTEGER NOT NULL,
                `read` INTEGER NOT NULL,
                `bookmark` INTEGER NOT NULL,
                `last_page_read` INTEGER NOT NULL,
                `pages_left` INTEGER NOT NULL,
                `chapter_number` REAL NOT NULL,
                `source_order` INTEGER NOT NULL,
                `smart_order` INTEGER NOT NULL,
                `date_fetch` INTEGER NOT NULL,
                `date_upload` INTEGER NOT NULL,
                `mangadex_chapter_id` TEXT,
                `old_mangadex_chapter_id` TEXT,
                `language` TEXT,
                FOREIGN KEY(`manga_id`) REFERENCES `mangas`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `chapters_new` (`_id`, `manga_id`, `url`, `name`, `chapter_txt`, `chapter_title`, `vol`, `scanlator`, `uploader`, `unavailable`, `read`, `bookmark`, `last_page_read`, `pages_left`, `chapter_number`, `source_order`, `smart_order`, `date_fetch`, `date_upload`, `mangadex_chapter_id`, `old_mangadex_chapter_id`, `language`)
            SELECT `_id`, COALESCE(`manga_id`, 0), COALESCE(`url`, ''), COALESCE(`name`, ''), COALESCE(`chapter_txt`, ''), COALESCE(`chapter_title`, ''), COALESCE(`vol`, ''), `scanlator`, `uploader`, COALESCE(`unavailable`, 0), COALESCE(`read`, 0), COALESCE(`bookmark`, 0), COALESCE(`last_page_read`, 0), COALESCE(`pages_left`, 0), COALESCE(`chapter_number`, 0.0), COALESCE(`source_order`, 0), COALESCE(`smart_order`, 0), COALESCE(`date_fetch`, 0), COALESCE(`date_upload`, 0), `mangadex_chapter_id`, `old_mangadex_chapter_id`, `language` FROM `chapters`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `chapters`")
        db.execSQL("ALTER TABLE `chapters_new` RENAME TO `chapters`")
    }

    private fun migrateCategoriesTable(db: SupportSQLiteDatabase) {
        // Ensure table exists to prevent select crashes on fresh/missing data
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `categories` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sort` INTEGER NOT NULL, `flags` INTEGER NOT NULL, `manga_order` TEXT NOT NULL)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `sort` INTEGER NOT NULL,
                `flags` INTEGER NOT NULL,
                `manga_order` TEXT NOT NULL
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `categories_new` (`_id`, `name`, `sort`, `flags`, `manga_order`)
            SELECT `_id`, COALESCE(`name`, ''), COALESCE(`sort`, 0), COALESCE(`flags`, 0), COALESCE(`manga_order`, '') FROM `categories`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `categories`")
        db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories`")
    }

    private fun migrateArtworkTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `artwork` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` INTEGER NOT NULL, `filename` TEXT NOT NULL, `volume` TEXT NOT NULL, `locale` TEXT NOT NULL, `description` TEXT NOT NULL)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `artwork_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `manga_id` INTEGER NOT NULL,
                `filename` TEXT NOT NULL,
                `volume` TEXT NOT NULL,
                `locale` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                FOREIGN KEY(`manga_id`) REFERENCES `mangas`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `artwork_new` (`_id`, `manga_id`, `filename`, `volume`, `locale`, `description`)
            SELECT `_id`, COALESCE(`manga_id`, 0), COALESCE(`filename`, ''), COALESCE(`volume`, ''), COALESCE(`locale`, ''), COALESCE(`description`, '') FROM `artwork`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `artwork`")
        db.execSQL("ALTER TABLE `artwork_new` RENAME TO `artwork`")
    }

    private fun migrateMangasCategoriesTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `mangas_categories` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` INTEGER NOT NULL, `category_id` INTEGER NOT NULL)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `mangas_categories_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `manga_id` INTEGER NOT NULL,
                `category_id` INTEGER NOT NULL,
                FOREIGN KEY(`manga_id`) REFERENCES `mangas`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `mangas_categories_new` (`_id`, `manga_id`, `category_id`)
            SELECT `_id`, COALESCE(`manga_id`, 0), COALESCE(`category_id`, 0) FROM `mangas_categories`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `mangas_categories`")
        db.execSQL("ALTER TABLE `mangas_categories_new` RENAME TO `mangas_categories`")
    }

    private fun migrateMangaAggregateTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `manga_aggregate` (`manga_id` INTEGER NOT NULL PRIMARY KEY, `volumes` TEXT NOT NULL)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `manga_aggregate_new` (
                `manga_id` INTEGER NOT NULL,
                `volumes` TEXT NOT NULL,
                PRIMARY KEY(`manga_id`),
                FOREIGN KEY(`manga_id`) REFERENCES `mangas`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `manga_aggregate_new` (`manga_id`, `volumes`)
            SELECT COALESCE(`manga_id`, 0), COALESCE(`volumes`, '') FROM `manga_aggregate`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `manga_aggregate`")
        db.execSQL("ALTER TABLE `manga_aggregate_new` RENAME TO `manga_aggregate`")
    }

    private fun migrateMergeMangaTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `merge_manga` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` INTEGER NOT NULL, `cover_url` TEXT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `mergeType` INTEGER NOT NULL)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `merge_manga_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `manga_id` INTEGER NOT NULL,
                `cover_url` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `url` TEXT NOT NULL,
                `mergeType` INTEGER NOT NULL,
                FOREIGN KEY(`manga_id`) REFERENCES `mangas`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `merge_manga_new` (`_id`, `manga_id`, `cover_url`, `title`, `url`, `mergeType`)
            SELECT `_id`, COALESCE(`manga_id`, 0), COALESCE(`cover_url`, ''), COALESCE(`title`, ''), COALESCE(`url`, ''), COALESCE(`mergeType`, 0) FROM `merge_manga`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `merge_manga`")
        db.execSQL("ALTER TABLE `merge_manga_new` RENAME TO `merge_manga`")
    }

    private fun migrateHistoryTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `history_new` (
                `history_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `history_chapter_id` INTEGER NOT NULL,
                `history_last_read` INTEGER NOT NULL,
                `history_time_read` INTEGER NOT NULL,
                FOREIGN KEY(`history_chapter_id`) REFERENCES `chapters`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `history_new` (`history_id`, `history_chapter_id`, `history_last_read`, `history_time_read`)
            SELECT history_id, COALESCE(history_chapter_id, 0), COALESCE(history_last_read, 0), COALESCE(history_time_read, 0) FROM history 
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `history`")
        db.execSQL("ALTER TABLE `history_new` RENAME TO `history`")
    }

    private fun migrateTrackTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `track` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `track_manga_id` INTEGER NOT NULL,
                `track_sync_id` INTEGER NOT NULL,
                `track_media_id` INTEGER NOT NULL,
                `track_library_id` INTEGER NOT NULL,
                `track_title` TEXT NOT NULL,
                `track_last_chapter_read` REAL NOT NULL,
                `track_total_chapters` INTEGER NOT NULL,
                `track_status` INTEGER NOT NULL,
                `track_score` REAL NOT NULL,
                `track_tracking_url` TEXT NOT NULL,
                `track_start_date` INTEGER NOT NULL,
                `track_finish_date` INTEGER NOT NULL,
                FOREIGN KEY(`track_manga_id`) REFERENCES `mangas`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `track` (
                `_id`, `track_manga_id`, `track_sync_id`, `track_media_id`, `track_library_id`,
                `track_title`, `track_last_chapter_read`, `track_total_chapters`, `track_status`,
                `track_score`, `track_tracking_url`, `track_start_date`, `track_finish_date`
            )
            SELECT
                _id, COALESCE(manga_id, 0), COALESCE(sync_id, 0), COALESCE(remote_id, 0), COALESCE(library_id, 0),
                COALESCE(title, ''), COALESCE(last_chapter_read, 0.0), COALESCE(total_chapters, 0), COALESCE(status, 0),
                COALESCE(score, 0.0), COALESCE(remote_url, ''), COALESCE(start_date, 0), COALESCE(finish_date, 0)
            FROM `manga_sync`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `manga_sync`")
    }

    private fun migrateBrowseFilterTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `browse_filter_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `filters` TEXT NOT NULL,
                `is_default` INTEGER NOT NULL
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `browse_filter_new` (`_id`, `name`, `filters`, `is_default`)
            SELECT `_id`, COALESCE(`name`, ''), COALESCE(`filters`, ''), COALESCE(`is_default`, 0) FROM `browse_filter`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `browse_filter`")
        db.execSQL("ALTER TABLE `browse_filter_new` RENAME TO `browse_filter`")
    }

    private fun migrateScanlatorGroupsTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `scanlator_group` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `uuid` TEXT NOT NULL, `description` TEXT)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `scanlator_group_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `uuid` TEXT NOT NULL,
                `description` TEXT
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `scanlator_group_new` (`_id`, `name`, `uuid`, `description`)
            SELECT `_id`, COALESCE(`name`, ''), COALESCE(`uuid`, ''), `description` FROM `scanlator_group`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `scanlator_group`")
        db.execSQL("ALTER TABLE `scanlator_group_new` RENAME TO `scanlator_group`")
    }

    private fun migrateMangaRelatedTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `manga_related` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` TEXT NOT NULL, `matched_ids` TEXT NOT NULL)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `manga_related_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `manga_id` TEXT NOT NULL,
                `matched_ids` TEXT NOT NULL
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO manga_related_new (_id, manga_id, matched_ids)
            SELECT _id, COALESCE(manga_id, ''), COALESCE(matched_ids, '') FROM manga_related
            """
                .trimIndent()
        )
        db.execSQL("DROP TABLE IF EXISTS `manga_related`")
        db.execSQL("ALTER TABLE `manga_related_new` RENAME TO `manga_related`")
    }

    private fun migrateUploaderTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `uploader` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `uuid` TEXT NOT NULL)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `uploader_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `username` TEXT NOT NULL,
                `uuid` TEXT NOT NULL
            )
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `uploader_new` (`_id`, `username`, `uuid`)
            SELECT `_id`, COALESCE(`username`, ''), COALESCE(`uuid`, '') FROM `uploader`
            """
                .trimIndent()
        )

        db.execSQL("DROP TABLE IF EXISTS `uploader`")
        db.execSQL("ALTER TABLE `uploader_new` RENAME TO `uploader`")
    }

    private fun createAllIndices(db: SupportSQLiteDatabase) {
        // Mangas
        db.execSQL("CREATE INDEX IF NOT EXISTS `library_favorite_index` ON `mangas` (`favorite`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `mangas_url_index` ON `mangas` (`url`)")

        // Chapters
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `chapters_manga_id_index` ON `chapters` (`manga_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `chapters_unread_by_manga_index` ON `chapters` (`manga_id`, `read`)"
        )

        // History
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `history_history_chapter_id_index` ON `history` (`history_chapter_id`)"
        )

        // Track
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `track_manga_id_index` ON `track` (`track_manga_id`)"
        )

        // Relational constraints indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `artwork_manga_id_index` ON `artwork` (`manga_id`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `mangas_categories_manga_id_index` ON `mangas_categories` (`manga_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `mangas_categories_category_id_index` ON `mangas_categories` (`category_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `merge_manga_manga_id_index` ON `merge_manga` (`manga_id`)"
        )
    }
}
