package eu.kanade.tachiyomi.util.manga

import androidx.annotation.ColorInt
import java.util.concurrent.ConcurrentHashMap
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.injectLazy

/** Object that holds info about a covers size ratio + dominant colors */
object MangaCoverMetadata {
    private val coverVibrantColorMap = ConcurrentHashMap<Long, Int>()
    private val mapLock = Any()

    private val mangaDetailsPreferences by injectLazy<MangaDetailsPreferences>()

    fun load() {
        mangaDetailsPreferences.coverRatios().delete()
        mangaDetailsPreferences.coverColors().delete()

        val vibrantColors = mangaDetailsPreferences.coverVibrantColors().get()
        val parsedColors =
            vibrantColors
                .mapNotNull {
                    it.split('|').let { parts ->
                        parts.firstOrNull()?.toLongOrNull()?.let { id ->
                            parts.lastOrNull()?.toIntOrNull()?.let { color -> id to color }
                        }
                    }
                }
                .toMap()

        synchronized(mapLock) {
            val existing = coverVibrantColorMap.toMap()
            coverVibrantColorMap.clear()
            coverVibrantColorMap.putAll(parsedColors)
            coverVibrantColorMap.putAll(existing)
        }
    }

    fun remove(mangaId: Long) {
        synchronized(mapLock) { coverVibrantColorMap.remove(mangaId) }
    }

    fun addVibrantColor(mangaId: Long, @ColorInt color: Int) {
        synchronized(mapLock) { coverVibrantColorMap[mangaId] = color }
    }

    fun getVibrantColor(mangaId: Long): Int? {
        return coverVibrantColorMap[mangaId]
    }

    fun savePrefs() {
        val vibrantColorCopy = coverVibrantColorMap.toMap()
        mangaDetailsPreferences
            .coverVibrantColors()
            .set(vibrantColorCopy.map { "${it.key}|${it.value}" }.toSet())
    }
}
