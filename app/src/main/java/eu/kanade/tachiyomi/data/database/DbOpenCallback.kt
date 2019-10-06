package eu.kanade.tachiyomi.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import eu.kanade.tachiyomi.data.database.tables.*

class DbOpenCallback : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

    companion object {
        /**
         * Name of the database file.
         */
        const val DATABASE_NAME = "tachiyomi.db"

        /**
         * Version of the database.
         */
        const val DATABASE_VERSION = 10
    }

    override fun onCreate(db: SupportSQLiteDatabase) = with(db) {
        execSQL(MangaTable.createTableQuery)
        execSQL(ChapterTable.createTableQuery)
        execSQL(TrackTable.createTableQuery)
        execSQL(CategoryTable.createTableQuery)
        execSQL(MangaCategoryTable.createTableQuery)
        execSQL(HistoryTable.createTableQuery)

        // DB indexes
        execSQL(MangaTable.createUrlIndexQuery)
        execSQL(MangaTable.createLibraryIndexQuery)
        execSQL(ChapterTable.createMangaIdIndexQuery)
        execSQL(ChapterTable.createUnreadChaptersIndexQuery)
        execSQL(HistoryTable.createChapterIdIndexQuery)
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {

        if (oldVersion < 9) {
            db.execSQL(MangaTable.addLangFlagCol)
            db.execSQL(MangaTable.addDateAddedCol)
        }
        if (oldVersion < 10) {
            db.execSQL(MangaTable.addFollowStatusCol)
        }
    }

    override fun onConfigure(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

}
