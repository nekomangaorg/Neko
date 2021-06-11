package eu.kanade.tachiyomi.data.database.tables

object SimilarTable {

    const val TABLE = "manga_related"

    const val COL_ID = "_id"

    const val COL_MANGA_ID = "manga_id"

    const val COL_MANGA_DATA = "matched_ids"

    val dropTableQuery: String
        get() = "DROP TABLE IF EXISTS $TABLE"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_MANGA_ID TEXT NOT NULL,
            $COL_MANGA_DATA TEXT NOT NULL,
            UNIQUE ($COL_ID) ON CONFLICT REPLACE
            )"""

    val createMangaIdIndexQuery: String
        get() = "CREATE INDEX ${SimilarTable.TABLE}_${SimilarTable.COL_MANGA_ID}_index ON ${SimilarTable.TABLE}(${SimilarTable.COL_MANGA_ID})"
}
