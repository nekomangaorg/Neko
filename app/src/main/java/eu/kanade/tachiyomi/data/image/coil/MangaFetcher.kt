package eu.kanade.tachiyomi.data.image.coil

import android.graphics.BitmapFactory
import android.webkit.MimeTypeMap
import androidx.palette.graphics.Palette
import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.size.Size
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.buffer
import okio.sink
import okio.source
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.Date

class MangaFetcher : Fetcher<Manga> {

    companion object {
        const val realCover = "real_cover"
        const val onlyCache = "only_cache"
        const val onlyFetchRemotely = "only_fetch_remotely"
    }

    private val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val defaultClient = Injekt.get<NetworkHelper>().client
    val fileScope = CoroutineScope(Job() + Dispatchers.IO)

    override fun key(data: Manga): String? {
        if (data.thumbnail_url.isNullOrBlank()) return null
        return if (!data.favorite) {
            data.thumbnail_url!!
        } else {
            DiskUtil.hashKeyForDisk(data.thumbnail_url!!)
        }
    }

    override suspend fun fetch(pool: BitmapPool, data: Manga, size: Size, options: Options): FetchResult {
        val cover = data.thumbnail_url
        return when (getResourceType(cover)) {
            Type.URL -> httpLoader(data, options)
            Type.File -> fileLoader(data)
            null -> error("Invalid image")
        }
    }

    private suspend fun httpLoader(manga: Manga, options: Options): FetchResult {
        val onlyCache = options.parameters.value(onlyCache) == true
        val shouldFetchRemotely = options.parameters.value(onlyFetchRemotely) == true && !onlyCache
        if (!shouldFetchRemotely) {
            val customCoverFile = coverCache.getCustomCoverFile(manga)
            if (customCoverFile.exists() && options.parameters.value(realCover) != true) {
                setRatioAndColorsInScope(manga, customCoverFile)
                return fileLoader(customCoverFile)
            }
        }
        val coverFile = coverCache.getCoverFile(manga)
        if (!shouldFetchRemotely && coverFile.exists() && options.diskCachePolicy.readEnabled) {
            if (!manga.favorite) {
                coverFile.setLastModified(Date().time)
            }
            setRatioAndColorsInScope(manga, coverFile)
            return fileLoader(coverFile)
        }
        val (response, body) = awaitGetCall(
            manga,
            if (manga.favorite) {
                onlyCache
            } else {
                false
            },
            shouldFetchRemotely
        )

        if (options.diskCachePolicy.writeEnabled) {
            val tmpFile = File(coverFile.absolutePath + "_tmp")
            body.source().use { input ->
                tmpFile.sink().buffer().use { output ->
                    output.writeAll(input)
                }
            }

            if (response.isSuccessful || !coverFile.exists()) {
                if (coverFile.exists()) {
                    coverFile.delete()
                }

                tmpFile.renameTo(coverFile)
            }
            if (manga.favorite) {
                coverCache.deleteCachedCovers()
            }
        }
        setRatioAndColorsInScope(manga, coverFile, true)
        return fileLoader(coverFile)
    }

    private fun setRatioAndColorsInScope(manga: Manga, ogFile: File? = null, force: Boolean = false) {
        fileScope.launch {
            setRatioAndColors(manga, ogFile, force)
        }
    }

    fun setRatioAndColors(manga: Manga, ogFile: File? = null, force: Boolean = false) {
        if (!manga.favorite) {
            MangaCoverMetadata.remove(manga)
        }
        if (manga.vibrantCoverColor != null && !manga.favorite) return
        val file = ogFile ?: coverCache.getCustomCoverFile(manga).takeIf { it.exists() } ?: coverCache.getCoverFile(manga)
        // if the file exists and the there was still an error then the file is corrupted
        if (file.exists()) {
            val options = BitmapFactory.Options()
            val hasVibrantColor = if (manga.favorite) manga.vibrantCoverColor != null else true
            if (manga.dominantCoverColors != null && hasVibrantColor && !force) {
                options.inJustDecodeBounds = true
            } else {
                options.inSampleSize = 4
            }
            val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return
            if (!options.inJustDecodeBounds) {
                Palette.from(bitmap).generate {
                    if (it == null) return@generate
                    if (manga.favorite) {
                        it.dominantSwatch?.let { swatch ->
                            manga.dominantCoverColors = swatch.rgb to swatch.titleTextColor
                        }
                    }
                    val color = it.getBestColor() ?: return@generate
                    manga.vibrantCoverColor = color
                }
            }
            if (manga.favorite && !(options.outWidth == -1 || options.outHeight == -1)) {
                MangaCoverMetadata.addCoverRatio(manga, options.outWidth / options.outHeight.toFloat())
            }
        }
    }

    private suspend fun awaitGetCall(manga: Manga, onlyCache: Boolean = false, forceNetwork: Boolean): Pair<Response,
        ResponseBody> {
        val call = getCall(manga, onlyCache, forceNetwork)
        val response = call.await()
        return response to checkNotNull(response.body) { "Null response source" }
    }

    private fun getCall(manga: Manga, onlyCache: Boolean, forceNetwork: Boolean): Call {
        val source = sourceManager.get(manga.source) as? HttpSource
        val client = source?.client ?: defaultClient

        val newClient = client.newBuilder().build()

        val request = Request.Builder().url(manga.thumbnail_url!!).also {
            if (source != null) {
                it.headers(source.headers)
            }
            if (forceNetwork) {
                it.cacheControl(CacheControl.FORCE_NETWORK)
            } else if (onlyCache) {
                it.cacheControl(CacheControl.FORCE_CACHE)
            }
        }.build()

        return newClient.newCall(request)
    }

    /**
     * "text/plain" is often used as a default/fallback MIME type.
     * Attempt to guess a better MIME type from the file extension.
     */
    private fun getMimeType(data: String, body: ResponseBody): String? {
        val rawContentType = body.contentType()?.toString()
        return if (rawContentType == null || rawContentType.startsWith("text/plain")) {
            MimeTypeMap.getSingleton().getMimeTypeFromUrl(data) ?: rawContentType
        } else {
            rawContentType
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

    private fun fileLoader(manga: Manga): FetchResult {
        return fileLoader(File(manga.thumbnail_url!!.substringAfter("file://")))
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceResult(
            source = file.source().buffer(),
            mimeType = "image/*",
            dataSource = DataSource.DISK
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

    private enum class Type {
        File, URL;
    }
}
