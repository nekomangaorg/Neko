package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.data.database.models.SourceArtwork
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Response

/** A basic interface for creating a source. It could be an online source, a local source, etc... */
interface Source {

    /** Id for the source. Must be unique. */
    val id: Long

    /** Name of the source. */
    val name: String

    /**
     * Returns an observable with the list of pages a chapter has.
     *
     * @param chapter the chapter.
     */
    suspend fun getPageList(chapter: SChapter): List<Page>

    suspend fun getImage(page: Page): Response
}

data class MangaDetailChapterInformation(
    val sManga: SManga? = null,
    val sourceArtwork: List<SourceArtwork> = emptyList(),
    val sChapters: List<SChapter>,
)
