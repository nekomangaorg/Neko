package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.getExtra
import coil3.request.Options
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.MangaDex
import java.io.File
import java.net.HttpURLConnection.HTTP_NOT_MODIFIED
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CancellationException
import okhttp3.CacheControl
import okhttp3.Request
import okhttp3.Response
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.buffer
import okio.sink
import org.nekomanga.core.network.CACHE_CONTROL_NO_STORE
import org.nekomanga.domain.manga.Artwork
import org.nekomanga.logging.TimberKt
import tachiyomi.core.network.await
import uy.kohesive.injekt.injectLazy

class MangaCoverFetcher(
    private val altUrl: String,
    private val inLibrary: Boolean,
    private val mangaId: Long,
    private val originalThumbnailUrl: String,
    private val sourceLazy: Lazy<MangaDex>,
    private val options: Options,
    private val coverCache: CoverCache,
    private val diskCacheLazy: Lazy<DiskCache>,
) : Fetcher {

    // Prevent the UninitializedPropertyAccessException by passing "" for the cover
    private val diskCacheKey: String? by lazy {
        ArtworkKeyer()
            .key(
                Artwork(
                    cover = "", // Leave blank so Keyer knows this is a main cover
                    dynamicCover = altUrl,
                    originalCover = originalThumbnailUrl,
                    mangaId = mangaId,
                    inLibrary = inLibrary,
                ),
                options,
            )
    }

    lateinit var url: String

    override suspend fun fetch(): FetchResult {
        val useDynamic = options.getExtra(DynamicCoverKey)

        // Priority 3 & 1: Dynamic vs Default
        // (Priority 2, the custom user cover, remains safely inside httpLoader)
        url =
            if (useDynamic && altUrl.isNotBlank()) {
                altUrl
            } else {
                originalThumbnailUrl
            }

        return when (getResourceType(url)) {
            Type.URL -> httpLoader()
            Type.File -> fileLoader(File(url.substringAfter("file://")))
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
                return fileLoader(customCoverFile)
            }
        }
        val coverFile = coverCache.getCoverFile(url, inLibrary)
        if (!shouldFetchRemotely && coverFile.exists() && options.diskCachePolicy.readEnabled) {
            if (!inLibrary) {
                coverFile.setLastModified(Date().time)
            }

            return fileLoader(coverFile)
        }
        var snapshot = readFromDiskCache()
        try {
            // Fetch from disk cache
            if (snapshot != null) {
                val snapshotCoverCache = moveSnapshotToCoverCache(snapshot, coverFile)
                if (snapshotCoverCache != null) {
                    // Read from cover cache after added to library
                    return fileLoader(snapshotCoverCache)
                }

                return SourceFetchResult(
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

                if (responseCoverCache != null) {
                    return fileLoader(responseCoverCache)
                }

                // Read from disk cache
                snapshot = writeToDiskCache(response)
                if (snapshot != null) {
                    return SourceFetchResult(
                        source = snapshot.toImageSource(),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                }

                // Read from response if cache is unused or unusable
                return SourceFetchResult(
                    source =
                        ImageSource(source = responseBody.source(), fileSystem = FileSystem.SYSTEM),
                    mimeType = "image/*",
                    dataSource =
                        if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK,
                )
            } catch (e: Exception) {
                responseBody.close()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.close()
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
        val request =
            Request.Builder()
                .url(url)
                .headers(
                    sourceLazy.value.headers
                        .newBuilder()
                        .add("x-request-id", "Neko-" + UUID.randomUUID())
                        .build()
                )

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
            TimberKt.e(e) { "Failed to write snapshot data to cover cache ${cacheFile.name}" }
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
            TimberKt.e(e) { "Failed to write response data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private fun writeSourceToCoverCache(input: Source, cacheFile: File) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.delete()
        try {
            cacheFile.sink().buffer().use { output -> output.writeAll(input) }
        } catch (e: Exception) {
            cacheFile.delete()
            throw e
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled)
            diskCacheLazy.value.openSnapshot(diskCacheKey!!)
        else null
    }

    private fun writeToDiskCache(response: Response): DiskCache.Snapshot? {
        if (!options.diskCachePolicy.writeEnabled) {
            return null
        }
        val editor = diskCacheLazy.value.openEditor(diskCacheKey!!) ?: return null

        try {
            diskCacheLazy.value.fileSystem.write(editor.data) {
                response.body.source().readAll(this)
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (ignored: Exception) {}
            throw e
        }
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(
            file = data,
            fileSystem = FileSystem.SYSTEM,
            diskCacheKey = diskCacheKey,
            closeable = this,
        )
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceFetchResult(
            source =
                ImageSource(
                    file = file.toOkioPath(),
                    fileSystem = FileSystem.SYSTEM,
                    diskCacheKey = diskCacheKey,
                ),
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

    class Factory(private val diskCacheLazy: Lazy<DiskCache>) : Fetcher.Factory<Manga> {

        private val coverCache: CoverCache by injectLazy()
        private val sourceManager: SourceManager by injectLazy()

        override fun create(data: Manga, options: Options, imageLoader: ImageLoader): Fetcher {
            return MangaCoverFetcher(
                altUrl = "",
                inLibrary = data.favorite,
                mangaId = data.id!!,
                originalThumbnailUrl = data.thumbnail_url ?: """error("No cover specified")""",
                sourceLazy = lazy { sourceManager.mangaDex },
                options = options,
                coverCache = coverCache,
                diskCacheLazy = diskCacheLazy,
            )
        }
    }

    private enum class Type {
        File,
        URL,
    }

    companion object {
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE =
            CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
