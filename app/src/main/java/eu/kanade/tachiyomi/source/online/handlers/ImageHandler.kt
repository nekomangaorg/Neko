package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onException
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.handlers.external.BilibiliHandler
import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaPlusHandler
import eu.kanade.tachiyomi.source.online.models.dto.AtHomeImageReportDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import eu.kanade.tachiyomi.util.log
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.throws
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.util.Date
import kotlin.collections.set
import kotlin.time.Duration

class ImageHandler {
    val network: NetworkHelper by injectLazy()
    val preferences: PreferencesHelper by injectLazy()
    val mangaPlusHandler: MangaPlusHandler by injectLazy()
    val bilibiliHandler: BilibiliHandler by injectLazy()
    val comikeyHandler: ComikeyHandler by injectLazy()

    val log = XLog.tag("||ImageHandler").disableStackTrace().build()

    // chapter id and last request time
    private val tokenTracker = hashMapOf<String, Long>()

    suspend fun getImage(page: Page, isLogged: Boolean): Response {
        return withIOContext {
            return@withIOContext when {
                page.imageUrl!!.contains("mangaplus", true) -> {
                    mangaPlusHandler.client.newCall(GET(page.imageUrl!!,
                        mangaPlusHandler.headers))
                        .await()
                }
                page.imageUrl!!.contains("comikey", true) -> {
                    comikeyHandler.client.newCall(GET(page.imageUrl!!, comikeyHandler.headers))
                        .await()
                }
                page.imageUrl!!.contains("/bfs/comic/", true)
                -> {
                    bilibiliHandler.client.newCall(GET(page.imageUrl!!,
                        bilibiliHandler.headers))
                        .await()
                }

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

    fun reportFailedImage(url: String) {
        val atHomeImageReportDto = AtHomeImageReportDto(
            url,
            false,
            duration = Duration.seconds(30).inWholeMilliseconds
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
            duration
        )
        log.d(atHomeImageReportDto)
        sendReport(atHomeImageReportDto)
    }

    fun sendReport(atHomeImageReportDto: AtHomeImageReportDto) {
        launchIO {
            log.d("Image to report $atHomeImageReportDto")

            if (atHomeImageReportDto.url.startsWith(MdConstants.cdnUrl)) {
                log.d("image is at CDN don't report to md@home node")
                return@launchIO
            }
            network.service.atHomeImageReport(atHomeImageReportDto).onError {
                this.log("trying to post to dex@home")
            }.onException {
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
                    val atHomeResponse = network.service.getAtHomeServer(page.mangaDexChapterId,
                        preferences.usePort443Only())
                    atHomeResponse.onError {
                        atHomeResponse.log(" getting image")
                        atHomeResponse.throws("getting image")
                    }.onException {
                        atHomeResponse.log(" getting image")
                        atHomeResponse.throws("getting image")
                    }.getOrThrow()
                        .baseUrl
                }
            }
        log.d("Image server is $mdAtHomeServerUrl")
        log.d("page url is ${page.imageUrl}")
        return GET(mdAtHomeServerUrl + page.imageUrl, network.headers)
    }

    fun updateTokenTracker(chapterId: String, time: Long) {
        tokenTracker[chapterId] = time
    }
}
