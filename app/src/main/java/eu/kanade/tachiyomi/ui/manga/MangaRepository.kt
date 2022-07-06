package eu.kanade.tachiyomi.ui.manga

import androidx.core.text.isDigitsOnly
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import eu.kanade.tachiyomi.source.online.MergeSource
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.executeOnIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class MangaRepository {
    private val db: DatabaseHelper by injectLazy()
    private val preferences: PreferencesHelper by injectLazy()
    private val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by lazy { Injekt.get() }
    private val mangaDex: MangaDex by lazy { sourceManager.getMangadex() }
    private val mergedSource: MergeSource by lazy { sourceManager.getMergeSource() }

    suspend fun update(manga: Manga, isOnline: Boolean) = flow<MangaResult> {
        var errorFromNetwork: Throwable? = null
        var errorFromMerged: Throwable? = null

        if (!isOnline) {
            emit(MangaResult.Error(R.string.no_network_connection))
            return@flow
        }
        if (!mangaDex.checkIfUp()) {
            emit(MangaResult.Error(R.string.site_down))
            return@flow
        }

        val mangaUUID = MdUtil.getMangaId(manga.url)

        if (mangaUUID.isDigitsOnly()) {
            emit(MangaResult.Error(R.string.v3_manga))
            return@flow
        }

        XLog.d("Begin processing manga/chapter update for manga $mangaUUID")

        val resultingManga = runCatching {
            mangaDex.getMangaDetails(manga)
        }.getOrElse { e ->
            XLog.e("error with mangadex getting manga", e)
            errorFromNetwork = e
            null
        }

        val mangaWasInitialized = manga.initialized
        val originalThumbnail = manga.thumbnail_url

        resultingManga?.let { networkManga ->
            manga.copyFrom(networkManga)
            manga.initialized = true
            if (networkManga.thumbnail_url != null && networkManga.thumbnail_url != originalThumbnail) {
                coverCache.deleteFromCache(originalThumbnail, manga.favorite)
            }
            db.insertManga(manga).executeOnIO()
            emit(MangaResult.UpdatedManga)

        }

        emit(MangaResult.Success)

        /*
         val deferredChapters = withIOContext {
             runCatching {
                 mangaDex.fetchChapterList(manga)
             }.getOrElse { e ->
                 XLog.e("error with mangadex getting chapters", e)
                 errorFromNetwork = e
                 emptyList()
             }
         }

         val deferredMergedChapters = withIOContext {
             if (manga.isMerged()) {
                 kotlin.runCatching {
                     sourceManager.getMergeSource()
                         .fetchChapters(manga.merge_manga_url!!)
                 }.getOrElse { e ->
                     XLog.e("error with mergedsource", e)
                     errorFromMerged = e
                     emptyList()
                 }
             } else {
                 emptyList()
             }
         }*/

    }.flowOn(Dispatchers.IO)
}

sealed class MangaResult {
    class Error(val id: Int? = null, val text: String = "") : MangaResult()
    object UpdatedManga : MangaResult()
    object Success : MangaResult()
}

