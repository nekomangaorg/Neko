package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.os.Build
import android.view.WindowManager
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size as CoilSize
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.domain.CheckTallPageUseCase
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.util.system.GLUtil
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Headless preload coordinator managing disk prefetching, bitmap memory cache warming, and
 * background tall page splitting. Decoupled from Jetpack Compose lifecycles.
 */
class ReaderPreloadEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val checkTallPage: CheckTallPageUseCase = CheckTallPageUseCase(),
    private val onPageSplit: ((ReaderPage, List<ReaderPageSplit>) -> Unit)? = null,
    private val isSplitTallPagesEnabled: () -> Boolean = { false },
    private val getScreenHeight: () -> Int = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            windowManager?.currentWindowMetrics?.bounds?.height()
                ?: context.resources.displayMetrics.heightPixels
        } else {
            @Suppress("DEPRECATION") context.resources.displayMetrics.heightPixels
        }
    },
) {
    private val preloadedDisk = Collections.synchronizedSet(mutableSetOf<String>())
    private val preloadedMemory = Collections.synchronizedSet(mutableSetOf<String>())
    private val activeDisposables = ConcurrentHashMap<String, Disposable>()
    private val checkedTallPages = Collections.synchronizedSet(mutableSetOf<ReaderPage>())
    private var activeJob: Job? = null

    private val maxTextureBitmapSize = CoilSize(GLUtil.maxTextureSize, GLUtil.maxTextureSize)

    fun updateActiveIndex(activeIndex: Int, items: List<ReaderUiItem>, preloadAmount: Int) {
        activeJob?.cancel()
        activeJob =
            scope.launch(Dispatchers.IO) {
                delay(50L) // Debounce fast scrolling flings
                val windowStart = (activeIndex - 2).coerceAtLeast(0)
                val windowEnd = (activeIndex + preloadAmount).coerceAtMost(items.lastIndex)
                val memoryEnd = (activeIndex + 2).coerceAtMost(items.lastIndex)

                coroutineScope {
                    for (i in windowStart..windowEnd) {
                        val item = items.getOrNull(i) ?: continue
                        preloadItem(this, item, preloadMemory = i in activeIndex..memoryEnd)
                    }
                }
            }
    }

    private fun preloadItem(itemScope: CoroutineScope, item: ReaderUiItem, preloadMemory: Boolean) {
        val key = item.key("webtoon")
        // 1. Disk Preload
        if (preloadedDisk.add(key)) {
            when (item) {
                is ReaderUiItem.Page -> {
                    itemScope.launch(Dispatchers.IO) {
                        try {
                            item.page.chapter.pageLoader?.loadPage(item.page)
                        } catch (e: Exception) {
                            if (e !is CancellationException) {
                                preloadedDisk.remove(key)
                            }
                        }
                    }
                    if (isSplitTallPagesEnabled()) {
                        checkAndSplitTallPage(itemScope, item.page, preloadMemory)
                    }
                }
                is ReaderUiItem.SplitPage -> {
                    itemScope.launch(Dispatchers.IO) {
                        try {
                            item.page.chapter.pageLoader?.loadPage(item.page)
                        } catch (e: Exception) {
                            if (e !is CancellationException) {
                                preloadedDisk.remove(key)
                            }
                        }
                    }
                }
                is ReaderUiItem.Transition -> Unit
            }
        }

        // 2. Memory Preload
        if (preloadMemory && preloadedMemory.add(key)) {
            when (item) {
                is ReaderUiItem.Page -> {
                    if (!isSplitTallPagesEnabled() || checkedTallPages.contains(item.page)) {
                        warmMemoryCache(key, item.page)
                    }
                }
                is ReaderUiItem.SplitPage -> {
                    warmMemoryCache(key, item.split)
                }
                is ReaderUiItem.Transition -> Unit
            }
        }
    }

    private fun checkAndSplitTallPage(
        itemScope: CoroutineScope,
        page: ReaderPage,
        preloadMemory: Boolean,
    ) {
        if (!checkedTallPages.add(page)) return
        itemScope.launch(Dispatchers.IO) {
            try {
                page.statusFlow.first { it == Page.State.READY }
                val screenHeight = getScreenHeight()
                val splits = checkTallPage(page, screenHeight)
                if (splits != null && splits.isNotEmpty()) {
                    onPageSplit?.invoke(page, splits)
                    if (preloadMemory) {
                        splits.forEach { split ->
                            val splitKey =
                                "webtoon_split_${split.page.chapter.chapter.id}_${split.page.index}_${split.topOffset}"
                            if (preloadedMemory.add(splitKey)) {
                                warmMemoryCache(splitKey, split)
                            }
                        }
                    }
                } else if (preloadMemory) {
                    val key =
                        page.chapter.chapter.id?.let { cid -> "webtoon_page_${cid}_${page.index}" }
                    if (key != null && preloadedMemory.add(key)) {
                        warmMemoryCache(key, page)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    checkedTallPages.remove(page)
                }
            }
        }
    }

    private fun warmMemoryCache(key: String, data: Any) {
        val request =
            ImageRequest.Builder(context)
                .data(data)
                .size(CoilSize.ORIGINAL)
                .maxBitmapSize(maxTextureBitmapSize)
                .precision(Precision.EXACT)
                .crossfade(true)
                .build()

        activeDisposables[key]?.dispose()
        activeDisposables[key] = context.imageLoader.enqueue(request)
    }

    fun clear() {
        activeJob?.cancel()
        activeDisposables.values.forEach { it.dispose() }
        activeDisposables.clear()
        preloadedDisk.clear()
        preloadedMemory.clear()
        checkedTallPages.clear()
    }
}
