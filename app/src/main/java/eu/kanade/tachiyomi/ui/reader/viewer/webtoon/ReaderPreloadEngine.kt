package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.os.Build
import android.view.WindowManager
import eu.kanade.tachiyomi.ui.reader.domain.CheckTallPageUseCase
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPreloadControllerImpl
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.model.ReaderUiItem
import kotlinx.coroutines.CoroutineScope

/**
 * Headless preload coordinator managing disk prefetching, bitmap memory cache warming, and
 * background tall page splitting.
 *
 * @deprecated Use [eu.kanade.tachiyomi.ui.reader.loader.ReaderPreloadController] instead.
 */
@Deprecated("Use ReaderPreloadController instead")
class ReaderPreloadEngine(
    context: Context,
    scope: CoroutineScope,
    checkTallPage: CheckTallPageUseCase = CheckTallPageUseCase(),
    var onPageSplit: ((ReaderPage, List<ReaderPageSplit>) -> Unit)? = null,
    isSplitTallPagesEnabled: () -> Boolean = { false },
    getScreenHeight: () -> Int = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            windowManager?.currentWindowMetrics?.bounds?.height()
                ?: context.resources.displayMetrics.heightPixels
        } else {
            @Suppress("DEPRECATION") context.resources.displayMetrics.heightPixels
        }
    },
    controller: ReaderPreloadControllerImpl? = null,
) {
    companion object {
        /** Hard cap on memory preloaded pages to prevent Coil LRU cache thrashing. */
        const val MAX_MEMORY_PRELOAD_PAGES = ReaderPreloadControllerImpl.MAX_MEMORY_PRELOAD_PAGES

        /** Hard cap on memory preloaded slices (e.g. ~100MB of bitmap memory). */
        const val MAX_MEMORY_PRELOAD_SLICES = ReaderPreloadControllerImpl.MAX_MEMORY_PRELOAD_SLICES

        /** Minimum slice buffer ahead of viewport. */
        const val MIN_MEMORY_PRELOAD_SLICES = ReaderPreloadControllerImpl.MIN_MEMORY_PRELOAD_SLICES
    }

    private val internalController: ReaderPreloadControllerImpl =
        controller
            ?: ReaderPreloadControllerImpl(
                context = context,
                scope = scope,
                checkTallPage = checkTallPage,
                isSplitTallPagesEnabled = isSplitTallPagesEnabled,
                getScreenHeight = getScreenHeight,
            )

    init {
        internalController.onPageSplit = { page, splits -> onPageSplit?.invoke(page, splits) }
    }

    fun isJobActive(): Boolean = internalController.isJobActive()

    fun updateActiveIndex(activeIndex: Int, items: List<ReaderUiItem>, preloadAmount: Int) {
        internalController.onPageSplit = onPageSplit
        internalController.onPositionChanged(
            currentIndex = activeIndex,
            items = items,
            preloadAmount = preloadAmount,
            isRtl = false,
            isWebtoon = true,
        )
    }

    fun updateActivePagerIndex(
        activeIndex: Int,
        items: List<ReaderUiItem>,
        preloadAmount: Int,
        isRtl: Boolean,
    ) {
        internalController.onPageSplit = onPageSplit
        internalController.onPositionChanged(
            currentIndex = activeIndex,
            items = items,
            preloadAmount = preloadAmount,
            isRtl = isRtl,
            isWebtoon = false,
        )
    }

    fun calculateWindowStart(
        startIndex: Int,
        items: List<ReaderUiItem>,
        pageBudget: Int = 1,
        minItems: Int = 3,
    ): Int = internalController.calculateWindowStart(startIndex, items, pageBudget, minItems)

    fun calculateWindowEnd(
        startIndex: Int,
        items: List<ReaderUiItem>,
        pageBudget: Int,
        minItems: Int,
    ): Int = internalController.calculateWindowEnd(startIndex, items, pageBudget, minItems)

    fun clear() {
        internalController.release()
    }
}
