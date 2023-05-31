package eu.kanade.tachiyomi.data.database.tables

object TrackTable {

    const val TABLE = "manga_sync"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_SYNC_ID = "sync_id"

    const val COL_MEDIA_ID = "remote_id"

    const val COL_LIBRARY_ID = "library_id"

    const val COL_TITLE = "title"

    const val COL_LAST_CHAPTER_READ = "last_chapter_read"

    const val COL_STATUS = "status"

    const val COL_SCORE = "score"

    const val COL_TOTAL_CHAPTERS = "total_chapters"

    const val COL_LIST_ID = "list_id"

    const val COL_TRACKING_URL = "remote_url"

    const val COL_START_DATE = "start_date"

    const val COL_FINISH_DATE = "finish_date"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_SYNC_ID INTEGER NOT NULL,
            $COL_MEDIA_ID LONG NOT NULL,
            $COL_LIBRARY_ID INTEGER,
            $COL_TITLE TEXT NOT NULL,
            $COL_LAST_CHAPTER_READ REAL NOT NULL,
            $COL_TOTAL_CHAPTERS INTEGER NOT NULL,
            $COL_STATUS INTEGER NOT NULL,
            $COL_SCORE FLOAT NOT NULL,
            $COL_LIST_ID TEXT NOT NULL,
            $COL_TRACKING_URL TEXT NOT NULL,
            $COL_START_DATE LONG NOT NULL,
            $COL_FINISH_DATE LONG NOT NULL,
            UNIQUE ($COL_MANGA_ID, $COL_SYNC_ID) ON CONFLICT REPLACE,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )
            """

    val addTrackingUrl: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_TRACKING_URL TEXT DEFAULT ''"

    val addLibraryId: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_LIBRARY_ID INTEGER NULL"

    val addStartDate: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_START_DATE LONG NOT NULL DEFAULT 0"

    val addFinishDate: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_FINISH_DATE LONG NOT NULL DEFAULT 0"

    val renameTableToTemp: String
        get() =
            "ALTER TABLE $TABLE RENAME TO ${TABLE}_tmp"

    val insertFromTempTable: String
        get() =
            """
            |INSERT INTO $TABLE($COL_ID,$COL_MANGA_ID,$COL_SYNC_ID,$COL_MEDIA_ID,$COL_LIBRARY_ID,$COL_TITLE,$COL_LAST_CHAPTER_READ,$COL_TOTAL_CHAPTERS,$COL_STATUS,$COL_SCORE,$COL_TRACKING_URL,$COL_START_DATE,$COL_FINISH_DATE)
            |SELECT $COL_ID,$COL_MANGA_ID,$COL_SYNC_ID,$COL_MEDIA_ID,$COL_LIBRARY_ID,$COL_TITLE,$COL_LAST_CHAPTER_READ,$COL_TOTAL_CHAPTERS,$COL_STATUS,$COL_SCORE,$COL_TRACKING_URL,$COL_START_DATE,$COL_FINISH_DATE
            |FROM ${TABLE}_tmp
            """.trimMargin()

    val dropTempTable: String
        get() = "DROP TABLE ${TABLE}_tmp"

    val addListId: String
        get() = "ALTER TABLE $TABLE ADD COLUMN $COL_LIST_ID TEXT DEFAULT ''"
}
