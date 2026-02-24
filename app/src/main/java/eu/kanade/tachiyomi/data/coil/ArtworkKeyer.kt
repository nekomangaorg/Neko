package eu.kanade.tachiyomi.data.coil

import coil3.getExtra
import coil3.key.Keyer
import coil3.request.Options
import org.nekomanga.domain.manga.Artwork
import tachiyomi.core.util.storage.DiskUtil

class ArtworkKeyer : Keyer<Artwork> {
    override fun key(data: Artwork, options: Options): String? {
        // Priority 1: Specific/Alternative Artwork or User Custom Cover
        if (data.cover.isNotBlank()) {
            return DiskUtil.hashKeyForDisk(data.cover)
        }

        // Priority 2 & 3: Main Manga Cover
        if (data.originalCover.isBlank()) return null

        val useDynamic = options.getExtra(DynamicCoverKey)

        // FIX: If dynamic cover is enabled and available, use its URL for the cache key.
        // Because the URL changes per volume, Coil will automatically fetch the new image!
        if (useDynamic && data.dynamicCover.isNotBlank()) {
            return DiskUtil.hashKeyForDisk(data.dynamicCover)
        }

        // Priority 1: Fallback to the Default Cover
        return when (data.inLibrary) {
            true -> DiskUtil.hashKeyForDisk(data.originalCover)
            false -> data.originalCover
        }
    }
}
