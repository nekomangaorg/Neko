package eu.kanade.tachiyomi.v5.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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

    val idDb: SQLiteDatabase by lazy { openDatabase(context, "mangadex.db") }

    val dbCovers: SQLiteDatabase by lazy { openDatabase(context, "cover.db") }

    fun openDatabase(context: Context, dbPath: String): SQLiteDatabase {
        val dbFile: File = context.getDatabasePath(dbPath)
        if (!dbFile.exists()) {
            try {
                copyDatabase(context, dbFile, dbPath)
            } catch (e: IOException) {
                throw RuntimeException("Error creating source database", e)
            }
        }
        return SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    @Throws(IOException::class)
    private fun copyDatabase(context: Context, dbFile: File, dbPath: String) {
        val `is`: InputStream = context.getAssets().open(dbPath)
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
