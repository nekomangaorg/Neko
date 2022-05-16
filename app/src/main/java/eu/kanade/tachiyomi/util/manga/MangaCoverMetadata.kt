package eu.kanade.tachiyomi.util.manga

import android.graphics.BitmapFactory
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.getBestColor
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Object that holds info about a covers size ratio + dominant colors */
object MangaCoverMetadata {
    private var coverRatioMap = ConcurrentHashMap<Long, Float>()
    private var coverColorMap = ConcurrentHashMap<Long, Pair<Int, Int>>()
    private val preferences by injectLazy<PreferencesHelper>()
    private val coverCache by injectLazy<CoverCache>()

    fun load() {
        val ratios = preferences.coverRatios().get()
        coverRatioMap = ConcurrentHashMap(
            ratios.mapNotNull {
                val splits = it.split("|")
                val id = splits.firstOrNull()?.toLongOrNull()
                val ratio = splits.lastOrNull()?.toFloatOrNull()
                if (id != null && ratio != null) {
                    id to ratio
                } else {
                    null
                }
            }.toMap(),
        )
        val colors = preferences.coverColors().get()
        coverColorMap = ConcurrentHashMap(
            colors.mapNotNull {
                val splits = it.split("|")
                val id = splits.firstOrNull()?.toLongOrNull()
                val color = splits.getOrNull(1)?.toIntOrNull()
                val textColor = splits.getOrNull(2)?.toIntOrNull()
                if (id != null && color != null) {
                    id to (color to (textColor ?: 0))
                } else {
                    null
                }
            }.toMap(),
        )
    }

    fun setRatioAndColors(manga: Manga, ogFile: File? = null, force: Boolean = false) {
        if (!manga.favorite) {
            MangaCoverMetadata.remove(manga)
        }
        if (manga.vibrantCoverColor != null && !manga.favorite) return
        val file = ogFile ?: coverCache.getCustomCoverFile(manga).takeIf { it.exists() } ?: coverCache.getCoverFile(manga)
        // if the file exists and the there was still an error then the file is corrupted
        if (file.exists()) {
            val options = BitmapFactory.Options()
            val hasVibrantColor = if (manga.favorite) manga.vibrantCoverColor != null else true
            if (manga.dominantCoverColors != null && hasVibrantColor && !force) {
                options.inJustDecodeBounds = true
            } else {
                options.inSampleSize = 4
            }
            val bitmap = BitmapFactory.decodeFile(file.path, options) ?: return
            if (!options.inJustDecodeBounds) {
                Palette.from(bitmap).generate {
                    if (it == null) return@generate
                    if (manga.favorite) {
                        it.dominantSwatch?.let { swatch ->
                            manga.dominantCoverColors = swatch.rgb to swatch.titleTextColor
                        }
                    }
                    val color = it.getBestColor() ?: return@generate
                    manga.vibrantCoverColor = color
                }
            }
            if (manga.favorite && !(options.outWidth == -1 || options.outHeight == -1)) {
                addCoverRatio(manga, options.outWidth / options.outHeight.toFloat())
            }
        }
    }

    fun remove(manga: Manga) {
        val id = manga.id ?: return
        coverRatioMap.remove(id)
        coverColorMap.remove(id)
    }

    fun addCoverRatio(manga: Manga, ratio: Float) {
        val id = manga.id ?: return
        coverRatioMap[id] = ratio
    }

    fun addCoverColor(manga: Manga, @ColorInt color: Int, @ColorInt textColor: Int) {
        val id = manga.id ?: return
        coverColorMap[id] = color to textColor
    }

    fun getColors(manga: Manga): Pair<Int, Int>? {
        return coverColorMap[manga.id]
    }

    fun getRatio(manga: Manga): Float? {
        return coverRatioMap[manga.id]
    }

    fun savePrefs() {
        val mapCopy = coverRatioMap.toMap()
        preferences.coverRatios().set(mapCopy.map { "${it.key}|${it.value}" }.toSet())
        val mapColorCopy = coverColorMap.toMap()
        preferences.coverColors().set(mapColorCopy.map { "${it.key}|${it.value.first}|${it.value.second}" }.toSet())
    }
}
