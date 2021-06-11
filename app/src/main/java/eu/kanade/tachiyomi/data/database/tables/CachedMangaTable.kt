package eu.kanade.tachiyomi.data.database.tables

object CachedMangaTable {

    const val TABLE_FTS = "cached_manga_fts"

    const val COL_MANGA_TITLE = "manga_title"

    const val COL_MANGA_UUID = "manga_uuid"

    const val COL_MANGA_RATING = "manga_rating"

    val dropVirtualTableQuery: String
        get() =
            "DROP TABLE IF EXISTS $TABLE_FTS"

    val createVirtualTableQuery: String
        get() =
            """CREATE VIRTUAL TABLE $TABLE_FTS
               USING fts5($COL_MANGA_TITLE, $COL_MANGA_UUID UNINDEXED, $COL_MANGA_RATING UNINDEXED)"""
}
