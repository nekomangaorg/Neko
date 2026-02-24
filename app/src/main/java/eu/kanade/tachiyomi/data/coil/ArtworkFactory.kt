package eu.kanade.tachiyomi.data.image.coil

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.fetch.Fetcher
import coil3.request.Options
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.source.SourceManager
import org.nekomanga.domain.manga.Artwork
import uy.kohesive.injekt.injectLazy

class ArtworkFactory(private val diskCacheLazy: Lazy<DiskCache>) : Fetcher.Factory<Artwork> {

    private val coverCache: CoverCache by injectLazy()
    private val sourceManager: SourceManager by injectLazy()

    override fun create(data: Artwork, options: Options, imageLoader: ImageLoader): Fetcher {
        return when (data.cover.isBlank()) {
            true ->
                MangaCoverFetcher(
                    altUrl = data.dynamicCover,
                    inLibrary = data.inLibrary,
                    mangaId = data.mangaId,
                    originalThumbnailUrl = data.originalCover,
                    sourceLazy = lazy { sourceManager.mangaDex },
                    options = options,
                    coverCache = coverCache,
                    diskCacheLazy = diskCacheLazy,
                )
            // If 'cover' has a value, it is specific artwork (Priority 1)
            false ->
                AlternativeMangaCoverFetcher(
                    url = data.cover,
                    mangaId = data.mangaId,
                    sourceLazy = lazy { sourceManager.mangaDex },
                    options = options,
                    coverCache = coverCache,
                    diskCacheLazy = diskCacheLazy,
                )
        }
    }
}
