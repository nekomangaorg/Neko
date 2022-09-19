package eu.kanade.tachiyomi.util.manga

import android.graphics.BitmapFactory
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.getBestColor
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import uy.kohesive.injekt.injectLazy

/** Object that holds info about a covers size ratio + dominant colors */
object MangaCoverMetadata {
    private var coverRatioMap = ConcurrentHashMap<Long, Float>()
    private var coverColorMap = ConcurrentHashMap<Long, Pair<Int, Int>>()
    private var coverVibrantColorMap = ConcurrentHashMap<Long, Int>()

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

        val vibrantColors = preferences.coverVibrantColors().get()
        coverVibrantColorMap = ConcurrentHashMap(
            vibrantColors.mapNotNull {
                val splits = it.split("|")
                val id = splits.firstOrNull()?.toLongOrNull()
                val color = splits.lastOrNull()?.toIntOrNull()
                if (id != null && color != null) {
                    id to color
                } else {
                    null
                }
            }.toMap(),
        )
    }

    fun setRatioAndColors(mangaId: Long, originalThumbnailUrl: String?, inLibrary: Boolean, ogFile: File? = null, force: Boolean = false) {
        if (inLibrary.not()) {
            remove(mangaId)
        }

        val vibrantColor = getVibrantColor(mangaId)
        val dominantColors = getColors(mangaId)

        if (vibrantColor != null && inLibrary.not()) return

        val file = ogFile ?: coverCache.getCustomCoverFile(mangaId).takeIf { it.exists() } ?: coverCache.getCoverFile(originalThumbnailUrl, inLibrary)
        // if the file exists and the there was still an error then the file is corrupted
        if (file.exists()) {
            val options = BitmapFactory.Options()
            val hasVibrantColor = when (inLibrary) {
                true -> vibrantColor != null
                false -> true
            }

            if (dominantColors != null && hasVibrantColor && !force) {
                options.inJustDecodeBounds = true
            } else {
                options.inSampleSize = 4
            }
            val bitmap = BitmapFactory.decodeFile(file.path, options)
            if (bitmap != null) {
                Palette.from(bitmap).generate {
                    if (it == null) return@generate
                    if (inLibrary) {
                        it.dominantSwatch?.let { swatch ->
                            addCoverColor(mangaId, swatch.rgb, swatch.titleTextColor)
                        }
                    }
                    val color = it.getBestColor() ?: return@generate
                    addVibrantColor(mangaId, color)
                }
            }
            if (inLibrary && !(options.outWidth == -1 || options.outHeight == -1)) {
                addCoverRatio(mangaId, options.outWidth / options.outHeight.toFloat())
            }
        }
    }

    fun setRatioAndColors(manga: Manga, ogFile: File? = null, force: Boolean = false) {
        manga.id ?: return
        setRatioAndColors(manga.id!!, manga.thumbnail_url, manga.favorite, ogFile, force)
    }

    fun remove(manga: Manga) {
        val id = manga.id ?: return
        remove(id)
    }

    fun remove(mangaId: Long) {
        coverRatioMap.remove(mangaId)
        coverColorMap.remove(mangaId)
        coverVibrantColorMap.remove(mangaId)
    }

    fun addCoverRatio(mangaId: Long, ratio: Float) {
        coverRatioMap[mangaId] = ratio
    }

    fun addCoverColor(mangaId: Long, @ColorInt color: Int, @ColorInt textColor: Int) {
        coverColorMap[mangaId] = color to textColor
    }

    fun addVibrantColor(mangaId: Long, @ColorInt color: Int) {
        coverVibrantColorMap[mangaId] = color
    }

    fun getColors(mangaId: Long): Pair<Int, Int>? {
        return coverColorMap[mangaId]
    }

    fun getRatio(manga: Manga): Float? {
        return coverRatioMap[manga.id]
    }

    fun getVibrantColor(mangaId: Long): Int? {
        return coverVibrantColorMap[mangaId]
    }

    fun savePrefs() {
        val mapCopy = coverRatioMap.toMap()
        preferences.coverRatios().set(mapCopy.map { "${it.key}|${it.value}" }.toSet())
        val mapColorCopy = coverColorMap.toMap()
        preferences.coverColors().set(mapColorCopy.map { "${it.key}|${it.value.first}|${it.value.second}" }.toSet())
        val vibrantColorCopy = coverVibrantColorMap.toMap()
        preferences.coverVibrantColors().set(vibrantColorCopy.map { "${it.key}|${it.value}" }.toSet())
    }
}
