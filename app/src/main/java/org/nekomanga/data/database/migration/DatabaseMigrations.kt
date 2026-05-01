package org.nekomanga.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_45_46 =
        object : Migration(45, 46) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 0. Clean up any leftover temporary tables from previous failed migrations
                dropTempTables(db)

                // 1. Copy all data into temporary tables (strips constraints)
                backupDataToTempTables(db)

                // 2. Safely drop old tables in the correct order to respect Foreign Keys
                dropOldTables(db)

                // 3. Create the new tables with the updated Room schemas
                createNewTables(db)

                // 4. Restore the data, applying transformations/COALESCE where needed
                restoreDataFromTempTables(db)

                // 5. Create indices required by Room
                createIndices(db)

                // 6. Clean up temporary tables
                dropTempTables(db)
            }
        }

    private fun backupDataToTempTables(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE `temp_mangas` AS SELECT * FROM `mangas`")
        db.execSQL("CREATE TABLE `temp_chapters` AS SELECT * FROM `chapters`")
        db.execSQL("CREATE TABLE `temp_history` AS SELECT * FROM `history`")
        db.execSQL("CREATE TABLE `temp_categories` AS SELECT * FROM `categories`")
        db.execSQL("CREATE TABLE `temp_mangas_categories` AS SELECT * FROM `mangas_categories`")
        db.execSQL("CREATE TABLE `temp_manga_aggregate` AS SELECT * FROM `manga_aggregate`")
        db.execSQL("CREATE TABLE `temp_manga_sync` AS SELECT * FROM `manga_sync`")
        db.execSQL("CREATE TABLE `temp_artwork` AS SELECT * FROM `artwork`")
        db.execSQL("CREATE TABLE `temp_browse_filter` AS SELECT * FROM `browse_filter`")
        db.execSQL("CREATE TABLE `temp_manga_related` AS SELECT * FROM `manga_related`")
        db.execSQL("CREATE TABLE `temp_scanlator_group` AS SELECT * FROM `scanlator_group`")
        db.execSQL("CREATE TABLE `temp_uploader` AS SELECT * FROM `uploader`")
        db.execSQL("CREATE TABLE `temp_merge_manga` AS SELECT * FROM `merge_manga`")
    }

    private fun dropOldTables(db: SupportSQLiteDatabase) {
        // Child tables MUST be dropped before Parent tables
        db.execSQL("DROP TABLE IF EXISTS `history`") // Child of chapters
        db.execSQL("DROP TABLE IF EXISTS `artwork`") // Child of mangas
        db.execSQL("DROP TABLE IF EXISTS `manga_sync`") // Child of mangas
        db.execSQL("DROP TABLE IF EXISTS `manga_aggregate`") // Child of mangas
        db.execSQL("DROP TABLE IF EXISTS `mangas_categories`") // Child of mangas & categories
        db.execSQL("DROP TABLE IF EXISTS `chapters`") // Child of mangas

        // Parent tables and standalone tables
        db.execSQL("DROP TABLE IF EXISTS `mangas`")
        db.execSQL("DROP TABLE IF EXISTS `categories`")
        db.execSQL("DROP TABLE IF EXISTS `browse_filter`")
        db.execSQL("DROP TABLE IF EXISTS `manga_related`")
        db.execSQL("DROP TABLE IF EXISTS `scanlator_group`")
        db.execSQL("DROP TABLE IF EXISTS `uploader`")
        db.execSQL("DROP TABLE IF EXISTS `merge_manga`")
    }

    private fun createNewTables(db: SupportSQLiteDatabase) {
        // 1. Manga (Parent)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `manga` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `source` INTEGER NOT NULL, `url` TEXT NOT NULL,
                `artist` TEXT, `author` TEXT, `description` TEXT, `genre` TEXT, `title` TEXT NOT NULL, `status` INTEGER NOT NULL,
                `thumbnail_url` TEXT, `favorite` INTEGER NOT NULL, `last_update` INTEGER, `next_update` INTEGER,
                `date_added` INTEGER, `initialized` INTEGER NOT NULL, `viewer` INTEGER NOT NULL, `chapter_flags` INTEGER NOT NULL,
                `lang_flag` TEXT, `follow_status` INTEGER NOT NULL, `anilist_id` TEXT, `kitsu_id` TEXT, `my_anime_list_id` TEXT,
                `manga_updates_id` TEXT, `anime_planet_id` TEXT, `other_urls` TEXT, `scanlator_filter_flag` TEXT, `missing_chapters` TEXT,
                `rating` TEXT, `users` TEXT, `thread_id` TEXT, `replies_count` TEXT, `merge_manga_url` TEXT, `manga_last_volume` INTEGER,
                `manga_last_chapter` INTEGER, `merge_manga_image_url` TEXT, `alt_titles` TEXT, `user_cover` TEXT, `user_title` TEXT,
                `language_filter_flag` TEXT, `dynamic_cover` TEXT
            )
            """
                .trimIndent()
        )

        // 2. Categories (Parent)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sort` INTEGER NOT NULL,
                `flags` INTEGER NOT NULL, `manga_order` TEXT NOT NULL
            )
            """
                .trimIndent()
        )

        // 3. Chapters (Child of Manga)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chapters` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` INTEGER NOT NULL, `url` TEXT NOT NULL,
                `name` TEXT NOT NULL, `chapter_txt` TEXT NOT NULL, `chapter_title` TEXT NOT NULL, `vol` TEXT NOT NULL,
                `scanlator` TEXT, `uploader` TEXT, `unavailable` INTEGER NOT NULL, `read` INTEGER NOT NULL, `bookmark` INTEGER NOT NULL,
                `last_page_read` INTEGER NOT NULL, `pages_left` INTEGER NOT NULL, `chapter_number` REAL NOT NULL,
                `source_order` INTEGER NOT NULL, `smart_order` INTEGER NOT NULL, `date_fetch` INTEGER NOT NULL,
                `date_upload` INTEGER NOT NULL, `mangadex_chapter_id` TEXT, `language` TEXT,
                FOREIGN KEY(`manga_id`) REFERENCES `manga`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 4. History (Child of Chapters)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `chapter_id` INTEGER NOT NULL, `last_read` INTEGER NOT NULL, `time_read` INTEGER NOT NULL,
                FOREIGN KEY(`chapter_id`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 5. Manga Categories (Child of Manga & Categories)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `manga_categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` INTEGER NOT NULL, `category_id` INTEGER NOT NULL,
                FOREIGN KEY(`manga_id`) REFERENCES `manga`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 6. Manga Aggregate (Child of Manga)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `manga_aggregate` (
                `manga_id` INTEGER NOT NULL, `volumes` TEXT NOT NULL, PRIMARY KEY(`manga_id`),
                FOREIGN KEY(`manga_id`) REFERENCES `manga`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 7. Track (Child of Manga)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `track` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` INTEGER NOT NULL, `track_service_id` INTEGER NOT NULL,
                `media_id` INTEGER NOT NULL, `library_id` INTEGER, `title` TEXT NOT NULL, `last_chapter_read` REAL NOT NULL,
                `total_chapters` INTEGER NOT NULL, `status` INTEGER NOT NULL, `score` REAL NOT NULL, `tracking_url` TEXT NOT NULL,
                `start_date` INTEGER NOT NULL, `finish_date` INTEGER NOT NULL,
                FOREIGN KEY(`manga_id`) REFERENCES `manga`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 8. Artwork (Child of Manga)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `artwork` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` INTEGER NOT NULL, `file_name` TEXT NOT NULL,
                `volume` TEXT NOT NULL, `locale` TEXT NOT NULL, `description` TEXT NOT NULL,
                FOREIGN KEY(`manga_id`) REFERENCES `manga`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 9. Merge Manga (Child of Manga)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `merge_manga` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` INTEGER NOT NULL,
                `cover_url` TEXT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `merge_type` INTEGER NOT NULL,
                FOREIGN KEY(`manga_id`) REFERENCES `manga`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 10. Independent tables
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `browse_filter` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `filters` TEXT NOT NULL, `is_default` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `manga_similar` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manga_id` TEXT NOT NULL, `matched_ids` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `scanlator_group` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `uuid` TEXT NOT NULL, `description` TEXT)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `uploader` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT NOT NULL, `uuid` TEXT NOT NULL)"
        )
    }

    private fun restoreDataFromTempTables(db: SupportSQLiteDatabase) {
        // Insert order matters for Foreign Keys: Parents first, then children

        db.execSQL(
            """
            INSERT INTO `manga` (
                `id`, `source`, `url`, `artist`, `author`, `description`, `genre`, `title`, `status`, `thumbnail_url`,
                `favorite`, `last_update`, `next_update`, `date_added`, `initialized`, `viewer`, `chapter_flags`,
                `lang_flag`, `follow_status`, `anilist_id`, `kitsu_id`, `my_anime_list_id`, `manga_updates_id`,
                `anime_planet_id`, `other_urls`, `scanlator_filter_flag`, `missing_chapters`, `rating`, `users`,
                `thread_id`, `replies_count`, `merge_manga_url`, `manga_last_volume`, `manga_last_chapter`,
                `merge_manga_image_url`, `alt_titles`, `user_cover`, `user_title`, `language_filter_flag`, `dynamic_cover`
            )
            SELECT
                `_id`, COALESCE(`source`, 0), COALESCE(`url`, ''), `artist`, `author`, `description`, `genre`, COALESCE(`title`, ''),
                COALESCE(`status`, 0), `thumbnail_url`, COALESCE(`favorite`, 0), `last_update`, `next_update`, `date_added`,
                COALESCE(`initialized`, 0), COALESCE(`viewer`, 0), COALESCE(`chapter_flags`, 0), `lang_flag`, COALESCE(`follow_status`, 0),
                `anilist_id`, `kitsu_id`, `my_anime_list_id`, `manga_updates_id`, `anime_planet_id`, `other_urls`, `scanlator_filter_flag`,
                `missing_chapters`, `rating`, `users`, `thread_id`, `replies_count`, `merge_manga_url`, `manga_last_volume`,
                `manga_last_chapter`, `merge_manga_image_url`, `alt_titles`, `user_cover`, `user_title`, `language_filter_flag`, `dynamic_cover`
            FROM `temp_mangas`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `categories` (`id`, `name`, `sort`, `flags`, `manga_order`)
            SELECT `_id`, `name`, `sort`, `flags`, COALESCE(`manga_order`, '')
            FROM `temp_categories`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `chapters` (
                `id`, `manga_id`, `url`, `name`, `chapter_txt`, `chapter_title`, `vol`, `scanlator`, `uploader`,
                `unavailable`, `read`, `bookmark`, `last_page_read`, `pages_left`, `chapter_number`, `source_order`,
                `smart_order`, `date_fetch`, `date_upload`, `mangadex_chapter_id`, `language`
            )
            SELECT
                `_id`, COALESCE(`manga_id`, 0), COALESCE(`url`, ''), COALESCE(`name`, ''), COALESCE(`chapter_txt`, ''),
                COALESCE(`chapter_title`, ''), COALESCE(`vol`, ''), `scanlator`, `uploader`, COALESCE(`unavailable`, 0),
                COALESCE(`read`, 0), COALESCE(`bookmark`, 0), COALESCE(`last_page_read`, 0), COALESCE(`pages_left`, 0),
                COALESCE(`chapter_number`, 0.0), COALESCE(`source_order`, 0), COALESCE(`smart_order`, 0), COALESCE(`date_fetch`, 0),
                COALESCE(`date_upload`, 0), `mangadex_chapter_id`, `language`
            FROM `temp_chapters`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `history` (`id`, `chapter_id`, `last_read`, `time_read`)
            SELECT `history_id`, `history_chapter_id`, COALESCE(`history_last_read`, 0), COALESCE(`history_time_read`, 0)
            FROM `temp_history`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `manga_categories` (`id`, `manga_id`, `category_id`)
            SELECT `_id`, `manga_id`, `category_id`
            FROM `temp_mangas_categories`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `manga_aggregate` (`manga_id`, `volumes`)
            SELECT `manga_id`, COALESCE(`volumes`, '')
            FROM `temp_manga_aggregate`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `track` (
                `id`, `manga_id`, `track_service_id`, `media_id`, `library_id`, `title`, `last_chapter_read`,
                `total_chapters`, `status`, `score`, `tracking_url`, `start_date`, `finish_date`
            )
            SELECT
                `_id`, COALESCE(`manga_id`, 0), COALESCE(`sync_id`, 0), COALESCE(`remote_id`, 0), `library_id`, COALESCE(`title`, ''),
                COALESCE(`last_chapter_read`, 0.0), COALESCE(`total_chapters`, 0), COALESCE(`status`, 0), COALESCE(`score`, 0.0),
                COALESCE(`remote_url`, ''), COALESCE(`start_date`, 0), COALESCE(`finish_date`, 0)
            FROM `temp_manga_sync`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `artwork` (`id`, `manga_id`, `file_name`, `volume`, `locale`, `description`)
            SELECT `_id`, `manga_id`, `filename`, `volume`, `locale`, `description`
            FROM `temp_artwork`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `merge_manga` (`id`, `manga_id`, `cover_url`, `title`, `url`, `merge_type`)
            SELECT `_id`, COALESCE(`manga_id`, 0), COALESCE(`cover_url`, ''), COALESCE(`title`, ''), COALESCE(`url`, ''), COALESCE(`mergeType`, 0)
            FROM `temp_merge_manga`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `browse_filter` (`id`, `name`, `filters`, `is_default`)
            SELECT `_id`, `name`, `filters`, `is_default`
            FROM `temp_browse_filter`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `manga_similar` (`id`, `manga_id`, `matched_ids`)
            SELECT `_id`, COALESCE(`manga_id`, ''), COALESCE(`matched_ids`, '')
            FROM `temp_manga_related`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `scanlator_group` (`id`, `name`, `uuid`, `description`)
            SELECT `_id`, COALESCE(`name`, ''), COALESCE(`uuid`, ''), `description`
            FROM `temp_scanlator_group`
            """
                .trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `uploader` (`id`, `username`, `uuid`)
            SELECT `_id`, COALESCE(`username`, ''), COALESCE(`uuid`, '')
            FROM `temp_uploader`
            """
                .trimIndent()
        )
    }

    private fun createIndices(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_artwork_manga_id` ON `artwork` (`manga_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_manga_favorite` ON `manga` (`favorite`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_manga_url` ON `manga` (`url`)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_manga_categories_manga_id` ON `manga_categories` (`manga_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_manga_categories_category_id` ON `manga_categories` (`category_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_manga_similar_manga_id` ON `manga_similar` (`manga_id`)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_track_manga_id` ON `track` (`manga_id`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_track_manga_id_track_service_id` ON `track` (`manga_id`, `track_service_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_merge_manga_manga_id` ON `merge_manga` (`manga_id`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_scanlator_group_uuid` ON `scanlator_group` (`uuid`)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_uploader_uuid` ON `uploader` (`uuid`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_history_chapter_id` ON `history` (`chapter_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chapters_manga_id` ON `chapters` (`manga_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chapters_manga_id_read` ON `chapters` (`manga_id`, `read`)"
        )
    }

    private fun dropTempTables(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS `temp_mangas`")
        db.execSQL("DROP TABLE IF EXISTS `temp_chapters`")
        db.execSQL("DROP TABLE IF EXISTS `temp_history`")
        db.execSQL("DROP TABLE IF EXISTS `temp_categories`")
        db.execSQL("DROP TABLE IF EXISTS `temp_mangas_categories`")
        db.execSQL("DROP TABLE IF EXISTS `temp_manga_aggregate`")
        db.execSQL("DROP TABLE IF EXISTS `temp_manga_sync`")
        db.execSQL("DROP TABLE IF EXISTS `temp_artwork`")
        db.execSQL("DROP TABLE IF EXISTS `temp_merge_manga`")
        db.execSQL("DROP TABLE IF EXISTS `temp_browse_filter`")
        db.execSQL("DROP TABLE IF EXISTS `temp_manga_related`")
        db.execSQL("DROP TABLE IF EXISTS `temp_scanlator_group`")
        db.execSQL("DROP TABLE IF EXISTS `temp_uploader`")
    }
}
