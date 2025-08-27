package eu.kanade.tachiyomi.data.database.tables

object UploaderTable {

    const val TABLE = "uploader"

    const val COL_ID = "_id"

    const val COL_USERNAME = "username"

    const val COL_UUID = "uuid"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_USERNAME TEXT NOT NULL,
            $COL_UUID TEXT NOT NULL,
            UNIQUE ($COL_UUID) ON CONFLICT REPLACE
            )
            """
}
