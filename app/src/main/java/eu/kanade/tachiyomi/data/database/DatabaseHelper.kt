package eu.kanade.tachiyomi.data.database

/** This class provides operations to manage the database through its interfaces. */
import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite
import eu.kanade.tachiyomi.data.database.mappers.MangaAggregateTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.MangaTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.MergeMangaTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.ScanlatorTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.SimilarTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.TrackTypeMapping
import eu.kanade.tachiyomi.data.database.mappers.UploaderTypeMapping
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaAggregate
import eu.kanade.tachiyomi.data.database.models.MangaSimilar
import eu.kanade.tachiyomi.data.database.models.MergeMangaImpl
import eu.kanade.tachiyomi.data.database.models.ScanlatorGroupImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.UploaderImpl
import eu.kanade.tachiyomi.data.database.queries.MangaAggregateQueries
import eu.kanade.tachiyomi.data.database.queries.MangaQueries
import eu.kanade.tachiyomi.data.database.queries.MergeMangaQueries
import eu.kanade.tachiyomi.data.database.queries.ScanlatorGroupQueries
import eu.kanade.tachiyomi.data.database.queries.SimilarQueries
import eu.kanade.tachiyomi.data.database.queries.TrackQueries
import eu.kanade.tachiyomi.data.database.queries.UploaderQueries
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

/** This class provides operations to manage the database through its interfaces. */
open class DatabaseHelper(context: Context) :
    MangaQueries,
    MergeMangaQueries,
    TrackQueries,
    ScanlatorGroupQueries,
    UploaderQueries,
    MangaAggregateQueries,
    SimilarQueries {

    private val configuration =
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DbOpenCallback.DATABASE_NAME)
            .callback(DbOpenCallback())
            .build()

    override val db =
        DefaultStorIOSQLite.builder()
            .sqliteOpenHelper(RequerySQLiteOpenHelperFactory().create(configuration))
            .addTypeMapping(Manga::class.java, MangaTypeMapping())
            .addTypeMapping(Track::class.java, TrackTypeMapping())
            .addTypeMapping(MangaSimilar::class.java, SimilarTypeMapping())
            .addTypeMapping(ScanlatorGroupImpl::class.java, ScanlatorTypeMapping())
            .addTypeMapping(UploaderImpl::class.java, UploaderTypeMapping())
            .addTypeMapping(MergeMangaImpl::class.java, MergeMangaTypeMapping())
            .addTypeMapping(MangaAggregate::class.java, MangaAggregateTypeMapping())
            .build()

    inline fun inTransaction(block: () -> Unit) = db.inTransaction(block)

    inline fun <T> inTransactionReturn(block: () -> T): T = db.inTransactionReturn(block)

    fun lowLevel() = db.lowLevel()
}
