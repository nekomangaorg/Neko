package eu.kanade.tachiyomi.data.database.tables

object CachedMangaTable {

    const val TABLE_FTS = "cached_manga_fts"

    const val COL_MANGA_ID = "manga_id"
    
    const val COL_MANGA_TITLE = "manga_title"

    val createVirtualTableQuery: String
        get() =
            """CREATE VIRTUAL TABLE $TABLE_FTS
               USING fts5($COL_MANGA_ID, $COL_MANGA_TITLE)"""
}
