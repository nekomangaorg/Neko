package eu.kanade.tachiyomi.data.image.coil

import coil.key.Keyer
import coil.request.Options
import org.nekomanga.domain.manga.Artwork
import tachiyomi.core.util.storage.DiskUtil

class ArtworkKeyer : Keyer<Artwork> {
    override fun key(data: Artwork, options: Options): String? {
        if (data.url.isBlank()) {
            if (data.originalArtwork.isBlank()) return null
            return when (data.inLibrary) {
                true -> DiskUtil.hashKeyForDisk(data.originalArtwork)
                false -> data.originalArtwork
            }
        } else {
            return DiskUtil.hashKeyForDisk(data.url)
        }
    }
}
