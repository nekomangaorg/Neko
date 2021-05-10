package eu.kanade.tachiyomi.v5.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.work.impl.WorkDatabasePathHelper.getDatabasePath
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite
import eu.kanade.tachiyomi.data.database.DbProvider
import eu.kanade.tachiyomi.data.database.mappers.CacheMangaTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.CategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.ChapterTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.HistoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.MangaCategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.MangaTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.SearchMetadataTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.SimilarTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.TrackTypeMapping
import eu.kanade.tachiyomi.data.database.models.CachedManga
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.data.database.models.SearchMetadata
import eu.kanade.tachiyomi.data.database.models.Track
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * This class provides operations to manage the database through its interfaces.
 * https://gist.github.com/wontondon/1271795
 */
class V5DbHelper(context: Context) {

    val db : SQLiteDatabase = openDatabase(context)

    fun openDatabase(context: Context): SQLiteDatabase {
        val dbFile: File = context.getDatabasePath("mangadex.db")
        if (!dbFile.exists()) {
            try {
                copyDatabase(context, dbFile)
            } catch (e: IOException) {
                throw RuntimeException("Error creating source database", e)
            }
        }
        return SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    @Throws(IOException::class)
    private fun copyDatabase(context: Context, dbFile: File) {
        val `is`: InputStream = context.getAssets().open("mangadex.db")
        val os: OutputStream = FileOutputStream(dbFile)
        val buffer = ByteArray(1024)
        while (`is`.read(buffer) > 0) {
            os.write(buffer)
        }
        os.flush()
        os.close()
        `is`.close()
    }

}
