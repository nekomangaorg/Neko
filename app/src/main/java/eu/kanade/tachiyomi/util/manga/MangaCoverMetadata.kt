package eu.kanade.tachiyomi.util.manga

import androidx.annotation.ColorInt
import java.util.concurrent.ConcurrentHashMap
import org.nekomanga.domain.details.MangaDetailsPreferences
import uy.kohesive.injekt.injectLazy

/** Object that holds info about a covers size ratio + dominant colors */
object MangaCoverMetadata {
    private var coverVibrantColorMap = ConcurrentHashMap<Long, Int>()

    private val mangaDetailsPreferences by injectLazy<MangaDetailsPreferences>()

    fun load() {
        mangaDetailsPreferences.coverRatios().delete()
        mangaDetailsPreferences.coverColors().delete()

        val vibrantColors = mangaDetailsPreferences.coverVibrantColors().get()
        coverVibrantColorMap =
            ConcurrentHashMap(
                vibrantColors
                    .mapNotNull {
                        it.split('|').let { parts ->
                            parts.firstOrNull()?.toLongOrNull()?.let { id ->
                                parts.lastOrNull()?.toIntOrNull()?.let { color -> id to color }
                            }
                        }
                    }
                    .toMap()
            )
    }

    fun remove(mangaId: Long) {
        coverVibrantColorMap.remove(mangaId)
    }

    fun addVibrantColor(mangaId: Long, @ColorInt color: Int) {
        coverVibrantColorMap[mangaId] = color
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
