package eu.kanade.tachiyomi.util.manga

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import coil.Coil
import coil.request.ImageRequest
import com.elvishew.xlog.XLog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.recents.RecentsPresenter
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.GlobalScope
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.min

class MangaShortcutManager(
    val preferences: PreferencesHelper = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
) {

    val context: Context = preferences.context
    fun updateShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            if (!preferences.showSeriesInShortcuts()) {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                shortcutManager.removeAllDynamicShortcuts()
                return
            }
            GlobalScope.launchIO {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)

                val recentManga = if (preferences.showSeriesInShortcuts()) {
                    RecentsPresenter.getRecentManga()
                } else {
                    emptyList()
                }

                val recents =
                    (recentManga.take(shortcutManager.maxShortcutCountPerActivity))
                        .sortedByDescending { it.second }
                        .map { it.first }
                        .take(shortcutManager.maxShortcutCountPerActivity)

                val shortcuts = recents.mapNotNull { item ->
                    val request = ImageRequest.Builder(context).data(item).build()
                    val bitmap = (
                        Coil.imageLoader(context)
                            .execute(request).drawable as? BitmapDrawable
                        )?.bitmap

                    ShortcutInfo.Builder(
                        context,
                        "Manga-${item.id?.toString() ?: item.title}"
                    )
                        .setShortLabel(
                            item.title.takeUnless { it.isBlank() }
                                ?: context.getString(R.string.manga)
                        )
                        .setLongLabel(
                            item.title.takeUnless { it.isBlank() }
                                ?: context.getString(R.string.manga)
                        )
                        .setIcon(
                            if (bitmap != null) if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                Icon.createWithAdaptiveBitmap(bitmap.toSquare())
                            } else {
                                Icon.createWithBitmap(bitmap)
                            }
                            else Icon.createWithResource(context, R.drawable.ic_book_24dp)
                        )
                        .setIntent(
                            SearchActivity.openMangaIntent(context, item.id, true)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        .build()
                }

                XLog.d("Shortcuts: ${shortcuts.joinToString(", ") { it.longLabel ?: "n/a" }}")
                shortcutManager.dynamicShortcuts = shortcuts
            }
        }
    }

    private fun Bitmap.toSquare(): Bitmap? {
        val side = min(width, height)

        val xOffset = (width - side) / 2
        // Slight offset for the y, since a lil bit under the top is usually the focus of covers
        val yOffset = ((height - side) / 2 * 0.25).toInt()

        return Bitmap.createBitmap(this, xOffset, yOffset, side, side)
    }
}
