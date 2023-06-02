package eu.kanade.tachiyomi.data.database.tables

object CustomListTable {

    const val TABLE = "customlist"

    const val COL_ID = "_id"

    const val COL_NAME = "name"

    const val COL_UUID = "uuid"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_NAME TEXT NOT NULL,
            $COL_UUID TEXT NOT NULL,
            UNIQUE ($COL_UUID) ON CONFLICT REPLACE
            )
            """
}
