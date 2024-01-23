package eu.kanade.tachiyomi.data.image.coil

import coil.ImageLoader
import coil.disk.DiskCache
import coil.fetch.Fetcher
import coil.request.Options
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.SourceManager
import okhttp3.Call
import org.nekomanga.domain.manga.Artwork
import uy.kohesive.injekt.injectLazy

class ArtworkFactory(
    private val callFactoryLazy: Lazy<Call.Factory>,
    private val diskCacheLazy: Lazy<DiskCache>,
) : Fetcher.Factory<Artwork> {

    private val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by injectLazy()

    override fun create(data: Artwork, options: Options, imageLoader: ImageLoader): Fetcher {
        return when (data.url.isBlank()) {
            true ->
                MangaCoverFetcher(
                    altUrl = data.url,
                    inLibrary = data.inLibrary,
                    mangaId = data.mangaId,
                    originalThumbnailUrl = data.originalArtwork,
                    sourceLazy = lazy { sourceManager.mangaDex },
                    options = options,
                    coverCache = coverCache,
                    callFactoryLazy = callFactoryLazy,
                    diskCacheLazy = diskCacheLazy,
                )
            false ->
                AlternativeMangaCoverFetcher(
                    url = data.url,
                    mangaId = data.mangaId,
                    sourceLazy = lazy { sourceManager.mangaDex },
                    options = options,
                    coverCache = coverCache,
                    callFactoryLazy = callFactoryLazy,
                    diskCacheLazy = diskCacheLazy,
                )
        }
    }
}
