package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.nekomanga.presentation.screens.reader.viewer.CachedSmartBackground

open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    mangaDexChapterId: String = "",
    var stream: (() -> InputStream)? = null,
) : Page(index, url, imageUrl, mangaDexChapterId, null) {

    /** Rendered height after image is decoded and laid out (pixels at fit-width). */
    var renderedHeight: Int = 0

    /** Cached aspect ratio (width / height) to prevent layout shifts. */
    var aspectRatio: Float = 0f

    /** Pre-calculated splits when tall page splitting is performed upstream. */
    var precomputedSplits: List<ReaderPageSplit>? = null

    /** Smart reader background picked for this page, so a page seen again draws it at once. */
    @Volatile var smartBackground: CachedSmartBackground? = null

    /** Value to check if this page is used to as if it was too wide */
    var shiftedPage: Boolean = false

    /**
     * Value to check if a page is can be doubled up, but can't because the next page is too wide
     */
    var isolatedPage: Boolean = false
    var firstHalf: Boolean? = null
    var longPage: Boolean? = null
    var endPageConfidence: Int? = null
    var startPageConfidence: Int? = null
    open lateinit var chapter: ReaderChapter

    /** Value to check if a page is too wide to be doubled up */
    var fullPage: Boolean? = null
        set(value) {
            field = value
            longPage = value
            if (value == true) shiftedPage = false
        }

    val alonePage: Boolean
        get() = fullPage == true || isolatedPage

    val isEndPage
        get() = endPageConfidence?.let { it > 0 && it > (startPageConfidence ?: 0) }

    val isStartPage
        get() = startPageConfidence?.let { it > 0 && it > (endPageConfidence ?: 0) }

    fun isFromSamePage(page: ReaderPage): Boolean =
        index == page.index && chapter.chapter.id == page.chapter.chapter.id

    private val _retryGenerationFlow = MutableStateFlow(0)

    /**
     * Changes each time Retry is tapped on this page. All pages share one counter, so a page object
     * created again starts below every earlier retry and still sees the failures from before.
     */
    val retryGenerationFlow = _retryGenerationFlow.asStateFlow()
    val retryGeneration: Int
        get() = _retryGenerationFlow.value

    fun retry() {
        _retryGenerationFlow.value = retryGenerations.incrementAndGet()
    }

    private companion object {
        val retryGenerations = AtomicInteger()
    }
}

data class ReaderPageSplit(
    val page: ReaderPage,
    val topOffset: Int,
    val splitHeight: Int,
) {
    var displayedHeight: Int = 0
    var aspectRatio: Float = 0f
}
