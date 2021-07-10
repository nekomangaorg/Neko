package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.dto.ChapterDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.system.logTimeTaken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.Request
import uy.kohesive.injekt.injectLazy

class MangaHandler {
    val network: NetworkHelper by injectLazy()
    val filterHandler: FilterHandler by injectLazy()
    val preferencesHelper: PreferencesHelper by injectLazy()
    val apiMangaParser: ApiMangaParser by injectLazy()

    suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        XLog.d("fetch manga and chapter details")

        return withContext(Dispatchers.IO) {
            logTimeTaken("Chapter and Manga Details for  ${manga.title}") {
                val chapterList = async {
                    fetchChapterList(manga)
                }
                val detailsManga = async {
                    fetchMangaDetails(manga)
                }

                manga.copyFrom(detailsManga.await())

                Pair(
                    manga,
                    chapterList.await()
                )
            }
        }
    }

    suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return withContext(Dispatchers.IO) {
            val response = network.service.viewChapter(urlChapterId)
            response.body()!!.relationships.first { it.type == MdConstants.Types.manga }.id
        }
    }

    suspend fun fetchMangaDetails(manga: SManga): SManga {
        return withContext(Dispatchers.IO) {
            logTimeTaken("Manga Detail for  ${manga.title}") {
                val response = network.service.viewManga(MdUtil.getMangaId(manga.url))
                if (response.isSuccessful.not()) {
                    throw(
                        Exception(
                            "Error from MangaDex ${response.code()} error body: ${
                            response.errorBody()?.string()
                            }"
                        )
                        )
                }
                apiMangaParser.mangaDetailsParse(response.body()!!)
            }
        }
    }

    suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return withContext(Dispatchers.IO) {
            logTimeTaken("Fetch Chapters for  ${manga.title}") {
                val langs = MdUtil.getLangsToShow(preferencesHelper)

                val response = logTimeTaken("fetching chapters from Dex") {
                    network.service.viewChapters(MdUtil.getMangaId(manga.url), langs, 0)
                }

                if (response.isSuccessful.not()) {
                    XLog.e("error", response.errorBody()!!.string())
                    throw Exception("error returned from MangaDex.  Http code : ${response.code()}")
                }

                val chapterListDto = response.body()!!
                val results = chapterListDto.results.toMutableList()

                var hasMoreResults =
                    chapterListDto.limit + chapterListDto.offset < chapterListDto.total

                var offset = chapterListDto.offset
                val limit = chapterListDto.limit

                while (hasMoreResults) {
                    offset += limit
                    val newResponse =
                        network.service.viewChapters(MdUtil.getMangaId(manga.url), langs, offset)

                    hasMoreResults = if (newResponse.code() != 200) {
                        false
                    } else {
                        val newChapterListDto = newResponse.body()!!
                        results.addAll(newChapterListDto.results)
                        newChapterListDto.limit + newChapterListDto.offset < newChapterListDto.total
                    }
                }

                val groupMap = getGroupMap(results)

                apiMangaParser.chapterListParse(results, groupMap)
            }
        }
    }

    private fun getGroupMap(results: List<ChapterDto>): Map<String, String> {
        return results.map { chapter -> chapter.relationships }
            .flatten()
            .filter { it.type == MdConstants.Types.scanlator }
            .map { it.id to it.attributes!!.name!! }
            .toMap()
    }

    private fun groupIdRequest(id: List<String>, offset: Int): Request {
        val urlSuffix = id.joinToString("&ids[]=", "?limit=100&offset=$offset&ids[]=")
        return GET(MdUtil.groupUrl + urlSuffix, network.headers)
    }
}
