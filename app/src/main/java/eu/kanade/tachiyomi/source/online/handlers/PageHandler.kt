package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.dto.AtHomeDto
import eu.kanade.tachiyomi.source.online.dto.ChapterDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import java.util.Date

class PageHandler {

    val network: NetworkHelper by injectLazy()
    val preferences: PreferencesHelper by injectLazy()
    val mangaPlusHandler: MangaPlusHandler by injectLazy()
    val imageHandler: ImageHandler by injectLazy()

    suspend fun fetchPageList(chapter: SChapter, isLogged: Boolean): List<Page> {
        return withContext(Dispatchers.IO) {
            XLog.d("fetching page list")
            val chapterResponse = async {
                network.service.viewChapter(chapter.mangadex_chapter_id)
            }

            if (chapter.scanlator.equals("mangaplus", true)) {
                val mpChpId = chapterResponse.await().body()!!.data.attributes.data.first()
                    .substringAfterLast("/")
                mangaPlusHandler.fetchPageList(mpChpId)
            } else {

                val service = if (isLogged) {
                    network.authService
                } else {
                    network.service
                }

                val atHomeResponse = async {
                    service.getAtHomeServer(chapter.mangadex_chapter_id,
                        preferences.usePort443Only())
                }

                pageListParse(chapterResponse.await().body()!!,
                    atHomeResponse.await().body()!!,
                    preferences.dataSaver())
            }
        }
    }

    fun pageListParse(
        chapterDto: ChapterDto,
        atHomeDto: AtHomeDto,
        dataSaver: Boolean,
    ): List<Page> {

        val hash = chapterDto.data.attributes.hash
        val pageArray = if (dataSaver) {
            chapterDto.data.attributes.dataSaver.map { "/data-saver/$hash/$it" }
        } else {
            chapterDto.data.attributes.data.map { "/data/$hash/$it" }
        }
        val now = Date().time

        val pages = pageArray.mapIndexed { pos, imgUrl ->
            Page(pos + 1, atHomeDto.baseUrl, imgUrl, chapterDto.data.id)
        }

        imageHandler.updateTokenTracker(chapterDto.data.id, now)

        return pages
    }
}
