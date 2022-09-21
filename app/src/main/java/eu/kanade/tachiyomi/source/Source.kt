package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.data.database.models.SourceArtwork
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.Logout
import okhttp3.Response

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
     * Returns the list of filters for the source.
     */
    fun getFilterList(): FilterList

    /**
     * Returns an observable with the list of pages a chapter has.
     *
     * @param chapter the chapter.
     */
    suspend fun fetchPageList(chapter: SChapter): List<Page>

    suspend fun fetchImage(page: Page): Response

    fun isLogged(): Boolean

    suspend fun login(username: String, password: String, twoFactorCode: String = ""): Boolean

    suspend fun logout(): Logout
}

data class MangaDetailChapterInformation(
    val sManga: SManga? = null,
    val sourceArtwork: List<SourceArtwork> = emptyList(),
    val sChapters: List<SChapter>,
)
