package eu.kanade.tachiyomi.source.online.handlers

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onSuccess
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.services.NetworkServices
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.handlers.external.AzukiHandler
import eu.kanade.tachiyomi.source.online.handlers.external.BilibiliHandler
import eu.kanade.tachiyomi.source.online.handlers.external.ComikeyHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaHotHandler
import eu.kanade.tachiyomi.source.online.handlers.external.MangaPlusHandler
import eu.kanade.tachiyomi.source.online.models.dto.AtHomeImageReportDto
import eu.kanade.tachiyomi.util.getOrResultError
import eu.kanade.tachiyomi.util.system.withIOContext
import eu.kanade.tachiyomi.util.system.withNonCancellableContext
import java.util.Date
import kotlin.collections.set
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.nekomanga.constants.MdConstants
import org.nekomanga.core.network.CACHE_CONTROL_NO_STORE
import org.nekomanga.core.network.GET
import org.nekomanga.domain.network.message
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import tachiyomi.core.network.newCachelessCallWithProgress
import uy.kohesive.injekt.injectLazy

class ImageHandler {
    val network: NetworkHelper by injectLazy()
    val networkServices: NetworkServices by injectLazy()
    val preferences: PreferencesHelper by injectLazy()
    private val azukiHandler: AzukiHandler by injectLazy()
    private val mangaHotHandler: MangaHotHandler by injectLazy()
    private val mangaPlusHandler: MangaPlusHandler by injectLazy()
    private val bilibiliHandler: BilibiliHandler by injectLazy()
    private val comikeyHandler: ComikeyHandler by injectLazy()

    // chapter id and last request time
    private val tokenTracker = hashMapOf<String, Long>()

    private val tag = "||ImageHandler"

    suspend fun getImage(page: Page, isLogged: Boolean): Response {
        return withIOContext {
            return@withIOContext when {
                isExternal(page, "mangaplus") || isExternal(page, "jumpg-assets") ->
                    getImageResponse(mangaPlusHandler.client, mangaPlusHandler.headers, page)
                isExternal(page, "comikey") ->
                    getImageResponse(comikeyHandler.client, comikeyHandler.headers, page)
                isExternal(page, "azuki") ->
                    getImageResponse(azukiHandler.client, azukiHandler.headers, page)
                isExternal(page, "mangahot") ->
                    getImageResponse(mangaHotHandler.client, mangaHotHandler.headers, page)
                isExternal(page, "/bfs/comic/") ->
                    getImageResponse(bilibiliHandler.client, bilibiliHandler.headers, page)
                else -> {
                    val request = imageRequest(page, isLogged)
                    requestImage(request, page)
                }
            }
        }
    }

    private suspend fun requestImage(request: Request, page: Page): Response {

        var attempt =
            com.github.michaelbull.result
                .runCatching {
                    network.cdnClient.newCachelessCallWithProgress(request, page).await()
                }
                .onSuccess { response ->
                    withNonCancellableContext { reportImageWithResponse(response) }
                }

        if (
            (attempt.getError() != null || !attempt.get()!!.isSuccessful) &&
                attempt.getError() !is CancellationException &&
                !request.url.toString().startsWith(MdConstants.cdnUrl)
        ) {
            TimberKt.e(attempt.getError()) {
                "$tag error getting image from at home node falling back to cdn"
            }

            attempt =
                com.github.michaelbull.result.runCatching {
                    val newRequest =
                        buildRequest(MdConstants.cdnUrl + page.imageUrl, network.headers)
                    network.cdnClient.newCachelessCallWithProgress(newRequest, page).await()
                }
        }

        attempt.onSuccess { response ->
            if (!response.isSuccessful) {
                response.close()
                TimberKt.e(attempt.getError()) {
                    "$tag response for image was not successful http status code ${response.code}"
                }
                throw Exception("HTTP error ${response.code}")
            }
        }

        return attempt.getOrThrow { e ->
            if (e !is CancellationException) {
                TimberKt.e(attempt.getError()) { "$tag error getting images" }
                withNonCancellableContext { reportFailedImage(request.url.toString()) }
            }
            e
        }
    }

    private suspend fun reportFailedImage(url: String) {
        val atHomeImageReportDto =
            AtHomeImageReportDto(
                url,
                false,
                duration = 30.seconds.inWholeMilliseconds,
            )
        sendReport(atHomeImageReportDto)
    }

    private suspend fun reportImageWithResponse(response: Response) {
        val byteSize = response.peekBody(Long.MAX_VALUE).bytes().size.toLong()
        val duration = response.receivedResponseAtMillis - response.sentRequestAtMillis
        val cache = response.header("X-Cache", "") == "HIT"
        val atHomeImageReportDto =
            AtHomeImageReportDto(
                response.request.url.toString(),
                response.isSuccessful,
                byteSize,
                cache,
                duration,
            )
        TimberKt.d { "$tag  $atHomeImageReportDto" }
        sendReport(atHomeImageReportDto)
    }

    private suspend fun sendReport(atHomeImageReportDto: AtHomeImageReportDto) {
        TimberKt.d { "$tag Image to report $atHomeImageReportDto" }

        if (atHomeImageReportDto.url.startsWith(MdConstants.cdnUrl)) {
            TimberKt.d { "$tag image is at CDN don't report to md@home node" }
            return
        }
        networkServices.service.atHomeImageReport(atHomeImageReportDto)
    }

    private suspend fun imageRequest(page: Page, isLogged: Boolean): Request {
        val data = page.url.split(",")
        val currentTime = Date().time

        val mdAtHomeServerUrl =
            when (
                tokenTracker[page.mangaDexChapterId] != null &&
                    (currentTime - tokenTracker[page.mangaDexChapterId]!!) <
                        MdConstants.mdAtHomeTokenLifespan
            ) {
                true -> data[0]
                false -> {
                    TimberKt.d { "$tag Time has expired get new at home url isLogged $isLogged" }
                    updateTokenTracker(page.mangaDexChapterId, currentTime)

                    networkServices.atHomeService
                        .getAtHomeServer(
                            page.mangaDexChapterId,
                            preferences.usePort443Only().get(),
                        )
                        .getOrResultError("getting image")
                        .getOrThrow { Exception(it.message()) }
                        .baseUrl
                }
            }
        TimberKt.d {
            """
            $tag
            Image server is $mdAtHomeServerUrl
            page url is ${page.imageUrl}
            """
        }
        return buildRequest(mdAtHomeServerUrl + page.imageUrl, network.headers)
    }

    // images will be cached or saved manually, so don't take up network cache
    private fun buildRequest(url: String, headers: Headers): Request {
        return GET(url, headers).newBuilder().cacheControl(CACHE_CONTROL_NO_STORE).build()
    }

    private suspend fun getImageResponse(
        client: OkHttpClient,
        headers: Headers,
        page: Page
    ): Response {
        return client
            .newCachelessCallWithProgress(buildRequest(page.imageUrl!!, headers), page)
            .await()
    }

    private fun isExternal(page: Page, scanlatorName: String): Boolean {
        TimberKt.d { "PAGE IMAGE URL: ${page.imageUrl}" }
        return page.imageUrl?.contains(scanlatorName, true) ?: false
    }

    fun updateTokenTracker(chapterId: String, time: Long) {
        tokenTracker[chapterId] = time
    }
}
