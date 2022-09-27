package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.getOrThrow
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.handlers.external.AzukiHandler
import eu.kanade.tachiyomi.source.online.handlers.external.BilibiliHandler
import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaHotHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaPlusHandler
import eu.kanade.tachiyomi.source.online.models.dto.AtHomeDto
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.log
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.domain.network.message
import uy.kohesive.injekt.injectLazy

class PageHandler {

    val network: NetworkHelper by injectLazy()
    val preferences: PreferencesHelper by injectLazy()
    val mangaPlusHandler: MangaPlusHandler by injectLazy()
    val comikeyHandler: ComikeyHandler by injectLazy()
    val azukiHandler: AzukiHandler by injectLazy()
    val mangaHotHandler: MangaHotHandler by injectLazy()
    val bilibiliHandler: BilibiliHandler by injectLazy()
    val imageHandler: ImageHandler by injectLazy()

    suspend fun fetchPageList(chapter: SChapter): List<Page> {
        return withContext(Dispatchers.IO) {
            XLog.d("fetching page list")

            try {
                val chapterAttributesDto = network.service.viewChapter(chapter.mangadex_chapter_id)
                    .onFailure {
                        this.log("trying to fetch page list")
                        throw Exception("error returned from chapterResponse")
                    }.getOrThrow().data.attributes

                val externalUrl = chapterAttributesDto.externalUrl
                val currentDate = System.currentTimeMillis()
                val chapterDate = MdUtil.parseDate(chapterAttributesDto.readableAt)
                val chapterDateNewer = chapterDate - currentDate > 0

                if (externalUrl != null && chapterAttributesDto.pages == 0) {
                    when {
                        "azuki manga".equals(chapter.scanlator, true) -> {
                            return@withContext azukiHandler.fetchPageList(externalUrl)
                        }
                        "mangahot".equals(chapter.scanlator, true) -> {
                            return@withContext mangaHotHandler.fetchPageList(externalUrl)
                        }
                        "mangaplus".equals(chapter.scanlator, true) -> {
                            return@withContext mangaPlusHandler.fetchPageList(externalUrl)
                        }
                        /*"comikey".equals(chapter.scanlator, true) -> {
                            return@withContext comikeyHandler.fetchPageList(externalUrl)
                        }*/
                        "bilibili comics".equals(chapter.scanlator, true) -> {
                            return@withContext bilibiliHandler.fetchPageList(externalUrl)
                        }
                        else -> throw Exception("${chapter.scanlator} not supported, try webview")
                    }
                }

                if (chapterDateNewer) {
                    throw Exception("This chapter has no pages, it might not be release yet, try refreshing")
                }

                val atHomeDto = network.cdnService.getAtHomeServer(
                    chapter.mangadex_chapter_id,
                    preferences.usePort443Only(),
                ).getOrResultError("trying to get at home response")
                    .getOrThrow { Exception(it.message()) }

                return@withContext pageListParse(
                    chapter.mangadex_chapter_id,
                    atHomeDto,
                    preferences.dataSaver(),
                )
            } catch (e: Exception) {
                XLog.e("error processing page list ", e)
                throw (e)
            }
        }
    }

    fun pageListParse(
        chapterId: String,
        atHomeDto: AtHomeDto,
        dataSaver: Boolean,
    ): List<Page> {
        val hash = atHomeDto.chapter.hash
        val pageArray = if (dataSaver) {
            atHomeDto.chapter.dataSaver.map { "/data-saver/$hash/$it" }
        } else {
            atHomeDto.chapter.data.map { "/data/$hash/$it" }
        }
        val now = Date().time

        val pages = pageArray.mapIndexed { pos, imgUrl ->
            Page(pos + 1, atHomeDto.baseUrl, imgUrl, chapterId)
        }

        imageHandler.updateTokenTracker(chapterId, now)

        return pages
    }
}
