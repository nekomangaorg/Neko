package eu.kanade.tachiyomi.util.manga

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.feed.FeedRepository
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.toDisplayManga
import kotlin.math.min
import kotlinx.coroutines.GlobalScope
import org.nekomanga.R
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaShortcutManager(
    val preferences: PreferencesHelper = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
) {

    val context: Context = preferences.context

    fun updateShortcuts() {
        if (!preferences.showSeriesInShortcuts().get()) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            shortcutManager.removeAllDynamicShortcuts()
            return
        }
        GlobalScope.launchIO {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)

            val recentManga =
                if (preferences.showSeriesInShortcuts().get()) {
                    FeedRepository.getRecentlyReadManga()
                } else {
                    emptyList()
                }

            val recents =
                (recentManga.take(shortcutManager.maxShortcutCountPerActivity)).take(
                    shortcutManager.maxShortcutCountPerActivity
                )

            val shortcuts =
                recents.map { item ->
                    val request =
                        ImageRequest.Builder(context)
                            .data(item.toDisplayManga().currentArtwork)
                            .build()
                    val bitmap = context.imageLoader.execute(request).image?.toBitmap()

                    ShortcutInfo.Builder(context, "Manga-${item.id.toString() ?: item.title}")
                        .setShortLabel(
                            item.title.takeUnless { it.isBlank() }
                                ?: context.getString(R.string.manga)
                        )
                        .setLongLabel(
                            item.title.takeUnless { it.isBlank() }
                                ?: context.getString(R.string.manga)
                        )
                        .setIcon(
                            if (bitmap != null) {
                                Icon.createWithAdaptiveBitmap(bitmap.toSquare())
                            } else {
                                Icon.createWithResource(context, R.drawable.ic_book_24dp)
                            }
                        )
                        .setIntent(
                            MainActivity.openMangaIntent(context, item.id!!, true)
                                .addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                                )
                        )
                        .build()
                }

            TimberKt.d { "Shortcuts: ${shortcuts.joinToString(", ") { it.longLabel ?: "n/a" }}" }
            shortcutManager.dynamicShortcuts = shortcuts
        }
    }

    private fun Bitmap.toSquare(): Bitmap {
        val side = min(width, height)

        val xOffset = (width - side) / 2
        // Slight offset for the y, since a lil bit under the top is usually the focus of covers
        val yOffset = ((height - side) / 2 * 0.25).toInt()

        return Bitmap.createBitmap(this, xOffset, yOffset, side, side)
    }
}
