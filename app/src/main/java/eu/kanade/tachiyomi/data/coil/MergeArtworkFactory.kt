package eu.kanade.tachiyomi.data.image.coil

import coil3.ImageLoader
import coil3.fetch.Fetcher
import coil3.request.Options
import eu.kanade.tachiyomi.data.database.models.MergeType
import eu.kanade.tachiyomi.source.SourceManager
import org.nekomanga.domain.manga.MergeArtwork
import uy.kohesive.injekt.injectLazy

class MergeArtworkFactory : Fetcher.Factory<MergeArtwork> {

    private val sourceManager: SourceManager by injectLazy()

    override fun create(data: MergeArtwork, options: Options, imageLoader: ImageLoader): Fetcher {
        return MergeMangaCoverFetcher(
            url = data.url,
            sourceLazy = lazy { MergeType.getSource(data.mergeType, sourceManager) },
            options = options,
        )
    }
}
