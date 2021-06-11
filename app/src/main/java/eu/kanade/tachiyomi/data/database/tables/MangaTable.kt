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

    const val COL_NEXT_UPDATE = "next_update"

    const val COL_INITIALIZED = "initialized"

    const val COL_VIEWER = "viewer"

    const val COL_CHAPTER_FLAGS = "chapter_flags"

    const val COL_UNREAD = "unread"

    const val COL_HAS_READ = "has_read"

    const val COL_CATEGORY = "category"

    const val COL_DATE_ADDED = "date_added"

    const val COL_LANG_FLAG = "lang_flag"

    const val COL_FOLLOW_STATUS = "follow_status"

    const val COL_ANILIST_ID = "anilist_id"

    const val COL_KITSU_ID = "kitsu_id"

    const val COL_MY_ANIME_LIST_ID = "my_anime_list_id"

    const val COL_MANGA_UPDATES_ID = "manga_updates_id"

    const val COL_ANIME_PLANET_ID = "anime_planet_id"

    const val COL_SCANLATOR_FILTER_FLAG = "scanlator_filter_flag"

    const val COL_MISSING_CHAPTERS = "missing_chapters"

    const val COL_RATING = "rating"

    const val COL_USERS = "users"

    const val COL_MERGE_MANGA_URL = "merge_manga_url"

    const val COL_MANGA_LAST_CHAPTER = "manga_last_chapter"

    const val COL_MERGE_MANGA_IMAGE_URL = "merge_manga_image_url"

    val createTableQuery: String
        get() =
            """CREATE TABLE $TABLE(
            $COL_ID INTEGER NOT NULL PRIMARY KEY,
            $COL_SOURCE INTEGER NOT NULL,
            $COL_URL TEXT NOT NULL,
            $COL_ARTIST TEXT,
            $COL_AUTHOR TEXT,
            $COL_DESCRIPTION TEXT,
            $COL_GENRE TEXT,
            $COL_TITLE TEXT NOT NULL,
            $COL_STATUS INTEGER NOT NULL,
            $COL_THUMBNAIL_URL TEXT,
            $COL_FAVORITE INTEGER NOT NULL,
            $COL_LAST_UPDATE LONG,
            $COL_NEXT_UPDATE LONG,
            $COL_INITIALIZED BOOLEAN NOT NULL,
            $COL_VIEWER INTEGER NOT NULL,
            $COL_CHAPTER_FLAGS INTEGER NOT NULL,
            $COL_DATE_ADDED LONG,
            $COL_LANG_FLAG TEXT,
            $COL_ANILIST_ID TEXT,
            $COL_KITSU_ID TEXT,
            $COL_MY_ANIME_LIST_ID TEXT,
            $COL_ANIME_PLANET_ID TEXT,
            $COL_MANGA_UPDATES_ID TEXT,
            $COL_SCANLATOR_FILTER_FLAG TEXT,
            $COL_MISSING_CHAPTERS TEXT,
            $COL_RATING TEXT,
            $COL_USERS TEXT,
            $COL_MERGE_MANGA_URL TEXT,
            $COL_MERGE_MANGA_IMAGE_URL TEXT,
            $COL_MANGA_LAST_CHAPTER INTEGER,
            $COL_FOLLOW_STATUS INTEGER
            )"""

    val createUrlIndexQuery: String
        get() = "CREATE INDEX ${TABLE}_${COL_URL}_index ON $TABLE($COL_URL)"

    val createLibraryIndexQuery: String
        get() = "CREATE INDEX library_${COL_FAVORITE}_index ON $TABLE($COL_FAVORITE) " +
            "WHERE $COL_FAVORITE = 1"

    val addDateAddedCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_DATE_ADDED} LONG DEFAULT 0"

    val addNextUpdateCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_NEXT_UPDATE} LONG DEFAULT 0"

    val addLangFlagCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_LANG_FLAG} TEXT DEFAULT NULL"

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

    val addScanlatorFilterFlagCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_SCANLATOR_FILTER_FLAG} TEXT DEFAULT NULL"

    val addMissingChaptersCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_MISSING_CHAPTERS} TEXT DEFAULT NULL"

    val addRatingCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_RATING} TEXT DEFAULT NULL"

    val addUsersCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_USERS} TEXT DEFAULT NULL"

    val addMergeMangaCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_MERGE_MANGA_URL} TEXT DEFAULT NULL"

    val addMangaLastChapter: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_MANGA_LAST_CHAPTER} INTEGER DEFAULT NULL"

    val addMergeMangaImageCol: String
        get() = "ALTER TABLE ${MangaTable.TABLE} ADD COLUMN ${MangaTable.COL_MERGE_MANGA_IMAGE_URL} TEXT DEFAULT NULL"
}
