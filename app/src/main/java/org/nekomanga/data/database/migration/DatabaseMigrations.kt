package org.nekomanga.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val MIGRATION_45_46 =
        object : Migration(45, 46) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create a temporary table with Room's strict Expected schema (INTEGER instead
                // of LONG/BOOLEAN)
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
                        `follow_status` INTEGER,
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

                // 2. Safely copy the existing user data into the new table
                db.execSQL(
                    """
                    INSERT INTO `mangas_new` (`_id`, `source`, `url`, `artist`, `author`, `description`, `genre`, `title`, `status`, `thumbnail_url`, `favorite`, `last_update`, `next_update`, `date_added`, `initialized`, `viewer`, `chapter_flags`, `lang_flag`, `follow_status`, `anilist_id`, `kitsu_id`, `my_anime_list_id`, `manga_updates_id`, `anime_planet_id`, `other_urls`, `scanlator_filter_flag`, `missing_chapters`, `rating`, `users`, `thread_id`, `replies_count`, `merge_manga_url`, `manga_last_volume`, `manga_last_chapter`, `merge_manga_image_url`, `alt_titles`, `user_cover`, `user_title`, `language_filter_flag`, `dynamic_cover`)
                    SELECT `_id`, `source`, `url`, `artist`, `author`, `description`, `genre`, `title`, `status`, `thumbnail_url`, `favorite`, `last_update`, `next_update`, `date_added`, `initialized`, `viewer`, `chapter_flags`, `lang_flag`, `follow_status`, `anilist_id`, `kitsu_id`, `my_anime_list_id`, `manga_updates_id`, `anime_planet_id`, `other_urls`, `scanlator_filter_flag`, `missing_chapters`, `rating`, `users`, `thread_id`, `replies_count`, `merge_manga_url`, `manga_last_volume`, `manga_last_chapter`, `merge_manga_image_url`, `alt_titles`, `user_cover`, `user_title`, `language_filter_flag`, `dynamic_cover` FROM `mangas`
                    """
                        .trimIndent()
                )

                // 3. Drop the old legacy table
                db.execSQL("DROP TABLE `mangas`")

                // 4. Rename the new table to the original name
                db.execSQL("ALTER TABLE `mangas_new` RENAME TO `mangas`")

                // 5. Recreate the indices exactly as Room expects them
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `library_favorite_index` ON `mangas` (`favorite`)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `mangas_url_index` ON `mangas` (`url`)")

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

                // 2. Safely copy the existing user data into the new table
                db.execSQL(
                    """
                    INSERT INTO `chapters_new` (`_id`, `manga_id`, `url`, `name`, `chapter_txt`, `chapter_title`, `vol`, `scanlator`, `uploader`, `unavailable`, `read`, `bookmark`, `last_page_read`, `pages_left`, `chapter_number`, `source_order`, `smart_order`, `date_fetch`, `date_upload`, `mangadex_chapter_id`, `old_mangadex_chapter_id`, `language`)
                    SELECT `_id`, `manga_id`, `url`, `name`, `chapter_txt`, `chapter_title`, `vol`, `scanlator`, `uploader`, `unavailable`, `read`, `bookmark`, `last_page_read`, `pages_left`, `chapter_number`, `source_order`, `smart_order`, `date_fetch`, `date_upload`, `mangadex_chapter_id`, `old_mangadex_chapter_id`, `language` FROM `chapters`
                    """
                        .trimIndent()
                )

                // 3. Drop the old legacy table
                db.execSQL("DROP TABLE `chapters`")

                // 4. Rename the new table to the original name
                db.execSQL("ALTER TABLE `chapters_new` RENAME TO `chapters`")

                // 5. Recreate the indices exactly as Room expects them
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `chapters_manga_id_index` ON `chapters` (`manga_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `chapters_unread_by_manga_index` ON `chapters` (`manga_id`, `read`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `chapters_bookmarked_by_manga_index` ON `chapters` (`manga_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `chapters_unavailable_by_manga_index` ON `chapters` (`manga_id`)"
                )

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

                // 2. Safely copy the existing user data into the new table
                db.execSQL(
                    """
                    INSERT INTO `browse_filter_new` (`_id`, `name`, `filters`, `is_default`)
                    SELECT `_id`, `name`, `filters`, `is_default` FROM `browse_filter`
                    """
                        .trimIndent()
                )

                // 3. Drop the old legacy table
                db.execSQL("DROP TABLE `browse_filter`")

                // 4. Rename the new table to the original name
                db.execSQL("ALTER TABLE `browse_filter_new` RENAME TO `browse_filter`")

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

                // 2. Safely copy the existing user data into the new table, converting any NULLs to
                // 0
                db.execSQL(
                    """
                    INSERT INTO `history_new` (`history_id`, `history_chapter_id`, `history_last_read`, `history_time_read`)
                    SELECT `history_id`, `history_chapter_id`, COALESCE(`history_last_read`, 0), COALESCE(`history_time_read`, 0) FROM `history`
                    """
                        .trimIndent()
                )

                // 3. Drop the old legacy table
                db.execSQL("DROP TABLE `history`")

                // 4. Rename the new table to the original name
                db.execSQL("ALTER TABLE `history_new` RENAME TO `history`")

                // 5. Recreate the index
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `history_history_chapter_id_index` ON `history` (`history_chapter_id`)"
                )

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

                // 2. Copy the user's data from the old 'manga_sync' table into the new 'track'
                // table
                db.execSQL(
                    """
                    INSERT INTO `track` (
                        `_id`, `track_manga_id`, `track_sync_id`, `track_media_id`, `track_library_id`,
                        `track_title`, `track_last_chapter_read`, `track_total_chapters`, `track_status`,
                        `track_score`, `track_tracking_url`, `track_start_date`, `track_finish_date`
                    )
                    SELECT
                        `_id`, `manga_id`, `sync_id`, `remote_id`, `library_id`,
                        `title`, `last_chapter_read`, `total_chapters`, `status`,
                        `score`, `remote_url`, `start_date`, `finish_date`
                    FROM `manga_sync`
                    """
                        .trimIndent()
                )

                // 3. Drop the old legacy table
                db.execSQL("DROP TABLE IF EXISTS `manga_sync`")

                // 4. Create the index for the Foreign Key (Crucial for performance)
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `track_manga_id_index` ON `track` (`track_manga_id`)"
                )
            }
        }
}
