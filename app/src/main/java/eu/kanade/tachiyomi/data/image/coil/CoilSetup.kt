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
import coil.util.DebugLogger
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import org.nekomanga.core.network.NetworkPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class CoilSetup(context: Context) {
    init {
        val imageLoader =
            ImageLoader.Builder(context)
                .apply {
                    val callFactoryInit = { Injekt.get<NetworkHelper>().cdnClient }
                    val diskCacheInit = { CoilDiskCache.get(context) }
                    val isCurrSDKPieOrGreater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    components {
                        if (isCurrSDKPieOrGreater) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                        add(SvgDecoder.Factory())
                        add(TachiyomiImageDecoder.Factory())
                        add(MangaCoverFactory(lazy(callFactoryInit), lazy(diskCacheInit)))
                        add(ArtworkFactory(lazy(callFactoryInit), lazy(diskCacheInit)))
                        add(MergeArtworkFactory())
                        add(ArtworkKeyer())
                    }
                    callFactory(callFactoryInit)
                    diskCache(diskCacheInit)
                    memoryCache { MemoryCache.Builder(context).maxSizePercent(0.40).build() }
                    crossfade(true)
                    allowRgb565(context.getSystemService<ActivityManager>()!!.isLowRamDevice)
                    allowHardware(isCurrSDKPieOrGreater)
                    if (Injekt.get<NetworkPreferences>().verboseLogging().get()) {
                        logger(DebugLogger())
                    }
                    // Coil spawns a new thread for every image load by default
                    fetcherDispatcher(Dispatchers.IO.limitedParallelism(8))
                    decoderDispatcher(Dispatchers.IO.limitedParallelism(2))
                    transformationDispatcher(Dispatchers.IO.limitedParallelism(2))
                }
                .build()
        Coil.setImageLoader(imageLoader)
    }
}

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
