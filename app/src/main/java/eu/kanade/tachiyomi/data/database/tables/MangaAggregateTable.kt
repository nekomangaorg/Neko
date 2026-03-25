package eu.kanade.tachiyomi.data.database.tables

object MangaAggregateTable {
    const val TABLE = "manga_aggregate"

    const val COL_MANGA_ID = "manga_id"

    const val COL_VOLUMES = "volumes"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_MANGA_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_VOLUMES TEXT NOT NULL,
            FOREIGN KEY($COL_MANGA_ID) REFERENCES ${MangaTable.TABLE} (${MangaTable.COL_ID})
            ON DELETE CASCADE
            )
            """
}
