package eu.kanade.tachiyomi.data.database.tables

object MergeMangaTable {

    const val TABLE = "merge_manga"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_COVER_URL = "cover_url"

    const val COL_TITLE = "title"

    const val COL_URL = "url"

    const val COL_MERGE_TYPE = "mergeType"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_TITLE TEXT NOT NULL,
            $COL_COVER_URL TEXT NOT NULL,
            $COL_URL TEXT NOT NULL,
            $COL_MERGE_TYPE INTEGER NOT NULL,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )
            """
}
