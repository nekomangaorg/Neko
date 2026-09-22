package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import coil3.ImageLoader
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size as CoilSize
import eu.kanade.tachiyomi.util.system.GLUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Headless coordinator for warming Coil memory bitmap cache. Manages Disposable lifecycles and
 * bounded memory sliding windows outside Jetpack Compose rendering trees.
 */
open class MemoryCacheWarmManager(
    private val context: Context,
    private val imageLoaderProvider: (() -> ImageLoader?)? = null,
    private val maxTextureSizeProvider: () -> Int = { GLUtil.maxTextureSize },
) {
    private val activeDisposables = ConcurrentHashMap<String, Disposable>()

    private fun getImageLoader(): ImageLoader? {
        if (imageLoaderProvider != null) {
            return imageLoaderProvider.invoke()
        }
        return runCatching { context.imageLoader }.getOrNull()
    }

    open fun warmMemoryCache(
        key: String,
        data: Any,
        crossfade: Boolean = false,
        onSuccess: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
    ) {
        if (isWarming(key)) return

        val loader =
            getImageLoader()
                ?: run {
                    onError?.invoke(IllegalStateException("ImageLoader is not available"))
                    return
                }

        val maxSize = maxTextureSizeProvider()
        val maxTextureBitmapSize = CoilSize(maxSize, maxSize)

        val request =
            ImageRequest.Builder(context)
                .data(data)
                .size(CoilSize.ORIGINAL)
                .maxBitmapSize(maxTextureBitmapSize)
                .precision(Precision.EXACT)
                .crossfade(crossfade)
                .listener(
                    onSuccess = { _, _ ->
                        activeDisposables.remove(key)
                        onSuccess?.invoke()
                    },
                    onError = { _, result ->
                        activeDisposables.remove(key)
                        onError?.invoke(result.throwable)
                    },
                    onCancel = { _ -> activeDisposables.remove(key) },
                )
                .build()

        activeDisposables.remove(key)?.dispose()
        val handle = loader.enqueue(request)
        if (!handle.isDisposed) {
            activeDisposables[key] = handle
        }
    }

    open fun cancel(key: String) {
        activeDisposables.remove(key)?.dispose()
    }

    open fun cancelAllExcept(retainedKeys: Set<String>) {
        val iterator = activeDisposables.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in retainedKeys) {
                entry.value.dispose()
                iterator.remove()
            }
        }
    }

    open fun isWarming(key: String): Boolean = activeDisposables[key]?.isDisposed == false

    open fun activeCount(): Int = activeDisposables.size

    open fun release() {
        activeDisposables.values.forEach { it.dispose() }
        activeDisposables.clear()
    }
}
