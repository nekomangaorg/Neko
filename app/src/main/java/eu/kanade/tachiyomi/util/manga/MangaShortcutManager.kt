package eu.kanade.tachiyomi.util.manga

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.recents.RecentsPresenter
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.GlobalScope
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import kotlin.math.min

class MangaShortcutManager(
    val preferences: PreferencesHelper = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get()
) {

    val context: Context = preferences.context
    fun updateShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            GlobalScope.launchIO {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)

                val recentManga = RecentsPresenter.getRecentManga()
                val shortcuts = recentManga.subList(
                    0,
                    min(
                        recentManga.size,
                        shortcutManager.maxShortcutCountPerActivity
                    )
                ).map { manga ->
                    val customCoverFile = coverCache.getCustomCoverFile(manga)
                    val coverFile = if (customCoverFile.exists()) {
                        customCoverFile
                    } else {
                        val coverFile = coverCache.getCoverFile(manga)
                        if (coverFile.exists()) {
                            if (!manga.favorite) {
                                coverFile.setLastModified(Date().time)
                            }
                            coverFile
                        } else {
                            null
                        }
                    }
                    val bitmap = if (coverFile != null) {
                        BitmapFactory.decodeFile(coverFile.path)
                    } else {
                        null
                    }

                    ShortcutInfo.Builder(context, "Manga-${manga.id?.toString() ?: manga.title}")
                        .setShortLabel(manga.title)
                        .setLongLabel(manga.title)
                        .setIcon(
                            if (bitmap != null) if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                Icon.createWithAdaptiveBitmap(bitmap.toSquare())
                            } else {
                                Icon.createWithBitmap(bitmap)
                            }
                            else Icon.createWithResource(context, R.drawable.ic_book_24dp)
                        )
                        .setIntent(
                            Intent(
                                context,
                                SearchActivity::class.java
                            ).setAction(MainActivity.SHORTCUT_MANGA)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                .putExtra(MangaDetailsController.MANGA_EXTRA, manga.id)
                        )
                        .build()
                }
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
