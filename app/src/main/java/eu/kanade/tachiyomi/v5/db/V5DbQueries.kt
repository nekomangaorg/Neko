package eu.kanade.tachiyomi.v5.db

import android.database.sqlite.SQLiteDatabase

class V5DbQueries {

    companion object {

        fun getNewMangaId(db: SQLiteDatabase, id: String): String {
            if (!db.isOpen) {
                return ""
            }
            val queryString = "SELECT new_id FROM manga WHERE legacy_id = ? LIMIT 1"
            val whereArgs = arrayOf(id)
            val cursor = db.rawQuery(queryString, whereArgs) ?: return ""
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
            cursor.close()
            return ""
        }

        fun getNewChapterId(db: SQLiteDatabase, id: String): String {
            if (!db.isOpen) {
                return ""
            }
            val queryString = "SELECT new_id FROM chapter WHERE legacy_id = ? LIMIT 1"
            val whereArgs = arrayOf(id)
            val cursor = db.rawQuery(queryString, whereArgs) ?: return ""
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
            cursor.close()
            return ""
        }
    }
}

