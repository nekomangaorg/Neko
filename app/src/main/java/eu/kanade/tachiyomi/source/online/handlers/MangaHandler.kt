package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.handlers.serializers.ChapterListResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.ChapterResponse
import eu.kanade.tachiyomi.source.online.handlers.serializers.GroupListResponse
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import rx.Observable

class MangaHandler(val client: OkHttpClient, val headers: Headers, val filterHandler: FilterHandler, private val langs: List<String>, private val forceLatestCovers: Boolean = false) {

    suspend fun fetchMangaAndChapterDetails(manga: SManga): Pair<SManga, List<SChapter>> {
        return withContext(Dispatchers.IO) {

            val response = client.newCall(mangaRequest(manga)).await()
            val covers = getCovers(manga, forceLatestCovers)
            val parser = ApiMangaParser(client, filterHandler)

            val jsonData = response.body!!.string()
            if (response.code != 200) {
                if (response.code == 502) {
                    throw Exception("MangaDex appears to be down, or under heavy load")
                } else {
                    XLog.e("error from MangaDex with response code ${response.code} \n body: \n$jsonData")
                    throw Exception("Error from MangaDex Response code ${response.code} ")
                }
            }

            val detailsManga = parser.mangaDetailsParse(jsonData, covers)
            manga.copyFrom(detailsManga)

            val chapterList = fetchChapterList(manga)

            Pair(
                manga,
                chapterList
            )
        }
    }

    suspend fun getCovers(manga: SManga, forceLatestCovers: Boolean): List<String> {
        /*  if (forceLatestCovers) {
              val response = client.newCall(coverRequest(manga)).await()
              val covers = MdUtil.jsonParser.decodeFromString(ApiCovers.serializer(), response.body!!.string())
              return covers.data.map { it.url }
          } else {*/
        return emptyList<String>()
        //  }
    }

    suspend fun getMangaIdFromChapterId(urlChapterId: String): String {
        return withContext(Dispatchers.IO) {
            val request = GET(MdUtil.chapterUrl + urlChapterId)
            val response = client.newCall(request).await()
            ApiMangaParser(client, filterHandler).chapterParseForMangaId(response)
        }
    }

    suspend fun fetchMangaDetails(manga: SManga): SManga {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(mangaRequest(manga)).await()
            val covers = getCovers(manga, forceLatestCovers)
            ApiMangaParser(client, filterHandler).mangaDetailsParse(response, covers).apply { initialized = true }
        }
    }

    fun fetchMangaDetailsObservable(manga: SManga): Observable<SManga> {

        return client.newCall(mangaRequest(manga))
            .asObservableSuccess()
            .map { response ->
                ApiMangaParser(client, filterHandler).mangaDetailsParse(response, emptyList()).apply { initialized = true }
            }
    }

    fun fetchChapterListObservable(manga: SManga): Observable<List<SChapter>> {
        return client.newCall(mangaFeedRequest(manga, 0, langs))
            .asObservableSuccess()
            .map { response ->
                val chapterListResponse = MdUtil.jsonParser.decodeFromString(ChapterListResponse.serializer(), response.body!!.string())
                val results = chapterListResponse.results

                var hasMoreResults = chapterListResponse.limit + chapterListResponse.offset < chapterListResponse.total

                while (hasMoreResults) {
                    val offset = chapterListResponse.offset + chapterListResponse.limit
                    val newResponse = client.newCall(mangaFeedRequest(manga, offset, langs)).execute()
                    val newChapterListResponse = MdUtil.jsonParser.decodeFromString(ChapterListResponse.serializer(), newResponse.body!!.string())
                    hasMoreResults = newChapterListResponse.limit + newChapterListResponse.offset < newChapterListResponse.total
                }
                val groupIds = results.map { chapter -> chapter.relationships }.flatten().filter { it.type == "scanlation_group" }.map { it.id }.distinct()

                val groupMap = runCatching {
                    groupIds.chunked(100).mapIndexed { index, ids ->
                        val newResponse = client.newCall(groupIdRequest(ids, 100 * index)).execute()
                        val groupList = MdUtil.jsonParser.decodeFromString(GroupListResponse.serializer(), newResponse.body!!.string())
                        groupList.results.map { group -> Pair(group.data.id, group.data.attributes.name) }
                    }.flatten().toMap()
                }.getOrNull() ?: emptyMap()


                ApiMangaParser(client, filterHandler).chapterListParse(results, groupMap)
            }
    }

    suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return withContext(Dispatchers.IO) {
            val response = client.newCall(mangaFeedRequest(manga, 0, langs)).await()
            if (response.code != 200) {
                XLog.e("error", response.body!!.string())
                throw Exception("error returned from MangaDex")
            }
            val chapterListResponse = MdUtil.jsonParser.decodeFromString(ChapterListResponse.serializer(), response.body!!.string())
            val results = chapterListResponse.results

            var hasMoreResults = chapterListResponse.limit + chapterListResponse.offset < chapterListResponse.total

            while (hasMoreResults) {
                val offset = chapterListResponse.offset + chapterListResponse.limit
                val newResponse = client.newCall(mangaFeedRequest(manga, offset, langs)).await()
                val newChapterListResponse = MdUtil.jsonParser.decodeFromString(ChapterListResponse.serializer(), newResponse.body!!.string())
                hasMoreResults = newChapterListResponse.limit + newChapterListResponse.offset < newChapterListResponse.total
            }

            val groupMap = getGroupMap(results)



            ApiMangaParser(client, filterHandler).chapterListParse(results, groupMap)
        }
    }

    private suspend fun getGroupMap(results: List<ChapterResponse>): Map<String, String> {
        val groupIds = results.map { chapter -> chapter.relationships }.flatten().filter { it.type == "scanlation_group" }.map { it.id }.distinct()
        val groupMap = runCatching {
            groupIds.chunked(100).mapIndexed { index, ids ->
                val newResponse = client.newCall(groupIdRequest(ids, 100 * index)).await()
                val groupList = MdUtil.jsonParser.decodeFromString(GroupListResponse.serializer(), newResponse.body!!.string())
                groupList.results.map { group -> Pair(group.data.id, group.data.attributes.name) }
            }.flatten().toMap()
        }.getOrNull() ?: emptyMap()

        return groupMap
    }

    fun fetchRandomMangaId(): Observable<String> {
        return client.newCall(randomMangaRequest())
            .asObservableSuccess()
            .map { response ->
                ApiMangaParser(client, filterHandler).randomMangaIdParse(response)
            }
    }

    private fun randomMangaRequest(): Request {
        return GET(MdUtil.randomMangaUrl)
    }

    private fun mangaRequest(manga: SManga): Request {
        return GET(MdUtil.mangaUrl + "/" + MdUtil.getMangaId(manga.url), headers, CacheControl.FORCE_NETWORK)
    }

    private fun mangaFeedRequest(manga: SManga, offset: Int, langs: List<String>): Request {
        return GET(MdUtil.mangaFeedUrl(MdUtil.getMangaId(manga.url), offset, langs), headers, CacheControl.FORCE_NETWORK)
    }

    private fun groupIdRequest(id: List<String>, offset: Int): Request {
        val urlSuffix = id.joinToString("&ids[]=", "?limit=100&offset=$offset&ids[]=")
        return GET(MdUtil.groupUrl + urlSuffix, headers)
    }

    /*  private fun coverRequest(manga: SManga): Request {
          return GET(MdUtil.apiUrl + MdUtil.apiManga + MdUtil.getMangaId(manga.url) + MdUtil.apiCovers, headers, CacheControl.FORCE_NETWORK)
      }*/

    companion object {
    }
}
