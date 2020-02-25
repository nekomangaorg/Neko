package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import rx.Observable

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
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    val lang: String

    /**
     * Returns an observable containing a page with a list of manga.
     *
     * @param page the page number to retrieve.
     */
    fun fetchPopularManga(page: Int): Observable<MangasPage>

    /**
     * Returns an observable containing a page with a list of manga.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage>

    /**
     * Returns an observable containing a page with a list of users follows.
     *
     * @param page the page number to retrieve.
     */
    fun fetchFollows(page: Int): Observable<MangasPage>

    /**
     * Returns a list of all Follows retrieved by Coroutines
     *
     * @param SManga all smanga found for user
     */
    suspend fun fetchAllFollows(): List<SManga>

    /**
     * Updates reading status
     */
    suspend fun updateReadingProgress(track: Track): Boolean

    /**
     * updates the follow status for a manga
     */
    suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean

    /**
     * Get a MdList Track of the manga
     */
    suspend fun fetchTrackingInfo(manga: SManga): Track

    /**
     * Returns the list of filters for the source.
     */
    fun getFilterList(): FilterList

    /**
     * Returns an observable with the updated details for a manga.
     *
     * @param manga the manga to update.
     */
    fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga>

    /**
     * Returns a updated details for a manga
     *
     * @param manga the manga to update.
     */
    suspend fun fetchMangaDetails(manga: SManga): SManga

    /**
     * Returns an observable with all the relatable for a manga.
     *
     * @param page the page number to retrieve.
     * @param manga the manga to update.
     */
    fun fetchMangaRelatedObservable(page: Int, manga: Manga): Observable<MangasPage>

    /**
     * Returns a updated details for a manga and the chapter list
     *
     * @param manga the manga to update.
     */
    suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>>

    /**
     * Returns an observable with all the available chapters for a manga.
     *
     * @param manga the manga to update.
     */
    fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>>

    /**
     * Returns an observable with all the available chapters for a manga.
     *
     * @param manga the manga to update.
     */
    suspend fun fetchChapterList(manga: SManga): List<SChapter>

    /**
     * Returns an observable with the list of pages a chapter has.
     *
     * @param chapter the chapter.
     */
    fun fetchPageList(chapter: SChapter): Observable<List<Page>>

    fun isLogged(): Boolean

    fun login(username: String, password: String, twoFactorCode: String = ""): Observable<Boolean>

    suspend fun logout(): Boolean
}
