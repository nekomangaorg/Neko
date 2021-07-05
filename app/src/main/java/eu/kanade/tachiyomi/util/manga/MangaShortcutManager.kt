package eu.kanade.tachiyomi.util.manga

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import coil.Coil
import coil.request.ImageRequest
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.recents.RecentsPresenter
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.util.system.launchIO
import kotlinx.coroutines.GlobalScope
import timber.log.Timber
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.min

class MangaShortcutManager(
    val preferences: PreferencesHelper = Injekt.get(),
    val db: DatabaseHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get()
) {

    val context: Context = preferences.context
    fun updateShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            if (!preferences.showSeriesInShortcuts() && !preferences.showSourcesInShortcuts()) {
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
                val recentSources = if (preferences.showSourcesInShortcuts()) {
                    preferences.lastUsedSources().get().mapNotNull {
                    val splitS = it.split(":")
                    splitS.first().toLongOrNull()?.let { id ->
                        sourceManager.getMangadex() to splitS[1].toLong()
                    }
                }
                } else {
                    emptyList()
                }
                val recents =
                    (recentManga.take(shortcutManager.maxShortcutCountPerActivity) + recentSources)
                        .sortedByDescending { it.second }
                        .map { it.first }
                        .take(shortcutManager.maxShortcutCountPerActivity)

                val shortcuts = recents.mapNotNull { item ->
                    when (item) {
                        is Manga -> {
                            val request = ImageRequest.Builder(context).data(item).build()
                            val bitmap = (
                                Coil.imageLoader(context)
                                    .execute(request).drawable as? BitmapDrawable
                                )?.bitmap

                            ShortcutInfo.Builder(
                                context,
                                "Manga-${item.id?.toString() ?: item.title}"
                            )
                                .setShortLabel(item.title.takeUnless { it.isBlank() } ?: context.getString(R.string.manga))
                                .setLongLabel(item.title.takeUnless { it.isBlank() } ?: context.getString(R.string.manga))
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
                        is Source -> {
                            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_tracker_mangadex_logo)

                            ShortcutInfo.Builder(context, "Source-${item.id}")
                                .setShortLabel(item.name)
                                .setLongLabel(item.name)
                                .setIcon(
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        Icon.createWithAdaptiveBitmap(bitmap.toSquare())
                                    } else {
                                        Icon.createWithBitmap(bitmap)
                                    }
                                )
                                .setIntent(
                                    Intent(
                                        context,
                                        SearchActivity::class.java
                                    ).setAction(MainActivity.SHORTCUT_SOURCE)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        .putExtra(BrowseSourceController.SOURCE_ID_KEY, item.id)
                                )
                                .build()
                        }
                        else -> {
                            null
                        }
                    }
                }
                Timber.d("Shortcuts: ${shortcuts.joinToString(", ") { it.longLabel ?: "n/a" }}")
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
