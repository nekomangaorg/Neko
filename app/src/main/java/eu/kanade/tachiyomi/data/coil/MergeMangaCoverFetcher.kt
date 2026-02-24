package eu.kanade.tachiyomi.data.coil

import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import eu.kanade.tachiyomi.source.online.HttpSource
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okio.FileSystem
import org.nekomanga.core.network.CACHE_CONTROL_NO_STORE
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await

class MergeMangaCoverFetcher(
    private val url: String,
    private val sourceLazy: Lazy<HttpSource>,
    private val options: Options,
) : Fetcher {

    val fileScope = CoroutineScope(Job() + Dispatchers.IO)

    override suspend fun fetch(): FetchResult {
        return httpLoader()
    }

    private suspend fun httpLoader(): FetchResult {
        try {
            // Fetch from network
            val response = executeNetworkRequest()
            val responseBody = checkNotNull(response.body) { "Null response source" }
            try {
                // Read from response if cache is unused or unusable
                return SourceFetchResult(
                    source =
                        ImageSource(source = responseBody.source(), fileSystem = FileSystem.SYSTEM),
                    mimeType = "image/*",
                    dataSource = DataSource.NETWORK,
                )
            } catch (e: Exception) {
                responseBody.close()
                throw e
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                TimberKt.e(e) { "error loading image" }
            }
            throw e
        }
    }

    private suspend fun executeNetworkRequest(): Response {
        val client = sourceLazy.value.client
        val response = client.newCall(newRequest()).await()
        if (!response.isSuccessful && response.code != HTTP_NOT_MODIFIED) {
            response.close()
            throw Exception(response.message)
        }
        return response
    }

    private fun newRequest(): Request {
        val request = Request.Builder().url(url).headers(sourceLazy.value.headers)

        when {
            options.networkCachePolicy.readEnabled -> {
                // don't take up okhttp cache
                request.cacheControl(CACHE_CONTROL_NO_STORE)
            }
            else -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        return request.build()
    }

    companion object {
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE =
            CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
