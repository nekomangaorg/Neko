package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.domain.CheckTallPageUseCase
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import eu.kanade.tachiyomi.ui.reader.viewer.pager.ReaderPagerController
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Headless implementation of [ReaderPreloadController] managing a two-tier execution pipeline: Tier
 * 1: Network -> Disk Cache via [PageLoader.loadPage] with bounded concurrency. Tier 2: Disk -> RAM
 * via [MemoryCacheWarmManager] strictly bounded to reading horizon.
 */
class ReaderPreloadControllerImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    private val memoryCacheWarmManager: MemoryCacheWarmManager = MemoryCacheWarmManager(context),
    private val checkTallPage: CheckTallPageUseCase = CheckTallPageUseCase(),
    override var onPageSplit: ((ReaderPage, List<ReaderPageSplit>) -> Unit)? = null,
    private val isSplitTallPagesEnabled: () -> Boolean = { false },
    private val getScreenHeight: () -> Int = {
        val config = context.resources.configuration
        val density = context.resources.displayMetrics.density
        val windowHeightPx = (config.screenHeightDp * density).toInt()
        if (windowHeightPx > 0) windowHeightPx else context.resources.displayMetrics.heightPixels
    },
    private val onRequestPreloadChapter: ((ReaderChapter) -> Unit)? = null,
    private val maxConcurrentDownloads: Int = MAX_CONCURRENT_DOWNLOADS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ReaderPreloadController {

    companion object {
        const val MAX_CONCURRENT_DOWNLOADS = 3
        const val MAX_MEMORY_PRELOAD_PAGES = 4
        const val MAX_MEMORY_PRELOAD_SLICES = 12
        const val MIN_MEMORY_PRELOAD_SLICES = 6
        const val DEBOUNCE_DELAY_MS = 50L
        const val DOWNLOAD_TIMEOUT_MS = 60_000L
        const val DOMAIN_KEY_PREFIX = "domain"
    }

    private val _state = MutableStateFlow(ReaderPreloadState())
    override val state: StateFlow<ReaderPreloadState> = _state.asStateFlow()

    private val preloadedDiskKeys = Collections.synchronizedSet(mutableSetOf<String>())
    private val preloadedMemoryKeys = Collections.synchronizedSet(mutableSetOf<String>())
    private val checkedTallPages = Collections.synchronizedSet(mutableSetOf<ReaderPage>())
    private val activeDownloads = ConcurrentHashMap<String, Job>()
    private val retryCounts = ConcurrentHashMap<String, Int>()

    private val downloadSemaphore = Semaphore(maxConcurrentDownloads)

    private var activeOrchestratorJob: Job? = null
    private var lastActiveIndex: Int = -1
    private var lastItems: List<ReaderUiItem>? = null
    private var lastPreloadAmount: Int = -1
    private var lastIsRtl: Boolean = false
    private var lastIsWebtoon: Boolean = false

    fun isJobActive(): Boolean =
        activeOrchestratorJob?.isActive == true || activeDownloads.values.any { it.isActive }

    internal fun itemDomainKey(item: ReaderUiItem): String = item.key(DOMAIN_KEY_PREFIX)

    override fun onPositionChanged(
        currentIndex: Int,
        items: List<ReaderUiItem>,
        preloadAmount: Int,
        isRtl: Boolean,
        isWebtoon: Boolean,
    ) {
        if (items.isEmpty()) {
            _state.update {
                it.copy(
                    activeIndex = 0,
                    windowRange = IntRange.EMPTY,
                    memoryRange = IntRange.EMPTY,
                    isIdle = true,
                )
            }
            return
        }

        if (
            activeOrchestratorJob?.isActive == true &&
                lastActiveIndex == currentIndex &&
                lastItems === items &&
                lastPreloadAmount == preloadAmount &&
                lastIsRtl == isRtl &&
                lastIsWebtoon == isWebtoon
        ) {
            return
        }

        lastActiveIndex = currentIndex
        lastItems = items
        lastPreloadAmount = preloadAmount
        lastIsRtl = isRtl
        lastIsWebtoon = isWebtoon

        activeOrchestratorJob?.cancel()
        activeOrchestratorJob =
            scope.launch(ioDispatcher) {
                delay(DEBOUNCE_DELAY_MS) // Debounce rapid swiping/scroll flings

                val safeStart = currentIndex.coerceIn(0, items.lastIndex)

                // 1. Calculate disk and memory windows based on viewer layout mode
                val windowIndices =
                    if (isWebtoon) {
                        resolveWebtoonIndices(safeStart, items, preloadAmount)
                    } else {
                        resolvePagerIndices(safeStart, items, preloadAmount, isRtl)
                    }

                // 2. Update state with current active index and ranges
                _state.update { current ->
                    current.copy(
                        activeIndex = currentIndex,
                        windowRange = windowIndices.diskRange,
                        memoryRange = windowIndices.memoryRange,
                        isIdle = false,
                    )
                }

                // 3. Priority Re-ordering & Demotion of superseded requests
                val activeDiskKeys =
                    windowIndices.orderedIndices
                        .mapNotNull { items.getOrNull(it)?.let(::itemDomainKey) }
                        .toSet()
                val activeMemoryKeys =
                    windowIndices.memoryIndices
                        .mapNotNull { items.getOrNull(it)?.let(::itemDomainKey) }
                        .toSet()

                // Cancel downloads for items that fall outside the new window
                val downloadIterator = activeDownloads.entries.iterator()
                val keysToPrune = mutableSetOf<String>()
                while (downloadIterator.hasNext()) {
                    val entry = downloadIterator.next()
                    if (entry.key !in activeDiskKeys) {
                        entry.value.cancel()
                        downloadIterator.remove()
                        keysToPrune.add(entry.key)
                    }
                }
                if (keysToPrune.isNotEmpty()) {
                    _state.update { current ->
                        val updated = current.pageStatuses.toMutableMap()
                        keysToPrune.forEach { updated.remove(it) }
                        current.copy(pageStatuses = updated)
                    }
                }

                // Prune memory tracking and cancel memory decodes outside sliding memory window
                preloadedMemoryKeys.retainAll(activeMemoryKeys)
                memoryCacheWarmManager.cancelAllExcept(activeMemoryKeys)

                // 4. Bounded Priority Execution: Network -> Disk -> Memory
                for (index in windowIndices.orderedIndices) {
                    val item = items.getOrNull(index) ?: continue
                    val key = itemDomainKey(item)
                    val shouldWarmMemory = index in windowIndices.memoryIndices

                    // If already on disk, check if it needs memory warm
                    if (preloadedDiskKeys.contains(key)) {
                        if (shouldWarmMemory && !preloadedMemoryKeys.contains(key)) {
                            warmItemMemory(item, key, isWebtoon)
                        }
                        continue
                    }

                    // If already actively downloading, let it continue
                    if (activeDownloads[key]?.isActive == true) {
                        continue
                    }

                    updatePageStatus(key, PreloadPageStatus.DiskQueued)

                    val job =
                        scope.launch(ioDispatcher) {
                            try {
                                downloadSemaphore.withPermit {
                                    updatePageStatus(key, PreloadPageStatus.DiskDownloading)
                                    val success = loadItemToDisk(item, key)
                                    if (success) {
                                        preloadedDiskKeys.add(key)
                                        updatePageStatus(key, PreloadPageStatus.DiskReady)

                                        // Tall page check if in Webtoon mode
                                        if (
                                            isWebtoon &&
                                                isSplitTallPagesEnabled() &&
                                                item is ReaderUiItem.Page
                                        ) {
                                            checkAndSplitTallPage(
                                                this,
                                                item.page,
                                                preloadMemory = shouldWarmMemory,
                                            )
                                        } else if (shouldWarmMemory) {
                                            warmItemMemory(item, key, isWebtoon)
                                        }
                                    } else {
                                        preloadedDiskKeys.remove(key)
                                    }
                                }
                            } finally {
                                activeDownloads.remove(key)
                                _state.update { current ->
                                    if (
                                        activeDownloads.isEmpty() &&
                                            memoryCacheWarmManager.activeCount() == 0
                                    ) {
                                        current.copy(isIdle = true)
                                    } else {
                                        current
                                    }
                                }
                            }
                        }
                    activeDownloads[key] = job
                }

                _state.update { current ->
                    if (activeDownloads.isEmpty() && memoryCacheWarmManager.activeCount() == 0) {
                        current.copy(isIdle = true)
                    } else {
                        current
                    }
                }
            }
    }

    private suspend fun loadItemToDisk(item: ReaderUiItem, key: String): Boolean {
        return try {
            when (item) {
                is ReaderUiItem.Page -> {
                    val page = item.page
                    page.chapter.pageLoader?.let { loader ->
                        loader.loadPage(page)
                        if (page.status != Page.State.READY) {
                            val finalStatus =
                                withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                                    page.statusFlow.first {
                                        it == Page.State.READY || it == Page.State.ERROR
                                    }
                                }
                            if (finalStatus == null) {
                                val count = retryCounts.getOrDefault(key, 0)
                                updatePageStatus(
                                    key,
                                    PreloadPageStatus.Error(
                                        TimeoutException("Timeout downloading page ${page.index}"),
                                        count,
                                    ),
                                )
                                return false
                            }
                        }
                        if (page.status == Page.State.ERROR) {
                            val count = retryCounts.getOrDefault(key, 0)
                            updatePageStatus(
                                key,
                                PreloadPageStatus.Error(
                                    Exception("Failed to load page ${page.index}"),
                                    count,
                                ),
                            )
                            return false
                        }
                    }
                    item.extraPage?.let { extra ->
                        extra.chapter.pageLoader?.let { extraLoader ->
                            extraLoader.loadPage(extra)
                            if (extra.status != Page.State.READY) {
                                val extraFinalStatus =
                                    withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                                        extra.statusFlow.first {
                                            it == Page.State.READY || it == Page.State.ERROR
                                        }
                                    }
                                if (extraFinalStatus == null) {
                                    val count = retryCounts.getOrDefault(key, 0)
                                    updatePageStatus(
                                        key,
                                        PreloadPageStatus.Error(
                                            TimeoutException(
                                                "Timeout downloading extra page ${extra.index}"
                                            ),
                                            count,
                                        ),
                                    )
                                    return false
                                }
                            }
                            if (extra.status == Page.State.ERROR) {
                                val count = retryCounts.getOrDefault(key, 0)
                                updatePageStatus(
                                    key,
                                    PreloadPageStatus.Error(
                                        Exception("Failed to load extra page ${extra.index}"),
                                        count,
                                    ),
                                )
                                return false
                            }
                        }
                    }
                    true
                }
                is ReaderUiItem.SplitPage -> {
                    val page = item.page
                    page.chapter.pageLoader?.let { loader ->
                        loader.loadPage(page)
                        if (page.status != Page.State.READY) {
                            val finalStatus =
                                withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                                    page.statusFlow.first {
                                        it == Page.State.READY || it == Page.State.ERROR
                                    }
                                }
                            if (finalStatus == null) {
                                val count = retryCounts.getOrDefault(key, 0)
                                updatePageStatus(
                                    key,
                                    PreloadPageStatus.Error(
                                        TimeoutException(
                                            "Timeout downloading parent page for split"
                                        ),
                                        count,
                                    ),
                                )
                                return false
                            }
                        }
                        if (page.status == Page.State.ERROR) {
                            val count = retryCounts.getOrDefault(key, 0)
                            updatePageStatus(
                                key,
                                PreloadPageStatus.Error(
                                    Exception(
                                        "Failed to load parent page for split ${item.split.topOffset}"
                                    ),
                                    count,
                                ),
                            )
                            return false
                        }
                    }
                    true
                }
                is ReaderUiItem.Transition -> true
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                val count = retryCounts.getOrDefault(key, 0)
                updatePageStatus(key, PreloadPageStatus.Error(e, count))
            }
            false
        }
    }

    private fun warmItemMemory(item: ReaderUiItem, key: String, isWebtoon: Boolean) {
        if (!preloadedMemoryKeys.add(key)) return

        updatePageStatus(key, PreloadPageStatus.MemoryDecoding)

        when (item) {
            is ReaderUiItem.Page -> {
                memoryCacheWarmManager.warmMemoryCache(
                    key = key,
                    data = item.page,
                    crossfade = !isWebtoon,
                    onSuccess = { updatePageStatus(key, PreloadPageStatus.MemoryReady) },
                    onError = { throwable ->
                        preloadedMemoryKeys.remove(key)
                        val count = retryCounts.getOrDefault(key, 0)
                        updatePageStatus(key, PreloadPageStatus.Error(throwable, count))
                    },
                )
                item.extraPage?.let { extra ->
                    val extraKey = "${key}_extra"
                    memoryCacheWarmManager.warmMemoryCache(
                        key = extraKey,
                        data = extra,
                        crossfade = !isWebtoon,
                    )
                }
            }
            is ReaderUiItem.SplitPage -> {
                memoryCacheWarmManager.warmMemoryCache(
                    key = key,
                    data = item.split,
                    crossfade = false,
                    onSuccess = { updatePageStatus(key, PreloadPageStatus.MemoryReady) },
                    onError = { throwable ->
                        preloadedMemoryKeys.remove(key)
                        val count = retryCounts.getOrDefault(key, 0)
                        updatePageStatus(key, PreloadPageStatus.Error(throwable, count))
                    },
                )
            }
            is ReaderUiItem.Transition -> {
                updatePageStatus(key, PreloadPageStatus.Idle)
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
                            "${DOMAIN_KEY_PREFIX}_split_${split.page.chapter.chapter.id ?: 0}_${split.page.index}_${split.topOffset}"
                        if (preloadedMemoryKeys.add(splitKey)) {
                            updatePageStatus(splitKey, PreloadPageStatus.MemoryDecoding)
                            memoryCacheWarmManager.warmMemoryCache(
                                key = splitKey,
                                data = split,
                                crossfade = false,
                                onSuccess = {
                                    updatePageStatus(splitKey, PreloadPageStatus.MemoryReady)
                                },
                                onError = { throwable ->
                                    preloadedMemoryKeys.remove(splitKey)
                                    val count = retryCounts.getOrDefault(splitKey, 0)
                                    updatePageStatus(
                                        splitKey,
                                        PreloadPageStatus.Error(throwable, count),
                                    )
                                },
                            )
                        }
                    }
                }
                onPageSplit?.invoke(page, precomputed)
            } else if (preloadMemory) {
                val key = "${DOMAIN_KEY_PREFIX}_page_${page.chapter.chapter.id ?: 0}_${page.index}"
                if (preloadedMemoryKeys.add(key)) {
                    updatePageStatus(key, PreloadPageStatus.MemoryDecoding)
                    memoryCacheWarmManager.warmMemoryCache(
                        key = key,
                        data = page,
                        crossfade = false,
                        onSuccess = { updatePageStatus(key, PreloadPageStatus.MemoryReady) },
                        onError = { throwable ->
                            preloadedMemoryKeys.remove(key)
                            val count = retryCounts.getOrDefault(key, 0)
                            updatePageStatus(key, PreloadPageStatus.Error(throwable, count))
                        },
                    )
                }
            }
            return
        }

        if (!checkedTallPages.add(page)) return

        itemScope.launch(ioDispatcher) {
            try {
                page.statusFlow.first { it == Page.State.READY }
                val screenHeight = getScreenHeight()
                val splits = checkTallPage(page, screenHeight)
                page.precomputedSplits = splits ?: emptyList()
                if (splits != null && splits.isNotEmpty()) {
                    if (preloadMemory) {
                        splits.forEach { split ->
                            val splitKey =
                                "${DOMAIN_KEY_PREFIX}_split_${split.page.chapter.chapter.id ?: 0}_${split.page.index}_${split.topOffset}"
                            if (preloadedMemoryKeys.add(splitKey)) {
                                updatePageStatus(splitKey, PreloadPageStatus.MemoryDecoding)
                                memoryCacheWarmManager.warmMemoryCache(
                                    key = splitKey,
                                    data = split,
                                    crossfade = false,
                                    onSuccess = {
                                        updatePageStatus(splitKey, PreloadPageStatus.MemoryReady)
                                    },
                                    onError = { throwable ->
                                        preloadedMemoryKeys.remove(splitKey)
                                        val count = retryCounts.getOrDefault(splitKey, 0)
                                        updatePageStatus(
                                            splitKey,
                                            PreloadPageStatus.Error(throwable, count),
                                        )
                                    },
                                )
                            }
                        }
                    }
                    onPageSplit?.invoke(page, splits)
                } else if (preloadMemory) {
                    val key =
                        "${DOMAIN_KEY_PREFIX}_page_${page.chapter.chapter.id ?: 0}_${page.index}"
                    if (preloadedMemoryKeys.add(key)) {
                        updatePageStatus(key, PreloadPageStatus.MemoryDecoding)
                        memoryCacheWarmManager.warmMemoryCache(
                            key = key,
                            data = page,
                            crossfade = false,
                            onSuccess = { updatePageStatus(key, PreloadPageStatus.MemoryReady) },
                            onError = { throwable ->
                                preloadedMemoryKeys.remove(key)
                                val count = retryCounts.getOrDefault(key, 0)
                                updatePageStatus(key, PreloadPageStatus.Error(throwable, count))
                            },
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    checkedTallPages.remove(page)
                }
            }
        }
    }

    private fun updatePageStatus(key: String, status: PreloadPageStatus) {
        _state.update { current ->
            if (current.pageStatuses[key] == status) return@update current
            val updatedStatuses = current.pageStatuses.toMutableMap()
            if (status is PreloadPageStatus.Idle) {
                updatedStatuses.remove(key)
            } else {
                updatedStatuses[key] = status
            }
            current.copy(pageStatuses = updatedStatuses)
        }
    }

    override fun requestPreloadChapter(chapter: ReaderChapter) {
        onRequestPreloadChapter?.invoke(chapter)
    }

    override fun retryPage(item: ReaderUiItem) {
        val key = itemDomainKey(item)
        val count = (retryCounts[key] ?: 0) + 1
        retryCounts[key] = count
        preloadedDiskKeys.remove(key)
        preloadedMemoryKeys.remove(key)
        when (item) {
            is ReaderUiItem.Page -> {
                item.page.chapter.pageLoader?.retryPage(item.page)
                item.extraPage?.let { extra -> extra.chapter.pageLoader?.retryPage(extra) }
            }
            is ReaderUiItem.SplitPage -> {
                item.page.chapter.pageLoader?.retryPage(item.page)
            }
            is ReaderUiItem.Transition -> Unit
        }
        val isWebtoon = item is ReaderUiItem.SplitPage || lastIsWebtoon
        scope.launch(ioDispatcher) {
            updatePageStatus(key, PreloadPageStatus.DiskQueued)
            downloadSemaphore.withPermit {
                updatePageStatus(key, PreloadPageStatus.DiskDownloading)
                val success = loadItemToDisk(item, key)
                if (success) {
                    preloadedDiskKeys.add(key)
                    updatePageStatus(key, PreloadPageStatus.DiskReady)
                    warmItemMemory(item, key, isWebtoon)
                }
            }
        }
    }

    override fun release() {
        activeOrchestratorJob?.cancel()
        activeOrchestratorJob = null
        lastActiveIndex = -1
        lastItems = null
        lastPreloadAmount = -1
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
        memoryCacheWarmManager.release()
        preloadedDiskKeys.clear()
        preloadedMemoryKeys.clear()
        checkedTallPages.clear()
        retryCounts.clear()
        _state.value = ReaderPreloadState(isIdle = true)
    }

    internal data class WindowIndices(
        val orderedIndices: List<Int>,
        val memoryIndices: Set<Int>,
        val diskRange: IntRange,
        val memoryRange: IntRange,
    )

    internal fun resolveWebtoonIndices(
        safeStart: Int,
        items: List<ReaderUiItem>,
        preloadAmount: Int,
    ): WindowIndices {
        val windowStart =
            calculateWindowStart(
                startIndex = safeStart,
                items = items,
                pageBudget = 1,
                minItems = 3,
            )
        val windowEnd =
            calculateWindowEnd(
                startIndex = safeStart,
                items = items,
                pageBudget = maxOf(1, preloadAmount),
                minItems = maxOf(4, preloadAmount * 2),
            )
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

        val forward = (safeStart..windowEnd).toList()
        val backward =
            if (safeStart > windowStart) (safeStart - 1 downTo windowStart).toList()
            else emptyList()
        val ordered = forward + backward
        val memory = (safeStart..memoryEnd).toSet()

        return WindowIndices(
            orderedIndices = ordered,
            memoryIndices = memory,
            diskRange = windowStart..windowEnd,
            memoryRange = safeStart..memoryEnd,
        )
    }

    internal fun resolvePagerIndices(
        safeStart: Int,
        items: List<ReaderUiItem>,
        preloadAmount: Int,
        isRtl: Boolean,
    ): WindowIndices {
        val ordered =
            ReaderPagerController.getPreloadIndices(
                currentIndex = safeStart,
                preloadAmount = preloadAmount,
                totalItems = items.size,
                isRtl = isRtl,
            )
        val memory =
            ReaderPagerController.getPreloadIndices(
                    currentIndex = safeStart,
                    preloadAmount = 2,
                    totalItems = items.size,
                    isRtl = isRtl,
                )
                .toSet()

        val diskRange = if (ordered.isEmpty()) IntRange.EMPTY else ordered.min()..ordered.max()
        val memoryRange = if (memory.isEmpty()) IntRange.EMPTY else memory.min()..memory.max()

        return WindowIndices(
            orderedIndices = ordered,
            memoryIndices = memory,
            diskRange = diskRange,
            memoryRange = memoryRange,
        )
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
}
