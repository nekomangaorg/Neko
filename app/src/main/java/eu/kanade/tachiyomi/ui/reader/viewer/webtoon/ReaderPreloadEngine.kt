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
    var onPageSplit: ((ReaderPage, List<ReaderPageSplit>) -> Unit)? = null,
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
    companion object {
        /** Hard cap on memory preloaded pages to prevent Coil LRU cache thrashing. */
        const val MAX_MEMORY_PRELOAD_PAGES = 4

        /** Hard cap on memory preloaded slices (e.g. ~100MB of bitmap memory). */
        const val MAX_MEMORY_PRELOAD_SLICES = 12

        /** Minimum slice buffer ahead of viewport. */
        const val MIN_MEMORY_PRELOAD_SLICES = 6
    }

    private val preloadedDisk = Collections.synchronizedSet(mutableSetOf<String>())
    private val preloadedMemory = Collections.synchronizedSet(mutableSetOf<String>())
    private val activeDisposables = ConcurrentHashMap<String, Disposable>()
    private val checkedTallPages = Collections.synchronizedSet(mutableSetOf<ReaderPage>())
    private var activeJob: Job? = null
    private var lastActiveIndex: Int = -1
    private var lastItems: List<ReaderUiItem>? = null
    private var lastPreloadAmount: Int = -1

    private val maxTextureBitmapSize = CoilSize(GLUtil.maxTextureSize, GLUtil.maxTextureSize)

    fun isJobActive(): Boolean = activeJob?.isActive == true

    fun updateActiveIndex(activeIndex: Int, items: List<ReaderUiItem>, preloadAmount: Int) {
        if (
            activeJob?.isActive == true &&
                lastActiveIndex == activeIndex &&
                lastItems === items &&
                lastPreloadAmount == preloadAmount
        ) {
            return
        }
        lastActiveIndex = activeIndex
        lastItems = items
        lastPreloadAmount = preloadAmount
        activeJob?.cancel()
        activeJob =
            scope.launch(Dispatchers.IO) {
                delay(50L) // Debounce fast scrolling flings

                val safeStart = activeIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))

                // 1. Backward window (1 page behind)
                val windowStart =
                    calculateWindowStart(
                        startIndex = safeStart,
                        items = items,
                        pageBudget = 1,
                        minItems = 3,
                    )

                // 2. Disk prefetch window: respects full user preference
                val windowEnd =
                    calculateWindowEnd(
                        startIndex = safeStart,
                        items = items,
                        pageBudget = maxOf(1, preloadAmount),
                        minItems = maxOf(4, preloadAmount * 2),
                    )

                // 3. Memory cache warming window: strictly bounded to immediate reading horizon
                val memoryEnd =
                    calculateWindowEnd(
                        startIndex = safeStart,
                        items = items,
                        pageBudget = minOf(maxOf(2, preloadAmount), MAX_MEMORY_PRELOAD_PAGES),
                        minItems =
                            minOf(
                                maxOf(MIN_MEMORY_PRELOAD_SLICES, preloadAmount * 2),
                                MAX_MEMORY_PRELOAD_SLICES,
                            ),
                    )

                coroutineScope {
                    for (i in windowStart..windowEnd) {
                        val item = items.getOrNull(i) ?: continue
                        preloadItem(this, item, preloadMemory = i in safeStart..memoryEnd)
                    }
                }
            }
    }

    internal fun calculateWindowStart(
        startIndex: Int,
        items: List<ReaderUiItem>,
        pageBudget: Int = 1,
        minItems: Int = 3,
    ): Int {
        if (items.isEmpty()) return 0
        val safeStart = startIndex.coerceIn(0, items.lastIndex)
        var distinctPages = 0
        var lastPage: ReaderPage? = null
        var lastIdx = safeStart
        for (i in safeStart downTo 0) {
            val item = items[i]
            val page =
                when (item) {
                    is ReaderUiItem.Page -> item.page
                    is ReaderUiItem.SplitPage -> item.page
                    is ReaderUiItem.Transition -> null
                }
            if (page != null && page != lastPage) {
                distinctPages++
                lastPage = page
            }
            lastIdx = i
            if (distinctPages > pageBudget && (safeStart - i) >= minItems) {
                break
            }
        }
        return lastIdx
    }

    internal fun calculateWindowEnd(
        startIndex: Int,
        items: List<ReaderUiItem>,
        pageBudget: Int,
        minItems: Int,
    ): Int {
        if (items.isEmpty()) return 0
        val safeStart = startIndex.coerceIn(0, items.lastIndex)
        var distinctPages = 0
        var lastPage: ReaderPage? = null
        var lastIdx = safeStart
        for (i in safeStart..items.lastIndex) {
            val item = items[i]
            val page =
                when (item) {
                    is ReaderUiItem.Page -> item.page
                    is ReaderUiItem.SplitPage -> item.page
                    is ReaderUiItem.Transition -> null
                }
            if (page != null && page != lastPage) {
                distinctPages++
                lastPage = page
            }
            lastIdx = i
            if (distinctPages > pageBudget && (i - safeStart) >= minItems) {
                break
            }
        }
        return lastIdx
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
        if (preloadMemory) {
            when (item) {
                is ReaderUiItem.Page -> {
                    if (!isSplitTallPagesEnabled() || checkedTallPages.contains(item.page)) {
                        if (preloadedMemory.add(key)) {
                            warmMemoryCache(key, item.page)
                        }
                    }
                }
                is ReaderUiItem.SplitPage -> {
                    if (preloadedMemory.add(key)) {
                        warmMemoryCache(key, item.split)
                    }
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
        val precomputed = page.precomputedSplits
        if (precomputed != null) {
            checkedTallPages.add(page)
            if (precomputed.isNotEmpty()) {
                if (preloadMemory) {
                    precomputed.forEach { split ->
                        val splitKey =
                            "webtoon_split_${split.page.chapter.chapter.id}_${split.page.index}_${split.topOffset}"
                        if (preloadedMemory.add(splitKey)) {
                            warmMemoryCache(splitKey, split)
                        }
                    }
                }
                onPageSplit?.invoke(page, precomputed)
            } else if (preloadMemory) {
                val key =
                    page.chapter.chapter.id?.let { cid -> "webtoon_page_${cid}_${page.index}" }
                if (key != null && preloadedMemory.add(key)) {
                    warmMemoryCache(key, page)
                }
            }
            return
        }
        if (!checkedTallPages.add(page)) return
        itemScope.launch(Dispatchers.IO) {
            try {
                page.statusFlow.first { it == Page.State.READY }
                val screenHeight = getScreenHeight()
                val splits = checkTallPage(page, screenHeight)
                page.precomputedSplits = splits ?: emptyList()
                if (splits != null && splits.isNotEmpty()) {
                    if (preloadMemory) {
                        splits.forEach { split ->
                            val splitKey =
                                "webtoon_split_${split.page.chapter.chapter.id}_${split.page.index}_${split.topOffset}"
                            if (preloadedMemory.add(splitKey)) {
                                warmMemoryCache(splitKey, split)
                            }
                        }
                    }
                    onPageSplit?.invoke(page, splits)
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
        var disposable: Disposable? = null
        val request =
            ImageRequest.Builder(context)
                .data(data)
                .size(CoilSize.ORIGINAL)
                .maxBitmapSize(maxTextureBitmapSize)
                .precision(Precision.EXACT)
                .crossfade(false)
                .listener(
                    onSuccess = { _, _ -> disposable?.let { activeDisposables.remove(key, it) } },
                    onError = { _, _ -> disposable?.let { activeDisposables.remove(key, it) } },
                    onCancel = { _ -> disposable?.let { activeDisposables.remove(key, it) } },
                )
                .build()

        activeDisposables[key]?.dispose()
        val handle = context.imageLoader.enqueue(request)
        disposable = handle
        if (!handle.isDisposed) {
            activeDisposables[key] = handle
        }
    }

    fun clear() {
        activeJob?.cancel()
        lastActiveIndex = -1
        lastItems = null
        lastPreloadAmount = -1
        activeDisposables.values.forEach { it.dispose() }
        activeDisposables.clear()
        preloadedDisk.clear()
        preloadedMemory.clear()
        checkedTallPages.clear()
    }
}
