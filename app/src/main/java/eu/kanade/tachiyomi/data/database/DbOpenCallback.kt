package eu.kanade.tachiyomi.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import eu.kanade.tachiyomi.data.database.tables.ArtworkTable
import eu.kanade.tachiyomi.data.database.tables.CachedMangaTable
import eu.kanade.tachiyomi.data.database.tables.CategoryTable
import eu.kanade.tachiyomi.data.database.tables.ChapterTable
import eu.kanade.tachiyomi.data.database.tables.HistoryTable
import eu.kanade.tachiyomi.data.database.tables.MangaCategoryTable
import eu.kanade.tachiyomi.data.database.tables.MangaTable
import eu.kanade.tachiyomi.data.database.tables.ScanlatorTable
import eu.kanade.tachiyomi.data.database.tables.SimilarTable
import eu.kanade.tachiyomi.data.database.tables.TrackTable

class DbOpenCallback : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

    companion object {
        /**
         * Name of the database file.
         */
        const val DATABASE_NAME = "tachiyomi.db"

        /**
         * Version of the database.
         */
        const val DATABASE_VERSION = 33
    }

    override fun onCreate(db: SupportSQLiteDatabase) = with(db) {
        execSQL(MangaTable.createTableQuery)
        execSQL(ChapterTable.createTableQuery)
        execSQL(TrackTable.createTableQuery)
        execSQL(CategoryTable.createTableQuery)
        execSQL(MangaCategoryTable.createTableQuery)
        execSQL(HistoryTable.createTableQuery)
        execSQL(SimilarTable.createTableQuery)
        execSQL(ArtworkTable.createTableQuery)
        execSQL(ScanlatorTable.createTableQuery)

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
        if (oldVersion < 11) {
            db.execSQL(MangaTable.addAnilistIdCol)
            db.execSQL(MangaTable.addKitsuIdCol)
            db.execSQL(MangaTable.addMyAnimeListIdCol)
            db.execSQL(MangaTable.addAnimePlanetIdCol)
            db.execSQL(MangaTable.addMangaUpdatesIdCol)
        }
        if (oldVersion < 12) {
            db.execSQL(SimilarTable.createTableQuery)
        }
        if (oldVersion < 13) {
            db.execSQL(CategoryTable.addMangaOrder)
            db.execSQL(ChapterTable.pagesLeftQuery)
        }
        if (oldVersion < 14) {
            db.execSQL(ChapterTable.addChapterCol)
            db.execSQL(ChapterTable.addChapterTitleCol)
            db.execSQL(ChapterTable.addVolumeCol)
        }
        if (oldVersion < 15) {
            db.execSQL(MangaTable.addScanlatorFilterFlagCol)
        }
        if (oldVersion < 16) {
            db.execSQL(MangaTable.addMissingChaptersCol)
        }
        if (oldVersion < 17) {
            db.execSQL(ChapterTable.addMangaDexChapterId)
        }
        if (oldVersion < 18) {
            db.execSQL(ChapterTable.addLanguage)
        }

        if (oldVersion < 20) {
            db.execSQL(MangaTable.addRatingCol)
            db.execSQL(MangaTable.addUsersCol)
        }
        if (oldVersion < 21) {
            db.execSQL(MangaTable.addMergeMangaCol)
            db.execSQL(MangaTable.addMangaLastChapter)
        }
        if (oldVersion < 22) {
            db.execSQL(MangaTable.addNextUpdateCol)
        }
        if (oldVersion < 23) {
            db.execSQL(MangaTable.addMergeMangaImageCol)
        }
        if (oldVersion < 24) {
            db.execSQL(CachedMangaTable.createVirtualTableQuery)
        }
        if (oldVersion < 26) {
            db.execSQL(ChapterTable.addOldMangaDexChapterId)
            db.execSQL(SimilarTable.dropTableQuery)
            db.execSQL(SimilarTable.createTableQuery)
            db.execSQL(SimilarTable.createMangaIdIndexQuery)
            db.execSQL(CachedMangaTable.dropVirtualTableQuery)
            db.execSQL(CachedMangaTable.createVirtualTableQuery)
        }
        if (oldVersion < 27) {
            db.execSQL(TrackTable.addStartDate)
            db.execSQL(TrackTable.addFinishDate)
        }

        if (oldVersion < 28) {
            db.execSQL(MangaTable.addOtherUrlsCol)
            db.execSQL(MangaTable.clearScanlators)
        }

        if (oldVersion < 29) {
            db.execSQL(CachedMangaTable.dropVirtualTableQuery)
            db.execSQL(SimilarTable.dropTableQuery)
            db.execSQL(SimilarTable.createTableQuery)
        }

        if (oldVersion < 30) {
            db.execSQL(TrackTable.renameTableToTemp)
            db.execSQL(TrackTable.createTableQuery)
            db.execSQL(TrackTable.insertFromTempTable)
            db.execSQL(TrackTable.dropTempTable)
        }

        if (oldVersion < 31) {
            db.execSQL(MangaTable.addAltTitles)
            db.execSQL(MangaTable.addUserCover)
            db.execSQL(MangaTable.addUserTitle)
            db.execSQL(ArtworkTable.createTableQuery)
        }

        if (oldVersion < 32) {
            db.execSQL(MangaTable.addLanguageFilterFlag)
        }

        if (oldVersion < 33) {
            db.execSQL(ScanlatorTable.createTableQuery)
        }
    }

    override fun onConfigure(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
        setPragma(db, "foreign_keys = ON")
        setPragma(db, "journal_mode = WAL")
        setPragma(db, "synchronous = NORMAL")
    }

    private fun setPragma(db: SupportSQLiteDatabase, pragma: String) {
        val cursor = db.query("PRAGMA $pragma")
        cursor.moveToFirst()
        cursor.close()
    }
}
