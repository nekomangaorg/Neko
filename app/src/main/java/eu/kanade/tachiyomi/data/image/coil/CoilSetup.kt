package eu.kanade.tachiyomi.data.image.coil

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import eu.kanade.tachiyomi.network.NetworkHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CoilSetup(context: Context) {
    init {
        val imageLoader = ImageLoader.Builder(context).apply {
            val callFactoryInit = { Injekt.get<NetworkHelper>().nonRateLimitedClient }
            val diskCacheInit = { CoilDiskCache.get(context) }
            components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
                add(TachiyomiImageDecoder.Factory())
                add(MangaCoverFetcher.Factory(lazy(callFactoryInit), lazy(diskCacheInit)))
                add(MangaCoverKeyer())
            }
            callFactory(callFactoryInit)
            diskCache(diskCacheInit)
            memoryCache { MemoryCache.Builder(context).maxSizePercent(0.40).build() }
            crossfade(true)
            allowRgb565(context.getSystemService<ActivityManager>()!!.isLowRamDevice)
            allowHardware(true)
        }.build()
        Coil.setImageLoader(imageLoader)
    }
}

/**
 * Direct copy of Coil's internal SingletonDiskCache so that [MangaCoverFetcher] can access it.
 */
internal object CoilDiskCache {

    private const val FOLDER_NAME = "image_cache"
    private var instance: DiskCache? = null

    @Synchronized
    fun get(context: Context): DiskCache {
        return instance ?: run {
            val safeCacheDir = context.cacheDir.apply { mkdirs() }
            // Create the singleton disk cache instance.
            DiskCache.Builder()
                .directory(safeCacheDir.resolve(FOLDER_NAME))
                .build()
                .also { instance = it }
        }
    }
}
