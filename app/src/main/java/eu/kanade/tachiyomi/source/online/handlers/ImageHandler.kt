package eu.kanade.tachiyomi.source.online.handlers

import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.newCallWithProgress
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.dto.AtHomeImageReportDto
import eu.kanade.tachiyomi.source.online.utils.MdConstants
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import uy.kohesive.injekt.injectLazy
import java.util.Date

class ImageHandler {
    val network: NetworkHelper by injectLazy()
    val preferences: PreferencesHelper by injectLazy()
    val mangaPlusHandler: MangaPlusHandler by injectLazy()

    // chapter id and last request time
    private val tokenTracker = hashMapOf<String, Long>()

    suspend fun getImage(page: Page, isLogged: Boolean): Response {
        if (page.imageUrl!!.contains("mangaplus", true)) {
            return mangaPlusHandler.client.newCall(GET(page.imageUrl!!, mangaPlusHandler.headers))
                .await()
        } else {
            val response = try {
                network.nonRateLimitedClient.newCallWithProgress(imageRequest(page, isLogged),
                    page)
                    .await()
            } catch (e: Exception) {
                XLog.e("error getting images", e)
                throw (e)
            }

            val byteSize = response.peekBody(Long.MAX_VALUE).bytes().size
            val duration = response.receivedResponseAtMillis - response.sentRequestAtMillis
            val cache = response.header("X-Cache", "") == "HIT"
            val result = AtHomeImageReportDto(
                page.imageUrl!!,
                response.isSuccessful,
                byteSize,
                cache,
                duration
            )

            runCatching {
                network.service.atHomeImageReport(result)
            }.onFailure { e ->
                Timber.e(e, "error trying to post to dex@home")

            }

            if (!response.isSuccessful) {
                response.close()
                throw Exception("HTTP error ${response.code}")
            }
            return response
        }
    }

    suspend fun imageRequest(page: Page, isLogged: Boolean): Request {
        val service = if (isLogged) {
            network.authService
        } else {
            network.service
        }

        val data = page.url.split(",")
        val currentTime = Date().time
        val mdAtHomeServerUrl =
            when (currentTime - tokenTracker[page.mangaDexChapterId]!! < MdConstants.mdAtHomeTokenLifespan) {
                true -> data[0]
                false -> {
                    updateTokenTracker(page.mangaDexChapterId, currentTime)
                    service.getAtHomeServer(page.mangaDexChapterId,
                        preferences.usePort443Only()).body()!!.baseUrl
                }
            }
        return GET(mdAtHomeServerUrl + page.imageUrl, network.headers)
    }

    fun updateTokenTracker(chapterId: String, time: Long) {
        tokenTracker[chapterId] = time
    }
}