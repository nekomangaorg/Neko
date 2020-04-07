package eu.kanade.tachiyomi.source

import android.graphics.drawable.Drawable
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * A basic interface for creating a source. It could be an online source, a local source, etc...
 */
interface Source {

    /**
     * Id for the source. Must be unique.
     */
    val id: Long

    /**
     * Name of the source.
     */
    val name: String

    /**
     * Returns an observable with the updated details for a manga.
     *
     * @param manga the manga to update.
     */
    fun fetchMangaDetails(manga: SManga): Observable<SManga>

    /**
     * Returns an observable with all the available chapters for a manga.
     *
     * @param manga the manga to update.
     */
    fun fetchChapterList(manga: SManga): Observable<List<SChapter>>

    /**
     * Returns an observable with the list of pages a chapter has.
     *
     * @param chapter the chapter.
     */
    fun fetchPageList(chapter: SChapter): Observable<List<Page>>
}

suspend fun Source.fetchMangaDetailsAsync(manga: SManga): SManga? {
    return withContext(Dispatchers.IO) {
        fetchMangaDetails(manga).toBlocking().single()
    }
}

suspend fun Source.fetchChapterListAsync(manga: SManga): List<SChapter>? {
    return withContext(Dispatchers.IO) {
        fetchChapterList(manga).toBlocking().single()
    }
}

fun Source.icon(): Drawable? =
    Injekt.get<ExtensionManager>().getAppIconForSource(this)
