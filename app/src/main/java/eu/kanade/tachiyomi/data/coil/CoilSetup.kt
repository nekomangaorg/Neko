package eu.kanade.tachiyomi.data.image.coil

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import eu.kanade.tachiyomi.data.coil.BufferedSourceFetcher
import eu.kanade.tachiyomi.network.NetworkHelper
import org.nekomanga.core.network.NetworkPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

fun coilImageLoader(context: Context) =
    ImageLoader.Builder(context)
        .apply {
            val callFactoryInit = { Injekt.get<NetworkHelper>().cdnClient }
            val diskCacheInit = { CoilDiskCache.get(context) }
            val isCurrSDKPieOrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            components {
                if (isCurrSDKPieOrGreater) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
                add(TachiyomiImageDecoder.Factory())
                add(MangaCoverFactory(lazy(callFactoryInit), lazy(diskCacheInit)))
                add(ArtworkFactory(lazy(callFactoryInit), lazy(diskCacheInit)))
                add(MergeArtworkFactory())
                add(ArtworkKeyer())
                add(BufferedSourceFetcher.Factory())
            }
            diskCache(diskCacheInit)
            memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.40).build() }
            crossfade(true)
            allowRgb565(context.getSystemService<ActivityManager>()!!.isLowRamDevice)
            allowHardware(isCurrSDKPieOrGreater)
            if (Injekt.get<NetworkPreferences>().verboseLogging().get()) {
                logger(coil3.util.DebugLogger())
            }
        }
        .build()

/** Direct copy of Coil's internal SingletonDiskCache so that [MangaCoverFetcher] can access it. */
internal object CoilDiskCache {

    const val FOLDER_NAME = "image_cache"
    private var instance: DiskCache? = null

    @Synchronized
    fun get(context: Context): DiskCache {
        return instance
            ?: run {
                val safeCacheDir = context.cacheDir.apply { mkdirs() }
                // Create the singleton disk cache instance.
                DiskCache.Builder().directory(safeCacheDir.resolve(FOLDER_NAME)).build().also {
                    instance = it
                }
            }
    }
}
