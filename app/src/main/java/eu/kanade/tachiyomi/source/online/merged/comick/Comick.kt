package eu.kanade.tachiyomi.source.online.merged.comick

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
// Import getComickFilters instead of getSorts for FilterList initialization
import eu.kanade.tachiyomi.source.online.merged.comick.getComickFilters
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class Comick(override val lang: String = "en") : HttpSource() {

    override val name = "Comick"
    override val baseUrl = ComickApi.COMICK_URL
    override val supportsLatest = true

    private val json: Json by injectLazy()
    private val comickApi = ComickApi(client, lang)

    override suspend fun fetchPopularManga(page: Int): MangasPage {
        val filters = getComickFilters() // Use default filters
        val popularSort = getSortType(filters)
        val genre = getGenre(filters)
        val demographic = getDemographic(filters)
        val completed = getCompleted(filters)
        try {
            val mangaList = comickApi.getTopComics(popularSort, "all", page, genre, demographic, completed)
            return MangasPage(
                mangas = mangaList.map { manga ->
                    SManga.create().apply {
                        title = manga.title
                        thumbnail_url = manga.mdCovers.firstOrNull()?.b2key ?: manga.mdCovers.firstOrNull()?.gpurl
                        url = getUrlWithoutDomain(comickApi.baseUrl + "/comic/" + manga.slug)
                        // Not enough details from this endpoint for full SManga object
                        // Consider fetching full details if necessary, or mark as initialized = false
                    }
                },
                hasNextPage = mangaList.size >= 20, // Assuming page size of 20
            )
        } catch (e: Exception) {
            // Handle error (e.g., log, return empty MangasPage)
            return MangasPage(emptyList(), false)
        }
    }

    override suspend fun fetchLatestUpdates(page: Int): MangasPage {
        val filters = FilterList() // No specific filters for latest updates
        try {
            val mangaList = comickApi.getTopComics("recent", "all", page, getGenre(filters), getDemographic(filters), getCompleted(filters))
            return MangasPage(
                mangas = mangaList.map { manga ->
                    SManga.create().apply {
                        title = manga.title
                        thumbnail_url = manga.mdCovers.firstOrNull()?.b2key ?: manga.mdCovers.firstOrNull()?.gpurl
                        url = getUrlWithoutDomain(comickApi.baseUrl + "/comic/" + manga.slug)
                    }
                },
                hasNextPage = mangaList.size >= 20,
            )
        } catch (e: Exception) {
            return MangasPage(emptyList(), false)
        }
    }

    override suspend fun fetchSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        val popularSort = getSortType(filters)
        val genre = getGenre(filters)
        val demographic = getDemographic(filters)
        val completed = getCompleted(filters)
        try {
            val mangaList = comickApi.search(query, page, genre, demographic, completed)
            return MangasPage(
                mangas = mangaList.map { manga ->
                    SManga.create().apply {
                        title = manga.title
                        thumbnail_url = manga.mdCovers.firstOrNull()?.b2key ?: manga.mdCovers.firstOrNull()?.gpurl
                        url = getUrlWithoutDomain(comickApi.baseUrl + "/comic/" + manga.slug)
                    }
                },
                hasNextPage = mangaList.size >= 20,
            )
        } catch (e: Exception) {
            return MangasPage(emptyList(), false)
        }
    }

    override suspend fun fetchMangaDetails(manga: SManga): SManga {
        val slug = getHid(manga.url) // Assuming URL is the slug or contains slug
        try {
            val comic = comickApi.getComic(slug)
            return SManga.create().apply {
                title = comic.title
                thumbnail_url = comic.mdcovers?.firstOrNull()?.b2key ?: comic.mainCover
                description = comic.desc
                author = comic.authors.joinToString { it.name }
                artist = comic.artists.joinToString { it.name }
                genre = comic.genres.joinToString()
                status = comic.status ?: SManga.UNKNOWN
                initialized = true // Mark as initialized after fetching details
            }
        } catch (e: Exception) {
            // Handle error, return original manga or throw
            return manga
        }
    }

    override suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        val comicHid = getHid(manga.url)
        var page = 1
        val chapters = mutableListOf<ComickChapter>()
        try {
            do {
                val chapterList = comickApi.getChapterList(comicHid, page)
                chapters.addAll(chapterList.chapters)
                page++
            } while (chapters.size < chapterList.total)
            return chapters.map { chapter ->
                SChapter.create().apply {
                    name = chapter.title ?: "Chapter ${chapter.chap ?: chapter.id}"
                    url = getUrlWithoutDomain(comickApi.baseUrl + "/chapter/" + chapter.hid)
                    date_upload = chapter.createdAt?.let { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(it)?.time } ?: 0L
                    chapter_number = chapter.chap?.toFloatOrNull() ?: -1f
                    scanlator = chapter.groupName?.joinToString()
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override suspend fun fetchPageList(chapter: SChapter): List<Page> {
        val chapterHid = getHid(chapter.url)
        try {
            return comickApi.getPageList(chapterHid)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    override fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException("Use fetchPopularManga instead.")
    override fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException("Use fetchPopularManga instead.")
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Use fetchLatestUpdates instead.")
    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException("Use fetchLatestUpdates instead.")
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException("Use fetchSearchManga instead.")
    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException("Use fetchSearchManga instead.")
    override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException("Use fetchMangaDetails instead.")
    override fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException("Use fetchChapterList instead.")
    override fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException("Use fetchPageList instead.")

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used")

    override fun getFilterList() = getComickFilters()

    private fun getSortType(filters: FilterList): String {
        return filters.filterIsInstance<SortFilter>().firstOrNull()?.toUriPart() ?: "popular"
    }

    private fun getGenre(filters: FilterList): String {
        return filters.filterIsInstance<GenreGroup>().firstOrNull()?.state
            ?.filter { it.state }
            ?.joinToString(",") { it.id }
            ?: "all"
    }

    private fun getDemographic(filters: FilterList): String {
        return filters.filterIsInstance<DemographicGroup>().firstOrNull()?.state
            ?.filter { it.state }
            ?.joinToString(",") { it.id }
            ?: "all"
    }

    private fun getCompleted(filters: FilterList): Boolean? {
        return filters.filterIsInstance<CompletedFilter>().firstOrNull()?.state
    }
    companion object {
        private val MDcovers.gpurl: String?
            get() {
                return this.b2key?.let { "https://meo.comick.pictures/$it" }
            }
    }
}
