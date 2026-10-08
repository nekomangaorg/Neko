package eu.kanade.tachiyomi.util.manga

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.main.MainActivity
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.logging.TimberKt
import org.nekomanga.presentation.screens.feed.FeedRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaShortcutManager(
    val preferences: PreferencesHelper = Injekt.get(),
    val coverCache: CoverCache = Injekt.get(),
    val sourceManager: SourceManager = Injekt.get(),
    val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    val context: Context = preferences.context

    suspend fun updateShortcuts() {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        if (!preferences.showSeriesInShortcuts().get()) {
            try {
                shortcutManager.removeAllDynamicShortcuts()
            } catch (e: Exception) {
                TimberKt.e(e) { "Failed to remove dynamic shortcuts" }
            }
            return
        }
        withContext(ioDispatcher) {
            val recentManga =
                if (preferences.showSeriesInShortcuts().get()) {
                    FeedRepository.getRecentlyReadManga()
                } else {
                    emptyList()
                }

            val recents = recentManga.take(shortcutManager.maxShortcutCountPerActivity)

            val shortcuts = recents.mapNotNull { item ->
                try {
                    val request =
                        ImageRequest.Builder(context)
                            .data(item.toDisplayManga().currentArtwork)
                            .allowHardware(false)
                            .build()
                    val bitmap = context.imageLoader.execute(request).image?.toBitmap()

                    val icon =
                        if (bitmap != null && !bitmap.isRecycled) {
                            try {
                                Icon.createWithAdaptiveBitmap(bitmap.toSquare())
                            } catch (e: Exception) {
                                TimberKt.e(e) { "Failed to create adaptive icon for ${item.title}" }
                                Icon.createWithResource(context, R.drawable.ic_book_24dp)
                            }
                        } else {
                            Icon.createWithResource(context, R.drawable.ic_book_24dp)
                        }

                    ShortcutInfo.Builder(context, "Manga-${item.id.toString() ?: item.title}")
                        .setShortLabel(
                            item.title.takeUnless { it.isBlank() }
                                ?: context.getString(R.string.manga)
                        )
                        .setLongLabel(
                            item.title.takeUnless { it.isBlank() }
                                ?: context.getString(R.string.manga)
                        )
                        .setIcon(icon)
                        .setIntent(
                            MainActivity.openMangaIntent(context, item.id!!, true)
                                .addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                                )
                        )
                        .build()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    TimberKt.e(e) { "Failed to create shortcut for ${item.title}" }
                    null
                }
            }

            TimberKt.d { "Shortcuts: ${shortcuts.joinToString(", ") { it.longLabel ?: "n/a" }}" }
            try {
                shortcutManager.dynamicShortcuts = shortcuts
            } catch (e: Exception) {
                TimberKt.e(e) { "Failed to set dynamic shortcuts" }
            }
        }
    }

    internal fun Bitmap.toSquare(): Bitmap {
        if (isRecycled) return this
        val side = min(width, height)
        if (side <= 0) return this

        val xOffset = (width - side) / 2
        // Slight offset for the y, since a lil bit under the top is usually the focus of covers
        val yOffset = ((height - side) / 2 * 0.25).toInt()

        val softwareBitmap =
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE
            ) {
                copy(Bitmap.Config.ARGB_8888, false) ?: this
            } else {
                this
            }

        return try {
            val output =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val cs =
                        softwareBitmap.colorSpace?.takeIf {
                            it.model == ColorSpace.Model.RGB &&
                                (it as? ColorSpace.Rgb)?.transferParameters != null
                        } ?: ColorSpace.get(ColorSpace.Named.SRGB)
                    Bitmap.createBitmap(
                        side,
                        side,
                        Bitmap.Config.ARGB_8888,
                        softwareBitmap.hasAlpha(),
                        cs,
                    )
                } else {
                    Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
                }
            output.density = density
            val canvas = Canvas(output)
            val srcRect = Rect(xOffset, yOffset, xOffset + side, yOffset + side)
            val dstRect = Rect(0, 0, side, side)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(softwareBitmap, srcRect, dstRect, paint)
            output
        } catch (e: Exception) {
            TimberKt.e(e) { "Failed to crop bitmap to square" }
            this
        } finally {
            if (softwareBitmap !== this) {
                softwareBitmap.recycle()
            }
        }
    }
}
