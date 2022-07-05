package eu.kanade.tachiyomi.data.image.coil

import coil.key.Keyer
import coil.request.Options
import eu.kanade.tachiyomi.util.storage.DiskUtil

class AlternativeMangaCoverKeyer : Keyer<AlternativeMangaCover> {
    override fun key(data: AlternativeMangaCover, options: Options): String {
        return DiskUtil.hashKeyForDisk(data.url)
    }
}
