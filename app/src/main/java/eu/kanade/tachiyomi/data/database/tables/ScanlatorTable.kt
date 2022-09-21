package eu.kanade.tachiyomi.data.database.tables

object ScanlatorTable {

    const val TABLE = "scanlator"

    const val COL_ID = "_id"

    const val COL_NAME = "name"

    const val COL_UUID = "uuid"

    const val COL_DESCRIPTION = "description"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_NAME TEXT NOT NULL,
            $COL_UUID TEXT NOT NULL,
            $COL_DESCRIPTION TEXT,
            UNIQUE ($COL_UUID) ON CONFLICT REPLACE
            )
            """
}
