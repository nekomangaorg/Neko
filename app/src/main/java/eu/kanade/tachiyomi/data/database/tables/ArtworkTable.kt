package eu.kanade.tachiyomi.data.database.tables

object ArtworkTable {

    const val TABLE = "artwork"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_FILENAME = "filename"

    const val COL_VOLUME = "volume"

    const val COL_LOCALE = "locale"

    const val COL_DESCRIPTION = "description"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID INTEGER NOT NULL,
            $COL_FILENAME TEXT NOT NULL,
            $COL_VOLUME TEXT NOT NULL,
            $COL_LOCALE TEXT NOT NULL,
            $COL_DESCRIPTION TEXT NOT NULL,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )
            """
}
