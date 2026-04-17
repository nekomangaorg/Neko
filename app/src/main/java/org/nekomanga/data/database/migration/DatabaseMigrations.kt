package org.nekomanga.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_45_46 =
        object : Migration(45, 46) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateArtworkTable(db)
                migrateBrowseFilterTable(db)
                migrateCategoriesTable(db)
                migrateChaptersTable(db)
                migrateHistoryTable(db)
                migrateMangaTable(db)
                migrateMangaAggregateTable(db)
                migrateMangaCategoriesTable(db)
                migrateMangaSimilarTable(db)
                migrateTrackTable(db)
                migrateScanlatorGroupTable(db)
                migrateUploaderTable(db)
            }
        }

    private fun migrateArtworkTable(db: SupportSQLiteDatabase) {
        // 1. Create the new table matching the exact schema Room expects
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `artwork_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `manga_id` INTEGER NOT NULL,
                `file_name` TEXT NOT NULL,
                `volume` TEXT NOT NULL,
                `locale` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                FOREIGN KEY(`manga_id`) REFERENCES `mangas`(`_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 2. Map the legacy columns to your new clean column names
        db.execSQL(
            """
            INSERT INTO `artwork_new` (`id`, `manga_id`, `file_name`, `volume`, `locale`, `description`)
            SELECT `_id`, `manga_id`, `filename`, `volume`, `locale`, `description`
            FROM `artwork`
            """
                .trimIndent()
        )

        // 3. Drop the old table and rename the new one
        db.execSQL("DROP TABLE `artwork`")
        db.execSQL("ALTER TABLE `artwork_new` RENAME TO `artwork`")

        // 4. Create the new index you defined in the Entity
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_artwork_manga_id` ON `artwork` (`manga_id`)")
    }

    private fun migrateBrowseFilterTable(db: SupportSQLiteDatabase) {
        // 1. Create the new table matching the exact schema Room expects
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `browse_filter_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `filters` TEXT NOT NULL,
                `is_default` INTEGER NOT NULL
            )
            """
                .trimIndent()
        )

        // 2. Map the legacy columns to your new clean column names
        db.execSQL(
            """
            INSERT INTO `browse_filter_new` (`id`, `name`, `filters`, `is_default`)
            SELECT `_id`, `name`, `filters`, `is_default`
            FROM `browse_filter`
            """
                .trimIndent()
        )

        // 3. Drop the old table and rename the new one
        db.execSQL("DROP TABLE `browse_filter`")
        db.execSQL("ALTER TABLE `browse_filter_new` RENAME TO `browse_filter`")
    }

    private fun migrateCategoriesTable(db: SupportSQLiteDatabase) {
        // 1. Create the new table matching the exact schema Room expects
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `categories_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `sort` INTEGER NOT NULL,
                `flags` INTEGER NOT NULL,
                `manga_order` TEXT NOT NULL
            )
            """
                .trimIndent()
        )

        // 2. Map the legacy columns to your new clean column names
        // Note: Using COALESCE on manga_order protects against older DB versions where it might be
        // null
        db.execSQL(
            """
            INSERT INTO `categories_new` (`id`, `name`, `sort`, `flags`, `manga_order`)
            SELECT `_id`, `name`, `sort`, `flags`, COALESCE(`manga_order`, '')
            FROM `categories`
            """
                .trimIndent()
        )

        // 3. Drop the old table and rename the new one
        db.execSQL("DROP TABLE `categories`")
        db.execSQL("ALTER TABLE `categories_new` RENAME TO `categories`")
    }

    private fun migrateChaptersTable(db: SupportSQLiteDatabase) {
        // 1. Create the new table matching the exact schema Room expects (No
        // old_mangadex_chapter_id)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chapters_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
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
                `language` TEXT,
                FOREIGN KEY(`manga_id`) REFERENCES `mangas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 2. Map legacy columns safely using COALESCE (Omit old_mangadex_chapter_id from both
        // lists)
        db.execSQL(
            """
            INSERT INTO `chapters_new` (
                `id`, `manga_id`, `url`, `name`, `chapter_txt`, `chapter_title`, `vol`, `scanlator`,
                `uploader`, `unavailable`, `read`, `bookmark`, `last_page_read`, `pages_left`,
                `chapter_number`, `source_order`, `smart_order`, `date_fetch`, `date_upload`,
                `mangadex_chapter_id`, `language`
            )
            SELECT
                `_id`,
                COALESCE(`manga_id`, 0),
                COALESCE(`url`, ''),
                COALESCE(`name`, ''),
                COALESCE(`chapter_txt`, ''),
                COALESCE(`chapter_title`, ''),
                COALESCE(`vol`, ''),
                `scanlator`,
                `uploader`,
                COALESCE(`unavailable`, 0),
                COALESCE(`read`, 0),
                COALESCE(`bookmark`, 0),
                COALESCE(`last_page_read`, 0),
                COALESCE(`pages_left`, 0),
                COALESCE(`chapter_number`, 0.0),
                COALESCE(`source_order`, 0),
                COALESCE(`smart_order`, 0),
                COALESCE(`date_fetch`, 0),
                COALESCE(`date_upload`, 0),
                `mangadex_chapter_id`,
                `language`
            FROM `chapters`
            """
                .trimIndent()
        )

        // 3. Drop old table and rename the new one
        db.execSQL("DROP TABLE `chapters`")
        db.execSQL("ALTER TABLE `chapters_new` RENAME TO `chapters`")
    }

    private fun migrateHistoryTable(db: SupportSQLiteDatabase) {
        // 1. Create the new table with clean column names
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `history_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `chapter_id` INTEGER NOT NULL,
                `last_read` INTEGER NOT NULL,
                `time_read` INTEGER NOT NULL,
                FOREIGN KEY(`chapter_id`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 2. Map old columns to new columns (using COALESCE for previously nullable fields)
        db.execSQL(
            """
            INSERT INTO `history_new` (`id`, `chapter_id`, `last_read`, `time_read`)
            SELECT `history_id`, `history_chapter_id`, COALESCE(`history_last_read`, 0), COALESCE(`history_time_read`, 0)
            FROM `history`
            """
                .trimIndent()
        )

        // 3. Swap tables
        db.execSQL("DROP TABLE `history`")
        db.execSQL("ALTER TABLE `history_new` RENAME TO `history`")

        // 4. Rebuild the unique index
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_history_chapter_id` ON `history` (`chapter_id`)"
        )
    }

    private fun migrateMangaTable(db: SupportSQLiteDatabase) {
        // 1. Create the new table matching the exact schema Room expects
        // Note: The table name is now 'manga' (singular) instead of 'mangas'
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `manga_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
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

        // 2. Map legacy columns safely using COALESCE
        // We explicitly omit the dropped columns (unread, has_read, etc.)
        db.execSQL(
            """
            INSERT INTO `manga_new` (
                `id`, `source`, `url`, `artist`, `author`, `description`, `genre`, `title`,
                `status`, `thumbnail_url`, `favorite`, `last_update`, `next_update`, `date_added`,
                `initialized`, `viewer`, `chapter_flags`, `lang_flag`, `follow_status`, `anilist_id`,
                `kitsu_id`, `my_anime_list_id`, `manga_updates_id`, `anime_planet_id`, `other_urls`,
                `scanlator_filter_flag`, `missing_chapters`, `rating`, `users`, `thread_id`, `replies_count`,
                `merge_manga_url`, `manga_last_volume`, `manga_last_chapter`, `merge_manga_image_url`,
                `alt_titles`, `user_cover`, `user_title`, `language_filter_flag`, `dynamic_cover`
            )
            SELECT
                `_id`, COALESCE(`source`, 0), COALESCE(`url`, ''), `artist`, `author`, `description`, `genre`,
                COALESCE(`title`, ''), COALESCE(`status`, 0), `thumbnail_url`, COALESCE(`favorite`, 0), `last_update`,
                `next_update`, `date_added`, COALESCE(`initialized`, 0), COALESCE(`viewer`, 0), COALESCE(`chapter_flags`, 0),
                `lang_flag`, COALESCE(`follow_status`, 0), `anilist_id`, `kitsu_id`, `my_anime_list_id`,
                `manga_updates_id`, `anime_planet_id`, `other_urls`, `scanlator_filter_flag`, `missing_chapters`,
                `rating`, `users`, `thread_id`, `replies_count`, `merge_manga_url`, `manga_last_volume`,
                `manga_last_chapter`, `merge_manga_image_url`, `alt_titles`, `user_cover`, `user_title`,
                `language_filter_flag`, `dynamic_cover`
            FROM `mangas`
            """
                .trimIndent()
        )

        // 3. Drop old table and rename the new one
        db.execSQL("DROP TABLE IF EXISTS `mangas`")
        db.execSQL("ALTER TABLE `manga_new` RENAME TO `manga`")

        // 4. Create the indices required by Room
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_manga_favorite` ON `manga` (`favorite`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_manga_url` ON `manga` (`url`)")
    }

    private fun migrateMangaAggregateTable(db: SupportSQLiteDatabase) {
        // 1. Create the new table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `manga_aggregate_new` (
                `manga_id` INTEGER NOT NULL,
                `volumes` TEXT NOT NULL,
                PRIMARY KEY(`manga_id`),
                FOREIGN KEY(`manga_id`) REFERENCES `manga`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 2. Map legacy columns
        db.execSQL(
            """
            INSERT INTO `manga_aggregate_new` (`manga_id`, `volumes`)
            SELECT `manga_id`, COALESCE(`volumes`, '')
            FROM `manga_aggregate`
            """
                .trimIndent()
        )

        // 3. Swap tables
        db.execSQL("DROP TABLE `manga_aggregate`")
        db.execSQL("ALTER TABLE `manga_aggregate_new` RENAME TO `manga_aggregate`")
    }

    private fun migrateMangaCategoriesTable(db: SupportSQLiteDatabase) {
        // 1. Create the target table directly (since the name is new)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `manga_categories` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `manga_id` INTEGER NOT NULL,
                `category_id` INTEGER NOT NULL,
                FOREIGN KEY(`manga_id`) REFERENCES `manga`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`category_id`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 2. Copy data from old table to new table
        db.execSQL(
            """
            INSERT INTO `manga_categories` (`id`, `manga_id`, `category_id`)
            SELECT `_id`, `manga_id`, `category_id`
            FROM `mangas_categories`
            """
                .trimIndent()
        )

        // 3. Drop the old table
        db.execSQL("DROP TABLE `mangas_categories`")

        // 4. Create the indices defined in the Entity
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_manga_categories_manga_id` ON `manga_categories` (`manga_id`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_manga_categories_category_id` ON `manga_categories` (`category_id`)"
        )
    }

    private fun migrateMangaSimilarTable(db: SupportSQLiteDatabase) {
        // 1. Create the target table directly (since the name is new)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `manga_similar` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `manga_id` TEXT NOT NULL,
                `matched_ids` TEXT NOT NULL
            )
            """
                .trimIndent()
        )

        // 2. Copy data from old table to new table
        db.execSQL(
            """
            INSERT INTO `manga_similar` (`id`, `manga_id`, `matched_ids`)
            SELECT `_id`, COALESCE(`manga_id`, ''), COALESCE(`matched_ids`, '')
            FROM `manga_related`
            """
                .trimIndent()
        )

        // 3. Drop the old table
        db.execSQL("DROP TABLE `manga_related`")

        // 4. Create the index defined in the Entity
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_manga_similar_manga_id` ON `manga_similar` (`manga_id`)"
        )
    }

    private fun migrateTrackTable(db: SupportSQLiteDatabase) {
        // 1. Create the target table directly (since the name is new)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `track` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `track_manga_id` INTEGER NOT NULL,
                `track_service_id` INTEGER NOT NULL,
                `media_id` INTEGER NOT NULL,
                `library_id` INTEGER,
                `title` TEXT NOT NULL,
                `last_chapter_read` REAL NOT NULL,
                `total_chapters` INTEGER NOT NULL,
                `status` INTEGER NOT NULL,
                `score` REAL NOT NULL,
                `tracking_url` TEXT NOT NULL,
                `start_date` INTEGER NOT NULL,
                `finish_date` INTEGER NOT NULL,
                FOREIGN KEY(`track_manga_id`) REFERENCES `manga`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """
                .trimIndent()
        )

        // 2. Map legacy columns to the new clean column names safely
        db.execSQL(
            """
            INSERT INTO `track` (
                `id`, `track_manga_id`, `track_service_id`, `media_id`, `library_id`,
                `title`, `last_chapter_read`, `total_chapters`, `status`,
                `score`, `tracking_url`, `start_date`, `finish_date`
            )
            SELECT
                `_id`,
                COALESCE(`manga_id`, 0),
                COALESCE(`sync_id`, 0),
                COALESCE(`remote_id`, 0),
                `library_id`,
                COALESCE(`title`, ''),
                COALESCE(`last_chapter_read`, 0.0),
                COALESCE(`total_chapters`, 0),
                COALESCE(`status`, 0),
                COALESCE(`score`, 0.0),
                COALESCE(`remote_url`, ''),
                COALESCE(`start_date`, 0),
                COALESCE(`finish_date`, 0)
            FROM `manga_sync`
            """
                .trimIndent()
        )

        // 3. Drop the old table
        db.execSQL("DROP TABLE IF EXISTS `manga_sync`")

        // 4. Create the indices defined in the Entity
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `track_manga_id_index` ON `track` (`track_manga_id`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_track_track_manga_id_track_service_id` ON `track` (`track_manga_id`, `track_service_id`)"
        )
    }

    private fun migrateScanlatorGroupTable(db: SupportSQLiteDatabase) {
        // 1. Create the new table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `scanlator_group_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `uuid` TEXT NOT NULL,
                `description` TEXT
            )
            """
                .trimIndent()
        )

        // 2. Copy the data
        db.execSQL(
            """
            INSERT INTO `scanlator_group_new` (`id`, `name`, `uuid`, `description`)
            SELECT `_id`, COALESCE(`name`, ''), COALESCE(`uuid`, ''), `description`
            FROM `scanlator_group`
            """
                .trimIndent()
        )

        // 3. Swap tables
        db.execSQL("DROP TABLE IF EXISTS `scanlator_group`")
        db.execSQL("ALTER TABLE `scanlator_group_new` RENAME TO `scanlator_group`")

        // 4. Create the UNIQUE index
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_scanlator_group_uuid` ON `scanlator_group` (`uuid`)"
        )
    }

    private fun migrateUploaderTable(db: SupportSQLiteDatabase) {
        // 1. Create the new table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `uploader_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `username` TEXT NOT NULL,
                `uuid` TEXT NOT NULL
            )
            """
                .trimIndent()
        )

        // 2. Copy the data
        db.execSQL(
            """
            INSERT INTO `uploader_new` (`id`, `username`, `uuid`)
            SELECT `_id`, COALESCE(`username`, ''), COALESCE(`uuid`, '')
            FROM `uploader`
            """
                .trimIndent()
        )

        // 3. Swap tables
        db.execSQL("DROP TABLE IF EXISTS `uploader`")
        db.execSQL("ALTER TABLE `uploader_new` RENAME TO `uploader`")

        // 4. Create the UNIQUE index
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_uploader_uuid` ON `uploader` (`uuid`)")
    }
}
