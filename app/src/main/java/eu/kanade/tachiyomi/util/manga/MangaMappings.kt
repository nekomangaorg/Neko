package eu.kanade.tachiyomi.util.manga

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
class MangaMappings(context: Context) {

    val dbMappings: SQLiteDatabase by lazy { openDatabase(context, "20220918_mappings.db") }

    private fun openDatabase(context: Context, dbPath: String): SQLiteDatabase {
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
        val `is`: InputStream = context.assets.open(dbPath)
        val os: OutputStream = FileOutputStream(dbFile)
        val buffer = ByteArray(1024)
        while (`is`.read(buffer) > 0) {
            os.write(buffer)
        }
        os.flush()
        os.close()
        `is`.close()
    }

    fun getMangadexID(id: String, service: String): String? {
        if (!dbMappings.isOpen) {
            return null
        }
        val queryString = "SELECT mdex FROM mappings WHERE ${service.lowercase()} = ? LIMIT 1"
        val whereArgs = arrayOf(id)
        val cursor = dbMappings.rawQuery(queryString, whereArgs) ?: return ""
        if (cursor.moveToFirst()) {
            return cursor.getString(0)
        }
        cursor.close()
        return null
    }

    fun getExternalID(id: String, service: String): String? {
        if (!dbMappings.isOpen) {
            return null
        }
        val queryString = "SELECT ${service.lowercase()} FROM mappings WHERE mdex = ? LIMIT 1"
        val whereArgs = arrayOf(id)
        val cursor = dbMappings.rawQuery(queryString, whereArgs) ?: return ""
        if (cursor.moveToFirst()) {
            return cursor.getString(0)
        }
        cursor.close()
        return null
    }
}
