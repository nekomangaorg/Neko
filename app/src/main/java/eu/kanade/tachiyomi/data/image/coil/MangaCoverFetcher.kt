package eu.kanade.tachiyomi.data.image.coil

import android.webkit.MimeTypeMap
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.network.HttpException
import coil.request.Options
import coil.request.Parameters
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.network.CACHE_CONTROL_NO_STORE
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import java.io.File
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.buffer
import okio.sink
import org.nekomanga.domain.manga.Artwork
import uy.kohesive.injekt.injectLazy

class MangaCoverFetcher(
    private val altUrl: String,
    private val inLibrary: Boolean,
    private val mangaId: Long,
    private val originalThumbnailUrl: String,
    private val sourceLazy: Lazy<HttpSource?>,
    private val options: Options,
    private val coverCache: CoverCache,
    private val callFactoryLazy: Lazy<Call.Factory>,
    private val diskCacheLazy: Lazy<DiskCache>,
) : Fetcher {

    // For non-custom cover
    private val diskCacheKey: String? by lazy { ArtworkKeyer().key(Artwork(url = url, inLibrary = inLibrary, originalArtwork = originalThumbnailUrl, mangaId = mangaId), options) }

    val fileScope = CoroutineScope(Job() + Dispatchers.IO)

    lateinit var url: String

    override suspend fun fetch(): FetchResult {
        // diskCacheKey is thumbnail_url
        url = when (altUrl.isBlank()) {
            true -> originalThumbnailUrl
            false -> url
        }

        return when (getResourceType(url)) {
            Type.URL -> httpLoader()
            Type.File -> {
                setRatioAndColorsInScope(mangaId = mangaId, inLibrary = inLibrary, originalThumbnail = originalThumbnailUrl, ogFile = File(url.substringAfter("file://")))
                fileLoader(File(url.substringAfter("file://")))
            }
            null -> error("Invalid image")
        }
    }

    private suspend fun httpLoader(): FetchResult {
        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled
        val onlyCache = !networkRead && diskRead
        val shouldFetchRemotely = networkRead && !diskRead && !onlyCache
        // Use custom cover if exists
        if (!shouldFetchRemotely) {
            val customCoverFile by lazy { coverCache.getCustomCoverFile(mangaId) }
            if (customCoverFile.exists()) {
                setRatioAndColorsInScope(mangaId = mangaId, inLibrary = inLibrary, originalThumbnail = originalThumbnailUrl, ogFile = customCoverFile)
                return fileLoader(customCoverFile)
            }
        }
        val coverFile = coverCache.getCoverFile(originalThumbnailUrl, inLibrary)
        if (!shouldFetchRemotely && coverFile.exists() && options.diskCachePolicy.readEnabled) {
            if (inLibrary.not()) {
                coverFile.setLastModified(Date().time)
            }
            setRatioAndColorsInScope(mangaId = mangaId, inLibrary = inLibrary, originalThumbnail = originalThumbnailUrl, ogFile = coverFile)
            return fileLoader(coverFile)
        }
        var snapshot = readFromDiskCache()
        try {
            // Fetch from disk cache
            if (snapshot != null) {
                val snapshotCoverCache = moveSnapshotToCoverCache(snapshot, coverFile)
                if (snapshotCoverCache != null) {
                    // Read from cover cache after added to library
                    setRatioAndColorsInScope(mangaId = mangaId, inLibrary = inLibrary, originalThumbnail = originalThumbnailUrl, ogFile = snapshotCoverCache)
                    return fileLoader(snapshotCoverCache)
                }

                // Read from snapshot
                setRatioAndColorsInScope(mangaId = mangaId, inLibrary = inLibrary, originalThumbnail = originalThumbnailUrl)
                return SourceResult(
                    source = snapshot.toImageSource(),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }

            // Fetch from network
            val response = executeNetworkRequest()
            val responseBody = checkNotNull(response.body) { "Null response source" }
            try {
                // Read from cover cache after library manga cover updated
                val responseCoverCache = writeResponseToCoverCache(response, coverFile)
                setRatioAndColorsInScope(mangaId = mangaId, inLibrary = inLibrary, originalThumbnail = originalThumbnailUrl)
                if (responseCoverCache != null) {
                    return fileLoader(responseCoverCache)
                }

                // Read from disk cache
                snapshot = writeToDiskCache(response)
                if (snapshot != null) {
                    return SourceResult(
                        source = snapshot.toImageSource(),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                }

                // Read from response if cache is unused or unusable
                return SourceResult(
                    source = ImageSource(source = responseBody.source(), context = options.context),
                    mimeType = "image/*",
                    dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK,
                )
            } catch (e: Exception) {
                responseBody.close()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.close()
            if (e !is CancellationException) {
                XLog.e("error loading image", e)
            }
            throw e
        }
    }

    private suspend fun executeNetworkRequest(): Response {
        val client = sourceLazy.value?.client ?: callFactoryLazy.value
        val response = client.newCall(newRequest()).await()
        if (!response.isSuccessful && response.code != HTTP_NOT_MODIFIED) {
            response.close()
            throw HttpException(response)
        }
        return response
    }

    private fun newRequest(): Request {
        val request = Request.Builder()
            .url(url)
            .headers(sourceLazy.value?.headers ?: options.headers)
            // Support attaching custom data to the network request.
            .tag(Parameters::class.java, options.parameters)

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

    private fun moveSnapshotToCoverCache(snapshot: DiskCache.Snapshot, cacheFile: File?): File? {
        if (cacheFile == null) return null
        return try {
            diskCacheLazy.value.run {
                fileSystem.source(snapshot.data).use { input ->
                    writeSourceToCoverCache(input, cacheFile)
                }
                remove(diskCacheKey!!)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            XLog.e("Failed to write snapshot data to cover cache ${cacheFile.name}", e)
            null
        }
    }

    private fun writeResponseToCoverCache(response: Response, cacheFile: File?): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
        return try {
            response.peekBody(Long.MAX_VALUE).source().use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            XLog.e("Failed to write response data to cover cache ${cacheFile.name}", e)
            null
        }
    }

    private fun writeSourceToCoverCache(input: Source, cacheFile: File) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.delete()
        try {
            cacheFile.sink().buffer().use { output ->
                output.writeAll(input)
            }
        } catch (e: Exception) {
            cacheFile.delete()
            throw e
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) diskCacheLazy.value[diskCacheKey!!] else null
    }

    private fun writeToDiskCache(
        response: Response,
    ): DiskCache.Snapshot? {
        val editor = diskCacheLazy.value.edit(diskCacheKey!!) ?: return null
        try {
            diskCacheLazy.value.fileSystem.write(editor.data) {
                response.body!!.source().readAll(this)
            }
            return editor.commitAndGet()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (ignored: Exception) {
            }
            throw e
        }
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(file = data, diskCacheKey = diskCacheKey, closeable = this)
    }

    private fun setRatioAndColorsInScope(mangaId: Long, originalThumbnail: String, inLibrary: Boolean, ogFile: File? = null, force: Boolean = false) {
        fileScope.launch {
            MangaCoverMetadata.setRatioAndColors(mangaId, originalThumbnail, inLibrary, ogFile, force)
        }
    }

    /** Modified from [MimeTypeMap.getFileExtensionFromUrl] to be more permissive with special characters. */
    private fun MimeTypeMap.getMimeTypeFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) {
            return null
        }

        val extension = url
            .substringBeforeLast('#') // Strip the fragment.
            .substringBeforeLast('?') // Strip the query.
            .substringAfterLast('/') // Get the last path segment.
            .substringAfterLast('.', missingDelimiterValue = "") // Get the file extension.

        return getMimeTypeFromExtension(extension)
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceResult(
            source = ImageSource(file = file.toOkioPath(), diskCacheKey = diskCacheKey),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private fun getResourceType(cover: String?): Type? {
        return when {
            cover.isNullOrEmpty() -> null
            cover.startsWith("http") || cover.startsWith("Custom-", true) -> Type.URL
            cover.startsWith("/") || cover.startsWith("file://") -> Type.File
            else -> null
        }
    }

    class Factory(
        private val callFactoryLazy: Lazy<Call.Factory>,
        private val diskCacheLazy: Lazy<DiskCache>,
    ) : Fetcher.Factory<Manga> {

        private val coverCache: CoverCache by injectLazy()
        private val sourceManager: SourceManager by injectLazy()

        override fun create(data: Manga, options: Options, imageLoader: ImageLoader): Fetcher {
            return MangaCoverFetcher(
                altUrl = "",
                inLibrary = data.favorite,
                mangaId = data.id!!,
                originalThumbnailUrl = data.thumbnail_url ?: """error("No cover specified")""",
                sourceLazy = lazy { sourceManager.get(data.source) as? HttpSource },
                options = options,
                coverCache = coverCache,
                callFactoryLazy = callFactoryLazy,
                diskCacheLazy = diskCacheLazy,
            )
        }
    }

    class ArtworkFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
        private val diskCacheLazy: Lazy<DiskCache>,
    ) : Fetcher.Factory<Artwork> {

        private val coverCache: CoverCache by injectLazy()
        private val sourceManager: SourceManager by injectLazy()

        override fun create(data: Artwork, options: Options, imageLoader: ImageLoader): Fetcher {
            return when (data.url.isBlank()) {
                true -> MangaCoverFetcher(
                    altUrl = data.url,
                    inLibrary = data.inLibrary,
                    mangaId = data.mangaId,
                    originalThumbnailUrl = data.originalArtwork,
                    sourceLazy = lazy { sourceManager.getMangadex() },
                    options = options,
                    coverCache = coverCache,
                    callFactoryLazy = callFactoryLazy,
                    diskCacheLazy = diskCacheLazy,
                )
                false -> AlternativeMangaCoverFetcher(
                    url = data.url,
                    mangaId = data.mangaId,
                    sourceLazy = lazy { sourceManager.getMangadex() },
                    options = options,
                    coverCache = coverCache,
                    callFactoryLazy = callFactoryLazy,
                    diskCacheLazy = diskCacheLazy,
                )
            }
        }
    }

    private enum class Type {
        File, URL;
    }

    companion object {
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
