package eu.kanade.tachiyomi.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite
import eu.kanade.tachiyomi.data.database.mappers.ArtworkTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.CategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.ChapterTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.HistoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.MangaCategoryTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.MangaTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.ScanlatorTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.SearchMetadataTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.SimilarTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.TrackTypeMapping
import eu.kanade.tachiyomi.data.database.models.ArtworkImpl
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.data.database.models.ScanlatorImpl
import eu.kanade.tachiyomi.data.database.models.SearchMetadata
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.queries.ArtworkQueries
import eu.kanade.tachiyomi.data.database.queries.CategoryQueries
import eu.kanade.tachiyomi.data.database.queries.ChapterQueries
import eu.kanade.tachiyomi.data.database.queries.HistoryQueries
import eu.kanade.tachiyomi.data.database.queries.MangaCategoryQueries
import eu.kanade.tachiyomi.data.database.queries.MangaQueries
import eu.kanade.tachiyomi.data.database.queries.ScanlatorQueries
import eu.kanade.tachiyomi.data.database.queries.SearchMetadataQueries
import eu.kanade.tachiyomi.data.database.queries.SimilarQueries
import eu.kanade.tachiyomi.data.database.queries.TrackQueries
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

/**
 * This class provides operations to manage the database through its interfaces.
 */
open class DatabaseHelper(context: Context) :
    ArtworkQueries,
    MangaQueries,
    ChapterQueries,
    TrackQueries,
    CategoryQueries,
    MangaCategoryQueries,
    HistoryQueries,
    SearchMetadataQueries,
    ScanlatorQueries,
    SimilarQueries {

    private val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
        .name(DbOpenCallback.DATABASE_NAME)
        .callback(DbOpenCallback())
        .build()

    override val db = DefaultStorIOSQLite.builder()
        .sqliteOpenHelper(RequerySQLiteOpenHelperFactory().create(configuration))
        .addTypeMapping(Manga::class.java, MangaTypeMapping())
        .addTypeMapping(Chapter::class.java, ChapterTypeMapping())
        .addTypeMapping(Track::class.java, TrackTypeMapping())
        .addTypeMapping(Category::class.java, CategoryTypeMapping())
        .addTypeMapping(MangaCategory::class.java, MangaCategoryTypeMapping())
        .addTypeMapping(SearchMetadata::class.java, SearchMetadataTypeMapping())
        .addTypeMapping(History::class.java, HistoryTypeMapping())
        .addTypeMapping(MangaSimilar::class.java, SimilarTypeMapping())
        .addTypeMapping(ArtworkImpl::class.java, ArtworkTypeMapping())
        .addTypeMapping(ArtworkImpl::class.java, ArtworkTypeMapping())
        .addTypeMapping(ScanlatorImpl::class.java, ScanlatorTypeMapping())
        .build()

    inline fun inTransaction(block: () -> Unit) = db.inTransaction(block)

    inline fun <T> inTransactionReturn(block: () -> T): T = db.inTransactionReturn(block)

    fun lowLevel() = db.lowLevel()
}
