package eu.kanade.tachiyomi.data.database.mappers

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getIntOrNull
import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping
import com.pushtorefresh.storio.sqlite.operations.delete.DefaultDeleteResolver
import com.pushtorefresh.storio.sqlite.operations.get.DefaultGetResolver
import com.pushtorefresh.storio.sqlite.operations.put.DefaultPutResolver
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery
import com.pushtorefresh.storio.sqlite.queries.InsertQuery
import com.pushtorefresh.storio.sqlite.queries.UpdateQuery
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_ANILIST_ID
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_ANIME_PLANET_ID
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_ARTIST
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_AUTHOR
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_CHAPTER_FLAGS
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_DATE_ADDED
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_DESCRIPTION
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_FAVORITE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_FOLLOW_STATUS
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_GENRE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_ID
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_INITIALIZED
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_KITSU_ID
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_LANG_FLAG
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_LAST_UPDATE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_MANGA_LAST_CHAPTER
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_MANGA_UPDATES_ID
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_MERGE_MANGA_IMAGE_URL
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_MERGE_MANGA_URL
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_MISSING_CHAPTERS
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_MY_ANIME_LIST_ID
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_NEXT_UPDATE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_RATING
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_SCANLATOR_FILTER_FLAG
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_SOURCE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_STATUS
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_THUMBNAIL_URL
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_TITLE
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_URL
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_USERS
import eu.kanade.tachiyomi.data.database.tables.MangaTable.COL_VIEWER
import eu.kanade.tachiyomi.data.database.tables.MangaTable.TABLE
import eu.kanade.tachiyomi.source.online.utils.FollowStatus

class MangaTypeMapping : SQLiteTypeMapping<Manga>(
    MangaPutResolver(),
    MangaGetResolver(),
    MangaDeleteResolver()
)

class MangaPutResolver : DefaultPutResolver<Manga>() {

    override fun mapToInsertQuery(obj: Manga) = InsertQuery.builder()
        .table(TABLE)
        .build()

    override fun mapToUpdateQuery(obj: Manga) = UpdateQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()

    override fun mapToContentValues(obj: Manga) = ContentValues(16).apply {
        put(COL_ID, obj.id)
        put(COL_SOURCE, obj.source)
        put(COL_URL, obj.url)
        put(COL_ARTIST, obj.artist)
        put(COL_AUTHOR, obj.author)
        put(COL_DESCRIPTION, obj.description)
        put(COL_GENRE, obj.genre)
        put(COL_TITLE, obj.title)
        put(COL_STATUS, obj.status)
        put(COL_THUMBNAIL_URL, obj.thumbnail_url)
        put(COL_FAVORITE, obj.favorite)
        put(COL_LAST_UPDATE, obj.last_update)
        put(COL_NEXT_UPDATE, obj.next_update)
        put(COL_INITIALIZED, obj.initialized)
        put(COL_VIEWER, obj.viewer_flags)
        put(COL_CHAPTER_FLAGS, obj.chapter_flags)
        put(COL_DATE_ADDED, obj.date_added)
        put(COL_LANG_FLAG, obj.lang_flag)
        put(COL_FOLLOW_STATUS, obj.follow_status?.int)
        put(COL_ANILIST_ID, obj.anilist_id)
        put(COL_KITSU_ID, obj.kitsu_id)
        put(COL_MY_ANIME_LIST_ID, obj.my_anime_list_id)
        put(COL_MANGA_UPDATES_ID, obj.manga_updates_id)
        put(COL_ANIME_PLANET_ID, obj.anime_planet_id)
        put(COL_SCANLATOR_FILTER_FLAG, obj.scanlator_filter)
        put(COL_MISSING_CHAPTERS, obj.missing_chapters)
        put(COL_RATING, obj.rating)
        put(COL_USERS, obj.users)
        put(COL_MERGE_MANGA_URL, obj.merge_manga_url)
        put(COL_MANGA_LAST_CHAPTER, obj.last_chapter_number)
        put(COL_MERGE_MANGA_IMAGE_URL, obj.merge_manga_image_url)
    }
}

interface BaseMangaGetResolver {
    fun mapBaseFromCursor(manga: Manga, cursor: Cursor) = manga.apply {
        id = cursor.getLong(cursor.getColumnIndex(COL_ID))
        source = cursor.getLong(cursor.getColumnIndex(COL_SOURCE))
        url = cursor.getString(cursor.getColumnIndex(COL_URL))
        artist = cursor.getString(cursor.getColumnIndex(COL_ARTIST))
        author = cursor.getString(cursor.getColumnIndex(COL_AUTHOR))
        description = cursor.getString(cursor.getColumnIndex(COL_DESCRIPTION))
        genre = cursor.getString(cursor.getColumnIndex(COL_GENRE))
        title = cursor.getString(cursor.getColumnIndex(COL_TITLE))
        status = cursor.getInt(cursor.getColumnIndex(COL_STATUS))
        thumbnail_url = cursor.getString(cursor.getColumnIndex(COL_THUMBNAIL_URL))
        favorite = cursor.getInt(cursor.getColumnIndex(COL_FAVORITE)) == 1
        last_update = cursor.getLong(cursor.getColumnIndex(COL_LAST_UPDATE))
        next_update = cursor.getLong(cursor.getColumnIndex(COL_NEXT_UPDATE))
        initialized = cursor.getInt(cursor.getColumnIndex(COL_INITIALIZED)) == 1
        viewer_flags = cursor.getInt(cursor.getColumnIndex(COL_VIEWER))
        chapter_flags = cursor.getInt(cursor.getColumnIndex(COL_CHAPTER_FLAGS))
        date_added = cursor.getLong(cursor.getColumnIndex(COL_DATE_ADDED))
        lang_flag = cursor.getString(cursor.getColumnIndex(COL_LANG_FLAG))
        anilist_id = cursor.getString(cursor.getColumnIndex(COL_ANILIST_ID))
        kitsu_id = cursor.getString(cursor.getColumnIndex(COL_KITSU_ID))
        my_anime_list_id = cursor.getString(cursor.getColumnIndex(COL_MY_ANIME_LIST_ID))
        manga_updates_id = cursor.getString(cursor.getColumnIndex(COL_MANGA_UPDATES_ID))
        anime_planet_id = cursor.getString(cursor.getColumnIndex(COL_ANIME_PLANET_ID))
        scanlator_filter = cursor.getString(cursor.getColumnIndex(COL_SCANLATOR_FILTER_FLAG))
        missing_chapters = cursor.getString(cursor.getColumnIndex(COL_MISSING_CHAPTERS))
        rating = cursor.getString(cursor.getColumnIndex(COL_RATING))
        users = cursor.getString(cursor.getColumnIndex(COL_USERS))
        merge_manga_url = cursor.getString(cursor.getColumnIndex(COL_MERGE_MANGA_URL))
        last_chapter_number = cursor.getIntOrNull(cursor.getColumnIndex(COL_MANGA_LAST_CHAPTER))
        follow_status =
            cursor.getInt(cursor.getColumnIndex(COL_FOLLOW_STATUS)).let { FollowStatus.fromInt(it) }
        merge_manga_image_url = cursor.getString(cursor.getColumnIndex(COL_MERGE_MANGA_IMAGE_URL))
    }
}

open class MangaGetResolver : DefaultGetResolver<Manga>(), BaseMangaGetResolver {

    override fun mapFromCursor(cursor: Cursor): Manga {
        return mapBaseFromCursor(MangaImpl(), cursor)
    }
}

class MangaDeleteResolver : DefaultDeleteResolver<Manga>() {

    override fun mapToDeleteQuery(obj: Manga) = DeleteQuery.builder()
        .table(TABLE)
        .where("$COL_ID = ?")
        .whereArgs(obj.id)
        .build()
}
