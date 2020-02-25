package eu.kanade.tachiyomi.data.database.tables

object MangaTable {

    const val TABLE = "mangas"

    const val COL_ID = "_id"

    const val COL_SOURCE = "source"

    const val COL_URL = "url"

    const val COL_ARTIST = "artist"

    const val COL_AUTHOR = "author"

    const val COL_DESCRIPTION = "description"

    const val COL_GENRE = "genre"

    const val COL_TITLE = "title"

    const val COL_STATUS = "status"

    const val COL_THUMBNAIL_URL = "thumbnail_url"

    const val COL_FAVORITE = "favorite"

    const val COL_LAST_UPDATE = "last_update"

    const val COL_LANG_FLAG = "lang_flag"

    const val COL_DATE_ADDED = "date_added"

    const val COL_INITIALIZED = "initialized"

    const val COL_VIEWER = "viewer"

    const val COL_CHAPTER_FLAGS = "chapter_flags"

    const val COL_FOLLOW_STATUS = "follow_status"

    const val COL_ANILIST_ID = "anilist_id"

    const val COL_KITSU_ID = "kitsu_id"

    const val COL_MY_ANIME_LIST_ID = "my_anime_list_id"

    const val COL_MANGA_UPDATES_ID = "manga_updates_id"

    const val COL_ANIME_PLANET_ID = "anime_planet_id"

    const val COL_UNREAD = "unread"

    const val COL_CATEGORY = "category"

    val createTableQuery: String
        get() = """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_SOURCE INTEGER NOT NULL,
            $COL_URL TEXT NOT NULL,
            $COL_ARTIST TEXT,
            $COL_AUTHOR TEXT,
            $COL_DESCRIPTION TEXT,
            $COL_LANG_FLAG TEXT,
            $COL_ANILIST_ID TEXT,
            $COL_KITSU_ID TEXT,
            $COL_MY_ANIME_LIST_ID TEXT,
            $COL_ANIME_PLANET_ID TEXT,
            $COL_MANGA_UPDATES_ID TEXT,
            $COL_GENRE TEXT,
            $COL_TITLE TEXT NOT NULL,
            $COL_STATUS INTEGER NOT NULL,
            $COL_THUMBNAIL_URL TEXT,
            $COL_FAVORITE INTEGER NOT NULL,
            $COL_DATE_ADDED LONG,
            $COL_LAST_UPDATE LONG,
            $COL_INITIALIZED BOOLEAN NOT NULL,
            $COL_VIEWER INTEGER NOT NULL,
            $COL_CHAPTER_FLAGS INTEGER NOT NULL,
            $COL_FOLLOW_STATUS INTEGER
            )"""

    val createUrlIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_${COL_URL}_index ON $TABLE($COL_URL)"

    val createLibraryIndexQuery: String
        get() = "CREATE INDEX library_${COL_FAVORITE}_index ON $TABLE($COL_FAVORITE) " +
                "WHERE $COL_FAVORITE = 1"

    val addLangFlagCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_LANG_FLAG} TEXT DEFAULT NULL"

    val addDateAddedCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_DATE_ADDED} LONG DEFAULT 0"

    val addFollowStatusCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_FOLLOW_STATUS} INT DEFAULT NULL"

    val addAnilistIdCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_ANILIST_ID} TEXT DEFAULT NULL"

    val addKitsuIdCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_KITSU_ID} TEXT DEFAULT NULL"

    val addMyAnimeListIdCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_MY_ANIME_LIST_ID} TEXT DEFAULT NULL"

    val addMangaUpdatesIdCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_MANGA_UPDATES_ID} TEXT DEFAULT NULL"

    val addAnimePlanetIdCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_ANIME_PLANET_ID} TEXT DEFAULT NULL"
}
