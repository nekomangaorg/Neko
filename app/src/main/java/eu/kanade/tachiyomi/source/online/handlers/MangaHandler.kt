package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.dto.ChapterDto
import eu.kanade.tachiyomi.source.online.dto.ChapterListDto
import eu.kanade.tachiyomi.source.online.dto.GroupListDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.CacheControl
import okhttp3.Request
import rx.Observable
import uy.kohesive.injekt.injectLazy

class MangaHandler {
    val network: NetworkHelper by injectLazy()
    val filterHandler: FilterHandler by injectLazy()
    val preferencesHelper: PreferencesHelper by injectLazy()
    val apiMangaParser: ApiMangaParser by injectLazy()

    suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        return withContext(Dispatchers.IO) {
            val detailsManga = async {
                fetchMangaDetails(manga)
            }
            val chapterList = async {
                fetchChapterList(manga)
            }

            manga.copyFrom(detailsManga.await())

            Pair(
                manga,
                chapterList.await()
            )
        }
    }

    suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return withContext(Dispatchers.IO) {
            val request = GET(MdUtil.chapterUrl + urlChapterId)
            val response = network.client.newCall(request).await()
            apiMangaParser.chapterParseForMangaId(response)
        }
    }

    suspend fun fetchMangaDetails(manga: SManga): SManga {
        return withContext(Dispatchers.IO) {
            val response = network.service.viewManga(MdUtil.getMangaId(manga.url))
            if (response.isSuccessful.not()) {
                throw(Exception("Error from MangaDex ${response.code()} error body: ${
                    response.errorBody()?.string()
                }"))
            }
            apiMangaParser.mangaDetailsParse(response.body()!!)
        }
    }

    fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        val langs = MdUtil.getLangsToShow(preferencesHelper)
        return network.client.newCall(mangaFeedRequest(manga, 0, langs))
            .asObservableSuccess()
            .map { response ->

                if (response.code == 204) {
                    return@map emptyList()
                }

                val chapterListResponse =
                    MdUtil.jsonParser.decodeFromString<ChapterListDto>(response.body!!.string())
                val results = chapterListResponse.results.toMutableList()

                var hasMoreResults =
                    chapterListResponse.limit + chapterListResponse.offset < chapterListResponse.total

                var offset = chapterListResponse.offset
                val limit = chapterListResponse.limit

                while (hasMoreResults) {
                    offset += limit
                    val newResponse =
                        network.client.newCall(mangaFeedRequest(manga, offset, langs)).execute()
                    val newChapterListResponse =
                        MdUtil.jsonParser.decodeFromString<ChapterListDto>(newResponse.body!!.string())
                    results.addAll(newChapterListResponse.results)
                    hasMoreResults =
                        newChapterListResponse.limit + newChapterListResponse.offset < newChapterListResponse.total
                }
                val groupIds = results.map { chapter -> chapter.relationships }.flatten()
                    .filter { it.type == "scanlation_group" }.map { it.id }.distinct()

                val groupMap = runCatching {
                    groupIds.chunked(100).mapIndexed { index, ids ->
                        val newResponse =
                            network.client.newCall(groupIdRequest(ids, 100 * index)).execute()
                        val groupList =
                            MdUtil.jsonParser.decodeFromString(GroupListDto.serializer(),
                                newResponse.body!!.string())
                        groupList.results.map { group ->
                            Pair(group.data.id,
                                group.data.attributes.name)
                        }
                    }.flatten().toMap()
                }.getOrNull() ?: emptyMap()

                apiMangaParser.chapterListParse(results, groupMap)
            }
    }

    suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return withContext(Dispatchers.IO) {
            val langs = MdUtil.getLangsToShow(preferencesHelper)

            val response = network.client.newCall(mangaFeedRequest(manga, 0, langs)).await()
            if (response.isSuccessful.not()) {
                XLog.e("error", response.body!!.string())
                throw Exception("error returned from MangaDex.  Http code : ${response.code}")
            }
            if (response.code == 204) {
                return@withContext emptyList()
            }
            val chapterListResponse =
                MdUtil.jsonParser.decodeFromString<ChapterListDto>(response.body!!.string())
            val results = chapterListResponse.results.toMutableList()

            var hasMoreResults =
                chapterListResponse.limit + chapterListResponse.offset < chapterListResponse.total

            var offset = chapterListResponse.offset
            val limit = chapterListResponse.limit

            while (hasMoreResults) {
                offset += limit
                val newResponse =
                    network.client.newCall(mangaFeedRequest(manga, offset, langs)).await()
                if (newResponse.code != 200) {
                    hasMoreResults = false
                } else {
                    val newChapterListResponse =
                        MdUtil.jsonParser.decodeFromString<ChapterListDto>(newResponse.body!!.string())
                    results.addAll(newChapterListResponse.results)
                    hasMoreResults =
                        newChapterListResponse.limit + newChapterListResponse.offset < newChapterListResponse.total
                }
            }

            val groupMap = getGroupMap(results)

            apiMangaParser.chapterListParse(results, groupMap)
        }
    }

    private suspend fun getGroupMap(results: List<ChapterDto>): Map<String, String> {
        val groupIds = results.map { chapter -> chapter.relationships }.flatten()
            .filter { it.type == "scanlation_group" }.map { it.id }.distinct()
        val groupMap = runCatching {
            groupIds.chunked(100).mapIndexed { index, ids ->
                val newResponse = network.client.newCall(groupIdRequest(ids, 100 * index)).await()
                val groupList =
                    MdUtil.jsonParser.decodeFromString<GroupListDto>(newResponse.body!!.string())
                groupList.results.map { group -> Pair(group.data.id, group.data.attributes.name) }
            }.flatten().toMap()
        }.getOrNull() ?: emptyMap()

        return groupMap
    }

    fun fetchRandomMangaId(): Observable<String> {
        return network.client.newCall(randomMangaRequest())
            .asObservableSuccess()
            .map { response ->
                apiMangaParser.randomMangaIdParse(response)
            }
    }

    private fun randomMangaRequest(): Request {
        return GET(MdUtil.randomMangaUrl)
    }

    private fun mangaRequest(manga: SManga): Request {
        return GET(MdUtil.mangaUrl + "/" + MdUtil.getMangaId(manga.url),
            network.headers,
            CacheControl.FORCE_NETWORK)
    }

    private fun mangaFeedRequest(manga: SManga, offset: Int, langs: List<String>): Request {
        return GET(MdUtil.mangaFeedUrl(MdUtil.getMangaId(manga.url), offset, langs),
            network.headers,
            CacheControl.FORCE_NETWORK)
    }

    private fun groupIdRequest(id: List<String>, offset: Int): Request {
        val urlSuffix = id.joinToString("&ids[]=", "?limit=100&offset=$offset&ids[]=")
        return GET(MdUtil.groupUrl + urlSuffix, network.headers)
    }
}
