package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressBar
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig.ZoomType
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.ThemeUtil
import eu.kanade.tachiyomi.util.system.bottomCutoutInset
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.isInNightMode
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.topCutoutInset
import eu.kanade.tachiyomi.util.view.backgroundColor
import eu.kanade.tachiyomi.util.view.isVisibleOnScreen
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import java.io.InputStream
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nekomanga.R
import org.nekomanga.domain.reader.ReaderPreferences
import org.nekomanga.logging.TimberKt
import uy.kohesive.injekt.injectLazy

/** View of the ViewPager that contains a page of a chapter. */
@SuppressLint("ViewConstructor")
class PagerPageHolder(
    val viewer: PagerViewer,
    val page: ReaderPage,
    private var extraPage: ReaderPage? = null,
) : ReaderPageImageView(viewer.activity), ViewPagerAdapter.PositionableView {

    /** Item that identifies this view. Needed by the adapter to not recreate views. */
    override val item
        get() = page to extraPage

    /** Loading progress bar to indicate the current progress. */
    private val progressBar = createProgressBar()

    /** Retry button used to allow retrying. */
    private var retryButton: PagerButton? = null

    /** Error layout to show when the image fails to decode. */
    private var decodeErrorLayout: ViewGroup? = null

    /** Job for loading the page. */
    private var loadJob: Job? = null

    /** Job for status changes of the page. */
    private var statusJob: Job? = null

    /** Job for progress changes of the page. */
    private var progressJob: Job? = null

    /** Job for loading the page. */
    private var extraLoadJob: Job? = null

    /** Job for status changes of the page. */
    private var extraStatusJob: Job? = null

    /** Job for progress changes of the page. */
    private var extraProgressJob: Job? = null

    /** Job for loading the image header and stream. */
    private var imageLoadJob: Job? = null

    private var status = Page.State.READY
    private var extraStatus = Page.State.READY
    private var progress: Int = 0
    private var extraProgress: Int = 0

    private var scope = MainScope()

    init {
        addView(progressBar)
        launchLoadJob()
        setBackgroundColor(
            when (val theme = viewer.config.readerTheme) {
                3 -> Color.TRANSPARENT
                else -> ThemeUtil.readerBackgroundColor(theme)
            }
        )
        progressBar.foregroundTintList =
            ColorStateList.valueOf(
                context.getResourceColor(
                    if (isInvertedFromTheme()) {
                        R.attr.colorPrimaryInverse
                    } else {
                        R.attr.colorPrimary
                    }
                )
            )
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        (pageView as? SubsamplingScaleImageView)?.apply {
            if (
                this@PagerPageHolder.extraPage == null &&
                    this@PagerPageHolder.page.longPage == null &&
                    sHeight < sWidth
            ) {
                this@PagerPageHolder.page.longPage = true
            }
        }
        onImageDecoded()
    }

    override fun onNeedsLandscapeZoom() {
        (pageView as? SubsamplingScaleImageView)?.apply {
            if (viewer.heldForwardZoom?.first == page.index) {
                landscapeZoom(viewer.heldForwardZoom?.second)
                viewer.heldForwardZoom = null
            } else if (isVisibleOnScreen()) {
                landscapeZoom(true)
            }
        }
    }

    override fun onScaleChanged(newScale: Float) {
        super.onScaleChanged(newScale)
        viewer.hideMenuIfVisible(item)
    }

    override fun onImageLoadError() {
        super.onImageLoadError()
        onImageDecodeError()
    }

    /** Called when this view is detached from the window. Unsubscribes any active subscription. */
    @SuppressLint("ClickableViewAccessibility")
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelProgressJob(1)
        cancelLoadJob(1)
        cancelProgressJob(2)
        cancelLoadJob(2)
        cancelImageLoadJob()
        (pageView as? SubsamplingScaleImageView)?.setOnImageEventListener(null)
    }

    /**
     * Starts loading the page and processing changes to the page's status.
     *
     * @see processStatus
     */
    private fun launchLoadJob() {
        loadJob?.cancel()
        statusJob?.cancel()

        val loader = page.chapter.pageLoader ?: return
        loadJob = scope.launch { loader.loadPage(page) }
        statusJob = scope.launch { page.statusFlow.collectLatest { processStatus(it) } }
        val extraPage = extraPage ?: return
        extraLoadJob = scope.launch { loader.loadPage(extraPage) }
        extraStatusJob = scope.launch { extraPage.statusFlow.collectLatest { processStatus2(it) } }
    }

    private fun launchProgressJob() {
        progressJob?.cancel()
        progressJob =
            scope.launch {
                page.progressFlow.collectLatest { value ->
                    progress = value
                    if (extraPage == null) {
                        progressBar.setProgress(progress)
                    } else {
                        progressBar.setProgress(
                            ((progress + extraProgress) / 2 * 0.95f).roundToInt()
                        )
                    }
                }
            }
    }

    private fun launchProgressJob2() {
        val extraPage = extraPage ?: return
        extraProgressJob?.cancel()
        extraProgressJob =
            scope.launch {
                extraPage.progressFlow.collectLatest { value ->
                    extraProgress = value
                    progressBar.setProgress(((progress + extraProgress) / 2 * 0.95f).roundToInt())
                }
            }
    }

    fun onPageSelected(forward: Boolean?) {
        (pageView as? SubsamplingScaleImageView)?.apply {
            if (isReady) {
                landscapeZoom(forward)
            } else {
                forward ?: return@apply
                setOnImageEventListener(
                    object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                        override fun onReady() {
                            setupZoom(imageConfig)
                            landscapeZoom(forward)
                            this@PagerPageHolder.onImageLoaded()
                        }

                        override fun onImageLoadError(e: Exception) {
                            onImageDecodeError()
                        }
                    }
                )
            }
        }
    }

    /** Check if the image can be panned to the left */
    fun canPanLeft(): Boolean = canPan { it.left }

    /** Check if the image can be panned to the right */
    fun canPanRight(): Boolean = canPan { it.right }

    /**
     * Check whether the image can be panned.
     *
     * @param fn a function that returns the direction to check for
     */
    private fun canPan(fn: (RectF) -> Float): Boolean {
        (pageView as? SubsamplingScaleImageView)?.let { view ->
            RectF().let {
                view.getPanRemaining(it)
                return fn(it) > 0.01f
            }
        }
        return false
    }

    /** Pans the image to the left by a screen's width worth. */
    fun panLeft() {
        pan { center, view -> center.also { it.x -= view.width / view.scale } }
    }

    /** Pans the image to the right by a screen's width worth. */
    fun panRight() {
        pan { center, view -> center.also { it.x += view.width / view.scale } }
    }

    /**
     * Pans the image.
     *
     * @param fn a function that computes the new center of the image
     */
    private fun pan(fn: (PointF, SubsamplingScaleImageView) -> PointF) {
        (pageView as? SubsamplingScaleImageView)?.let { view ->
            val target = fn(view.center ?: return, view)
            view
                .animateCenter(target)!!
                .withEasing(SubsamplingScaleImageView.EASE_OUT_QUAD)
                .withDuration(250)
                .withInterruptible(true)
                .start()
        }
    }

    private fun SubsamplingScaleImageView.landscapeZoom(forward: Boolean?) {
        forward ?: return
        if (
            viewer.config.landscapeZoom &&
                viewer.config.imageScaleType ==
                    SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE &&
                sWidth > sHeight &&
                scale == minScale
        ) {
            handler.postDelayed(
                {
                    val point =
                        when (viewer.config.imageZoomType) {
                            ZoomType.Left ->
                                if (forward) PointF(0F, 0F) else PointF(sWidth.toFloat(), 0F)
                            ZoomType.Right ->
                                if (forward) PointF(sWidth.toFloat(), 0F) else PointF(0F, 0F)
                            ZoomType.Center -> center.also { it?.y = 0F }
                        }

                    val rootInsets = viewer.activity.window.decorView.rootWindowInsets
                    val topInsets =
                        if (viewer.activity.isSplitScreen) {
                            0f
                        } else {
                            rootInsets?.topCutoutInset()?.toFloat() ?: 0f
                        }
                    val bottomInsets =
                        if (viewer.activity.isSplitScreen) {
                            0f
                        } else {
                            rootInsets?.bottomCutoutInset()?.toFloat() ?: 0f
                        }
                    val targetScale =
                        (height.toFloat() - topInsets - bottomInsets) / sHeight.toFloat()
                    animateScaleAndCenter(min(targetScale, minScale * 2), point)!!
                        .withDuration(500)
                        .withEasing(SubsamplingScaleImageView.EASE_IN_OUT_QUAD)
                        .withInterruptible(true)
                        .start()
                },
                500,
            )
        }
    }

    /**
     * Called when the status of the page changes.
     *
     * @param status the new status of the page.
     */
    private fun processStatus(status: Page.State) {
        when (status) {
            Page.State.QUEUE -> setQueued()
            Page.State.LOAD_PAGE -> setLoading()
            Page.State.DOWNLOAD_IMAGE -> {
                launchProgressJob()
                setDownloading()
            }
            Page.State.READY -> {
                if (extraStatus == Page.State.READY || extraPage == null) {
                    setImage()
                }
                cancelProgressJob(1)
            }
            Page.State.ERROR -> {
                setError()
                cancelProgressJob(1)
            }
        }
    }

    /**
     * Called when the status of the page changes.
     *
     * @param status the new status of the page.
     */
    private fun processStatus2(status: Page.State) {
        when (status) {
            Page.State.QUEUE -> setQueued()
            Page.State.LOAD_PAGE -> setLoading()
            Page.State.DOWNLOAD_IMAGE -> {
                launchProgressJob2()
                setDownloading()
            }
            Page.State.READY -> {
                if (this.status == Page.State.READY) {
                    setImage()
                }
                cancelProgressJob(2)
            }
            Page.State.ERROR -> {
                setError()
                cancelProgressJob(2)
            }
        }
    }

    /** Cancels loading the page and processing changes to the page's status. */
    private fun cancelLoadJob(page: Int) {
        if (page == 1) {
            loadJob?.cancel()
            loadJob = null
            statusJob?.cancel()
            statusJob = null
        } else {
            extraLoadJob?.cancel()
            extraLoadJob = null
            extraStatusJob?.cancel()
            extraStatusJob = null
        }
    }

    private fun cancelProgressJob(page: Int) {
        (if (page == 1) progressJob else extraProgressJob)?.cancel()
        if (page == 1) {
            progressJob = null
        } else {
            extraProgressJob = null
        }
    }

    /** Cancels the image load job. */
    private fun cancelImageLoadJob() {
        imageLoadJob?.cancel()
        imageLoadJob = null
    }

    /** Called when the page is queued. */
    private fun setQueued() {
        progressBar.isVisible = true
        retryButton?.isVisible = false
        decodeErrorLayout?.isVisible = false
    }

    /** Called when the page is loading. */
    private fun setLoading() {
        progressBar.isVisible = true
        retryButton?.isVisible = false
        decodeErrorLayout?.isVisible = false
    }

    /** Called when the page is downloading. */
    private fun setDownloading() {
        progressBar.isVisible = true
        retryButton?.isVisible = false
        decodeErrorLayout?.isVisible = false
    }

    /** Called when the page is ready. */
    private fun setImage() {
        progressBar.isVisible = true
        if (extraPage == null) {
            progressBar.completeAndFadeOut()
        } else {
            progressBar.setProgress(95)
        }
        retryButton?.isVisible = false
        decodeErrorLayout?.isVisible = false

        cancelImageLoadJob()
        val streamFn = page.stream ?: return
        val streamFn2 = extraPage?.stream

        imageLoadJob =
            scope.launch(Dispatchers.IO) {
                var openStream: InputStream? = null
                try {
                    val stream = streamFn().buffered(16)

                    val stream2 = streamFn2?.invoke()?.buffered(16)
                    openStream =
                        when (
                            viewer.config.doublePageRotate &&
                                stream2 == null &&
                                ImageUtil.isWideImage(stream)
                        ) {
                            true -> {
                                val rotation =
                                    if (viewer.config.doublePageRotateReverse) -90f else 90f
                                ImageUtil.rotateImage(stream, rotation)
                            }
                            false -> this@PagerPageHolder.mergeOrSplitPages(stream, stream2)
                        }

                    val isAnimated =
                        ImageUtil.isAnimatedAndSupported(stream) ||
                            if (stream2 != null) ImageUtil.isAnimatedAndSupported(stream2)
                            else false

                    withContext(Dispatchers.Main) {
                        if (!isAnimated) {
                            if (viewer.config.readerTheme >= 2) {
                                if (
                                    page.bg != null &&
                                        page.bgType ==
                                            getBGType(viewer.config.readerTheme, context) +
                                                item.hashCode()
                                ) {
                                    setImage(openStream!!, false, imageConfig)
                                    pageView?.background = page.bg
                                }
                                // if the user switches to automatic when pages are already cached,
                                // the bg needs to be loaded
                                else {
                                    val bytesArray = openStream!!.readBytes()
                                    val bytesStream = bytesArray.inputStream()
                                    setImage(bytesStream, false, imageConfig)
                                    bytesStream.close()

                                    scope.launchUI {
                                        try {
                                            pageView?.background = setBG(bytesArray)
                                        } catch (e: Exception) {
                                            TimberKt.e(e) { "Error setting BG" }
                                            pageView?.background = ColorDrawable(Color.WHITE)
                                        } finally {
                                            page.bg = pageView?.background
                                            page.bgType =
                                                getBGType(viewer.config.readerTheme, context) +
                                                    item.hashCode()
                                        }
                                    }
                                }
                            } else {
                                setImage(openStream!!, false, imageConfig)
                            }
                        } else {
                            setImage(openStream!!, true, imageConfig)
                            if (viewer.config.readerTheme >= 2 && page.bg != null) {
                                pageView?.background = page.bg
                            }
                        }
                    }

                    // Keep the stream alive
                    awaitCancellation()
                } catch (e: Exception) {
                    // Ignore errors as per original Rx implementation
                    TimberKt.e(e) { "Error loading image in PagerPageHolder" }
                    try {
                        openStream?.close()
                    } catch (e: Exception) {
                        TimberKt.e(e) { "Error closing stream" }
                    }
                }
            }
    }

    private val imageConfig: Config
        get() =
            Config(
                zoomDuration = viewer.config.doubleTapAnimDuration,
                minimumScaleType = viewer.config.imageScaleType,
                cropBorders = viewer.config.imageCropBorders,
                zoomStartPosition = viewer.config.imageZoomType,
                landscapeZoom = viewer.config.landscapeZoom,
                insetInfo =
                    InsetInfo(
                        cutoutBehavior = viewer.config.cutoutBehavior,
                        topCutoutInset =
                            viewer.activity.window.decorView.rootWindowInsets
                                ?.topCutoutInset()
                                ?.toFloat() ?: 0f,
                        bottomCutoutInset =
                            viewer.activity.window.decorView.rootWindowInsets
                                ?.bottomCutoutInset()
                                ?.toFloat() ?: 0f,
                        scaleTypeIsFullFit = viewer.config.scaleTypeIsFullFit(),
                        isFullscreen =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                viewer.config.isFullscreen &&
                                !viewer.activity.isInMultiWindowMode,
                        isSplitScreen =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                viewer.activity.isInMultiWindowMode,
                        insets = viewer.activity.window.decorView.rootWindowInsets,
                    ),
            )

    private suspend fun setBG(bytesArray: ByteArray): Drawable {
        return withContext(Default) {
            val readerPreferences by injectLazy<ReaderPreferences>()
            ImageUtil.autoSetBackground(
                BitmapFactory.decodeByteArray(bytesArray, 0, bytesArray.size),
                readerPreferences.readerTheme().get() == 2,
                readerPreferences.readerTheme().get() == 4,
                context,
            )
        }
    }

    /** Called when the page has an error. */
    private fun setError() {
        progressBar.isVisible = false
        initRetryButton().isVisible = true
    }

    /** Called when the image is decoded and going to be displayed. */
    private fun onImageDecoded() {
        progressBar.isVisible = false
    }

    /** Called when an image fails to decode. */
    private fun onImageDecodeError() {
        progressBar.isVisible = false
        initDecodeErrorLayout().isVisible = true
    }

    /** Creates a new progress bar. */
    private fun createProgressBar(): ReaderProgressBar {
        return ReaderProgressBar(context, null).apply {
            val size = 48.dpToPx
            layoutParams = LayoutParams(size, size).apply { gravity = Gravity.CENTER }
        }
    }

    private fun isInvertedFromTheme(): Boolean {
        return when (backgroundColor) {
            Color.WHITE -> context.isInNightMode()
            Color.BLACK -> !context.isInNightMode()
            else -> false
        }
    }

    /** Initializes a button to retry pages. */
    private fun initRetryButton(): PagerButton {
        if (retryButton != null) return retryButton!!

        retryButton =
            PagerButton(context, viewer).apply {
                layoutParams =
                    LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { gravity = Gravity.CENTER }
                setText(R.string.retry)
                setOnClickListener {
                    page.chapter.pageLoader?.retryPage(page)
                    extraPage?.let { it.chapter.pageLoader?.retryPage(it) }
                }
            }
        addView(retryButton)
        return retryButton!!
    }

    /** Initializes a decode error layout. */
    private fun initDecodeErrorLayout(): ViewGroup {
        if (decodeErrorLayout != null) return decodeErrorLayout!!

        val margins = 8.dpToPx

        val decodeLayout =
            LinearLayout(context).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.VERTICAL
            }
        decodeErrorLayout = decodeLayout

        TextView(context).apply {
            layoutParams =
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    setMargins(margins, margins, margins, margins)
                }
            gravity = Gravity.CENTER
            setText(R.string.decode_image_error)

            decodeLayout.addView(this)
        }

        PagerButton(context, viewer).apply {
            layoutParams =
                LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    setMargins(margins, margins, margins, margins)
                }
            setText(R.string.retry)
            setOnClickListener { page.chapter.pageLoader?.retryPage(page) }

            decodeLayout.addView(this)
        }

        val imageUrl = page.imageUrl
        if (imageUrl != null && imageUrl.startsWith("http", true)) {
            PagerButton(context, viewer).apply {
                layoutParams =
                    LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        setMargins(margins, margins, margins, margins)
                    }
                setText(R.string.open_in_browser)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, imageUrl.toUri())
                    context.startActivity(intent)
                }

                decodeLayout.addView(this)
            }
        }

        addView(decodeLayout)
        return decodeLayout
    }

    private fun mergeOrSplitPages(
        imageStream: InputStream,
        imageStream2: InputStream?,
    ): InputStream {
        if (ImageUtil.isAnimatedAndSupported(imageStream)) {
            imageStream.reset()
            if (page.longPage == null) {
                page.longPage = true
                if (viewer.config.splitPages || imageStream2 != null) {
                    splitDoublePages()
                }
            }
            scope.launchUI { progressBar.completeAndFadeOut() }
            return imageStream
        }
        if (page.longPage == true && viewer.config.splitPages) {
            val imageBytes = imageStream.readBytes()
            val imageBitmap =
                try {
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) {
                    imageStream.close()
                    TimberKt.e(e) { "Cannot split page" }
                    return imageBytes.inputStream()
                }
            val isLTR = (viewer !is R2LPagerViewer).xor(viewer.config.invertDoublePages)
            return ImageUtil.splitBitmap(imageBitmap, (page.firstHalf == false).xor(!isLTR)) {
                scope.launchUI {
                    if (it == 100) {
                        progressBar.completeAndFadeOut()
                    } else {
                        progressBar.setProgress(it)
                    }
                }
            }
        }
        if (imageStream2 == null) {
            if (viewer.config.splitPages && page.longPage == null) {
                val imageBytes = imageStream.readBytes()
                val imageBitmap =
                    try {
                        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    } catch (e: Exception) {
                        imageStream.close()
                        page.longPage = true
                        splitDoublePages()
                        TimberKt.e(e) { "Cannot split page" }
                        return imageBytes.inputStream()
                    }
                val height = imageBitmap.height
                val width = imageBitmap.width
                return if (height < width) {
                    imageStream.close()
                    page.longPage = true
                    splitDoublePages()
                    val isLTR = (viewer !is R2LPagerViewer).xor(viewer.config.invertDoublePages)
                    return ImageUtil.splitBitmap(imageBitmap, !isLTR) {
                        scope.launchUI {
                            if (it == 100) {
                                progressBar.completeAndFadeOut()
                            } else {
                                progressBar.setProgress(it)
                            }
                        }
                    }
                } else {
                    page.longPage = false
                    imageBytes.inputStream()
                }
            }
            return imageStream
        }
        if (page.fullPage == true) return imageStream
        if (ImageUtil.isAnimatedAndSupported(imageStream)) {
            page.fullPage = true
            splitDoublePages()
            return imageStream
        } else if (ImageUtil.isAnimatedAndSupported(imageStream)) {
            page.isolatedPage = true
            extraPage?.fullPage = true
            splitDoublePages()
            return imageStream
        }
        val imageBytes = imageStream.readBytes()
        val imageBitmap =
            try {
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                imageStream2.close()
                imageStream.close()
                page.fullPage = true
                splitDoublePages()
                TimberKt.e(e) { "Cannot combine pages" }
                return imageBytes.inputStream()
            }
        scope.launchUI { progressBar.setProgress(96) }
        val height = imageBitmap.height
        val width = imageBitmap.width

        if (height < width) {
            imageStream2.close()
            imageStream.close()
            page.fullPage = true
            splitDoublePages()
            return imageBytes.inputStream()
        }

        val imageBytes2 = imageStream2.readBytes()
        val imageBitmap2 =
            try {
                BitmapFactory.decodeByteArray(imageBytes2, 0, imageBytes2.size)
            } catch (e: Exception) {
                imageStream2.close()
                imageStream.close()
                extraPage?.fullPage = true
                page.isolatedPage = true
                splitDoublePages()
                TimberKt.e(e) { "Cannot combine pages" }
                return imageBytes.inputStream()
            }
        scope.launchUI { progressBar.setProgress(97) }
        val height2 = imageBitmap2.height
        val width2 = imageBitmap2.width

        if (height2 < width2) {
            imageStream2.close()
            imageStream.close()
            extraPage?.fullPage = true
            page.isolatedPage = true
            splitDoublePages()
            return imageBytes.inputStream()
        }
        val isLTR = (viewer !is R2LPagerViewer).xor(viewer.config.invertDoublePages)
        val bg =
            if (viewer.config.readerTheme >= 2 || viewer.config.readerTheme == 0) {
                Color.WHITE
            } else {
                Color.BLACK
            }

        imageStream.close()
        imageStream2.close()
        return ImageUtil.mergeBitmaps(
            imageBitmap,
            imageBitmap2,
            isLTR,
            bg,
            viewer.config.doublePageGap,
        ) {
            scope.launchUI {
                if (it == 100) {
                    progressBar.completeAndFadeOut()
                } else {
                    progressBar.setProgress(it)
                }
            }
        }
    }

    private fun splitDoublePages() {
        // extraPage ?: return
        scope.launchUI {
            delay(100)
            viewer.splitDoublePages(page)
            if (extraPage?.fullPage == true || page.fullPage == true) {
                extraPage = null
            }
        }
    }

    companion object {
        fun getBGType(readerTheme: Int, context: Context): Int {
            return if (readerTheme == 3) {
                if (context.isInNightMode()) 2 else 1
            } else {
                0 + (context.resources.configuration?.orientation ?: 0) * 10
            }
        }
    }
}
