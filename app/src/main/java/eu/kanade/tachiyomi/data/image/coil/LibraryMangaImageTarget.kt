package eu.kanade.tachiyomi.data.image.coil

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.Disposable
import coil.request.ImageRequest
import coil.target.ImageViewTarget
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.system.launchIO
import uy.kohesive.injekt.injectLazy

class LibraryMangaImageTarget(
    override val view: ImageView,
    val manga: Manga,
) : ImageViewTarget(view) {

    private val coverCache: CoverCache by injectLazy()

    override fun onError(error: Drawable?) {
        super.onError(error)
        if (manga.favorite) {
            launchIO {
                val file = coverCache.getCoverFile(manga)
                // if the file exists and the there was still an error then the file is corrupted
                if (file.exists()) {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(file.path, options)
                    if (options.outWidth == -1 || options.outHeight == -1) {
                        file.delete()
                        view.context.imageLoader.memoryCache?.remove(MemoryCache.Key(manga.key()))
                    }
                }
            }
        }
    }
}

@JvmSynthetic
inline fun ImageView.loadManga(
    manga: Manga,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {},
): Disposable {
    val request = ImageRequest.Builder(context)
        .data(manga)
        .target(LibraryMangaImageTarget(this, manga))
        .apply(builder)
        .memoryCacheKey(manga.key())
        .build()
    return imageLoader.enqueue(request)
}

fun Palette.getBestColor(defaultColor: Int) = getBestColor() ?: defaultColor

fun Palette.getBestColor(): Int? {
    val vibPopulation = vibrantSwatch?.population ?: -1
    val domLum = dominantSwatch?.hsl?.get(2) ?: -1f
    val mutedPopulation = mutedSwatch?.population ?: -1
    val mutedSaturationLimit = if (mutedPopulation > vibPopulation * 3f) 0.1f else 0.25f
    return when {
        (dominantSwatch?.hsl?.get(1) ?: 0f) >= .25f &&
            domLum <= .8f && domLum > .2f -> dominantSwatch?.rgb
        vibPopulation >= mutedPopulation * 0.75f -> vibrantSwatch?.rgb
        mutedPopulation > vibPopulation * 1.5f &&
            (mutedSwatch?.hsl?.get(1) ?: 0f) > mutedSaturationLimit -> mutedSwatch?.rgb
        else -> arrayListOf(vibrantSwatch, lightVibrantSwatch, darkVibrantSwatch).maxByOrNull {
            if (it === vibrantSwatch) (it?.population ?: -1) * 3 else it?.population ?: -1
        }?.rgb
    }
}
