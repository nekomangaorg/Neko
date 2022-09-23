package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.github.michaelbull.result.getOrThrow
import com.skydoves.sandwich.onFailure
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.CACHE_CONTROL_NO_STORE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.handlers.external.AzukiHandler
import eu.kanade.tachiyomi.source.online.handlers.external.BilibiliHandler
import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaHotHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaPlusHandler
import eu.kanade.tachiyomi.source.online.models.dto.AtHomeImageReportDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withIOContext
import java.util.Date
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.nekomanga.domain.network.message
import uy.kohesive.injekt.injectLazy

class ImageHandler {
    val network: NetworkHelper by injectLazy()
    val preferences: PreferencesHelper by injectLazy()
    private val azukiHandler: AzukiHandler by injectLazy()
    private val mangaHotHandler: MangaHotHandler by injectLazy()
    private val mangaPlusHandler: MangaPlusHandler by injectLazy()
    private val bilibiliHandler: BilibiliHandler by injectLazy()
    private val comikeyHandler: ComikeyHandler by injectLazy()

    val log = XLog.tag("||ImageHandler").disableStackTrace().build()

    // chapter id and last request time
    private val tokenTracker = hashMapOf<String, Long>()

    suspend fun getImage(page: Page, isLogged: Boolean): Response {
        return withIOContext {
            return@withIOContext when {
                isExternal(page, "mangaplus") -> getImageResponse(mangaPlusHandler.client, mangaPlusHandler.headers, page)
                isExternal(page, "comikey") -> getImageResponse(comikeyHandler.client, comikeyHandler.headers, page)
                isExternal(page, "azuki") -> getImageResponse(azukiHandler.client, azukiHandler.headers, page)
                isExternal(page, "mangahot") -> getImageResponse(mangaHotHandler.client, mangaHotHandler.headers, page)
                isExternal(page, "/bfs/comic/") -> getImageResponse(bilibiliHandler.client, bilibiliHandler.headers, page)

                else -> {
                    val request = imageRequest(page, isLogged)
                    val response = try {
                        network.nonRateLimitedClient.newCallWithProgress(request, page)
                            .await()
                    } catch (e: Exception) {
                        log.e("error getting images", e)
                        reportFailedImage(request.url.toString())
                        throw (e)
                    }

                    reportImageWithResponse(response)

                    if (!response.isSuccessful) {
                        response.close()
                        log.e("response for image was not successful http status code ${response.code}")
                        throw Exception("HTTP error ${response.code}")
                    }
                    response
                }
            }
        }
    }

    private fun reportFailedImage(url: String) {
        val atHomeImageReportDto = AtHomeImageReportDto(
            url,
            false,
            duration = 30.seconds.inWholeMilliseconds,
        )
        sendReport(atHomeImageReportDto)
    }

    private fun reportImageWithResponse(response: Response) {
        val byteSize = response.peekBody(Long.MAX_VALUE).bytes().size
        val duration = response.receivedResponseAtMillis - response.sentRequestAtMillis
        val cache = response.header("X-Cache", "") == "HIT"
        val atHomeImageReportDto = AtHomeImageReportDto(
            response.request.url.toString(),
            response.isSuccessful,
            byteSize,
            cache,
            duration,
        )
        log.d(atHomeImageReportDto)
        sendReport(atHomeImageReportDto)
    }

    private fun sendReport(atHomeImageReportDto: AtHomeImageReportDto) {
        launchIO {
            log.d("Image to report $atHomeImageReportDto")

            if (atHomeImageReportDto.url.startsWith(MdConstants.cdnUrl)) {
                log.d("image is at CDN don't report to md@home node")
                return@launchIO
            }
            network.service.atHomeImageReport(atHomeImageReportDto).onFailure {
                this.log("trying to post to dex@home")
            }
        }
    }

    suspend fun imageRequest(page: Page, isLogged: Boolean): Request {
        val data = page.url.split(",")
        val currentTime = Date().time
        val mdAtHomeServerUrl =
            when (currentTime - tokenTracker[page.mangaDexChapterId]!! < MdConstants.mdAtHomeTokenLifespan) {
                true -> data[0]
                false -> {
                    log.d("Time has expired get new at home url isLogged $isLogged")
                    updateTokenTracker(page.mangaDexChapterId, currentTime)

                    network.cdnService.getAtHomeServer(
                        page.mangaDexChapterId,
                        preferences.usePort443Only(),
                    )
                        .getOrResultError("getting image")
                        .getOrThrow { Exception(it.message()) }
                        .baseUrl
                }
            }
        log.d("Image server is $mdAtHomeServerUrl")
        log.d("page url is ${page.imageUrl}")
        return buildRequest(mdAtHomeServerUrl + page.imageUrl, network.headers)
    }

    // images will be cached or saved manually, so don't take up network cache
    private fun buildRequest(url: String, headers: Headers): Request {
        return GET(url, headers)
            .newBuilder()
            .cacheControl(CACHE_CONTROL_NO_STORE)
            .build()
    }

    private suspend fun getImageResponse(client: OkHttpClient, headers: Headers, page: Page): Response {
        return client.newCallWithProgress(buildRequest(page.imageUrl!!, headers), page).await()
    }

    private fun isExternal(page: Page, scanlatorName: String): Boolean {
        return page.imageUrl?.contains(scanlatorName, true) ?: false
    }

    fun updateTokenTracker(chapterId: String, time: Long) {
        tokenTracker[chapterId] = time
    }
}
