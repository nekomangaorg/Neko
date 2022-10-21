package eu.kanade.tachiyomi.data.database.tables

object BrowseFilterTable {

    const val TABLE = "browse_filter"

    const val COL_ID = "_id"

    const val COL_NAME = "name"

    const val COL_FILTERS = "filters"

    const val COL_DEFAULT = "is_default"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_NAME TEXT NOT NULL,
            $COL_FILTERS TEXT NOT NULL,
            $COL_DEFAULT BOOLEAN NOT NULL
            )
            """
}
