package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.models.dto.ChapterDataDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.logTimeTaken
import eu.kanade.tachiyomi.util.throws
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

class MangaHandler {
    val network: NetworkHelper by injectLazy()
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
            network.service.viewChapter(urlChapterId)
                .onFailure {
                    this.log("trying to get manga id from chapter id")
                }.getOrThrow()
                .data.relationships.first { it.type == MdConstants.Types.manga }.id
        }
    }

    suspend fun fetchMangaDetails(manga: SManga): SManga {
        return withContext(Dispatchers.IO) {
            logTimeTaken("Manga Detail for  ${manga.title}") {
                val response = network.service.viewManga(MdUtil.getMangaId(manga.url))
                    .onFailure {
                        val type = "trying to view manga ${manga.title}"
                        this.log(type)
                        this.throws(type)
                    }.getOrThrow()

                apiMangaParser.mangaDetailsParse(response.data)
            }
        }
    }

    suspend fun fetchChapterList(manga: SManga): List<SChapter> {
        return withContext(Dispatchers.IO) {
            logTimeTaken("Fetch Chapters for  ${manga.title}") {
                val langs = MdUtil.getLangsToShow(preferencesHelper)

                val chapterListDto = logTimeTaken("fetching chapters from Dex") {
                    network.service.viewChapters(MdUtil.getMangaId(manga.url), langs, 0).onFailure {
                        val type = "trying to view chapters"
                        this.log(type)
                        this.throws(type)
                    }.getOrThrow()
                }

                val results = chapterListDto.data.toMutableList()

                var hasMoreResults =
                    chapterListDto.limit + chapterListDto.offset < chapterListDto.total

                var offset = chapterListDto.offset
                val limit = chapterListDto.limit

                while (hasMoreResults) {
                    offset += limit
                    network.service.viewChapters(MdUtil.getMangaId(manga.url), langs, offset)
                        .onFailure {
                            this.log("trying to get more results")
                            hasMoreResults = false
                        }.onSuccess {
                            val newChapterListDto = this.data
                            results.addAll(newChapterListDto.data)
                            hasMoreResults =
                                newChapterListDto.limit + newChapterListDto.offset < newChapterListDto.total
                        }
                }

                val groupMap = getGroupMap(results)
                apiMangaParser.chapterListParse(results, groupMap)
            }
        }
    }

    private fun getGroupMap(results: List<ChapterDataDto>): Map<String, String> {
        return results.map { chapter -> chapter.relationships }
            .flatten()
            .filter { it.type == MdConstants.Types.scanlator }
            .map { it.id to it.attributes!!.name!! }
            .toMap()
    }
}
