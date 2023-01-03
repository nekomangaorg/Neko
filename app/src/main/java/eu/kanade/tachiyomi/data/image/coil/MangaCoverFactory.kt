package eu.kanade.tachiyomi.data.image.coil

import coil.ImageLoader
import coil.disk.DiskCache
import coil.fetch.Fetcher
import coil.request.Options
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import okhttp3.Call
import uy.kohesive.injekt.injectLazy

class MangaCoverFactory(
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
            sourceLazy = lazy { sourceManager.mangaDex },
            options = options,
            coverCache = coverCache,
            callFactoryLazy = callFactoryLazy,
            diskCacheLazy = diskCacheLazy,
        )
    }
}
