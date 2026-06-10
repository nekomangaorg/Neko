package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPageSplit
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressBar
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
import org.nekomanga.R

/**
 * Holder of the webtoon reader for a single page of a chapter.
 *
 * @param frame the root view for this holder.
 * @param viewer the webtoon viewer.
 * @constructor creates a new webtoon holder.
 */
class WebtoonPageHolder(private val frame: ReaderPageImageView, viewer: WebtoonViewer) :
    WebtoonBaseHolder(frame, viewer) {

    /** Loading progress bar to indicate the current progress. */
    private val progressBar = createProgressBar()

    /**
     * Progress bar container. Needed to keep a minimum height size of the holder, otherwise the
     * adapter would create more views to fill the screen, which is not wanted.
     */
    private lateinit var progressContainer: ViewGroup

    /** Retry button container used to allow retrying. */
    private var retryContainer: ViewGroup? = null

    /** Error layout to show when the image fails to decode. */
    private var decodeErrorLayout: ViewGroup? = null

    /** Getter to retrieve the height of the recycler view. */
    private val parentHeight
        get() = viewer.recycler.height

    /** Page of a chapter. */
    private var page: ReaderPage? = null

    private var regionTop = 0
    private var regionHeight = 0
    private var splitPage: ReaderPageSplit? = null

    private val scope = MainScope()

    /** Job for loading the page. */
    private var loadJob: Job? = null

    /** Job for status changes of the page. */
    private var statusJob: Job? = null

    /** Job for progress changes of the page. */
    private var progressJob: Job? = null

    /**
     * Job used to read the header of the image. This is needed in order to instantiate the
     * appropriate image view depending if the image is animated (GIF).
     */
    private var readImageHeaderSubscription: Job? = null

    init {
        refreshLayoutParams()
        frame.setBackgroundColor(Color.BLACK)

        frame.onImageLoaded = { onImageDecoded() }
        frame.onImageLoadError = { onImageDecodeError() }
        frame.onScaleChanged = { viewer.activity.hideMenu() }
    }

    fun bind(item: Any) {
        when (item) {
            is ReaderPage -> {
                page = item
                regionTop = 0
                regionHeight = 0
                splitPage = null
            }
            is ReaderPageSplit -> {
                page = item.page
                regionTop = item.topOffset
                regionHeight = item.splitHeight
                splitPage = item
                if (item.displayedHeight > 0) {
                    progressContainer.layoutParams?.height = item.displayedHeight
                }
            }
        }
        launchLoadJob()
        refreshLayoutParams()
    }

    private fun refreshLayoutParams() {
        frame.layoutParams =
            FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                val margin =
                    Resources.getSystem().displayMetrics.widthPixels *
                        (viewer.config.sidePadding / 100f)
                marginEnd = margin.toInt()
                marginStart = margin.toInt()
            }
        if (viewer.hasMargins) {
            frame.updatePaddingRelative(bottom = 15.dpToPx)
        }
    }

    /** Called when the view is recycled and added to the view pool. */
    override fun recycle() {
        cancelLoadJob()
        cancelProgressJob()
        unsubscribeReadImageHeader()

        regionTop = 0
        regionHeight = 0

        removeDecodeErrorLayout()
        frame.recycle()
        progressBar.setProgress(0)
        progressContainer.isVisible = true
    }

    /**
     * Observes the status of the page and notify the changes.
     *
     * @see processStatus
     */
    private fun launchLoadJob() {
        cancelLoadJob()

        val page = page ?: return
        val loader = page.chapter.pageLoader ?: return
        loadJob = scope.launch { loader.loadPage(page) }
        statusJob = scope.launch { page.statusFlow.collectLatest { processStatus(it) } }
    }

    /** Observes the progress of the page and updates view. */
    private fun launchProgressJob() {
        cancelProgressJob()

        val page = page ?: return

        progressJob = scope.launch {
            page.progressFlow.collectLatest { value -> progressBar.setProgress(value) }
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
                setImage()
                cancelProgressJob()
            }
            Page.State.ERROR -> {
                setError()
                cancelProgressJob()
            }
        }
    }

    /** Cancels loading the page and processing changes to the page's status. */
    private fun cancelLoadJob() {
        loadJob?.cancel()
        loadJob = null
        statusJob?.cancel()
        statusJob = null
    }

    /** Unsubscribes from the progress subscription. */
    private fun cancelProgressJob() {
        progressJob?.cancel()
        progressJob = null
    }

    /** Unsubscribes from the read image header subscription. */
    private fun unsubscribeReadImageHeader() {
        readImageHeaderSubscription?.cancel()
        readImageHeaderSubscription = null
    }

    /** Called when the page is queued. */
    private fun setQueued() {
        progressContainer.isVisible = true
        progressBar.isVisible = true
        retryContainer?.isVisible = false
        removeDecodeErrorLayout()
    }

    /** Called when the page is loading. */
    private fun setLoading() {
        progressContainer.isVisible = true
        progressBar.isVisible = true
        retryContainer?.isVisible = false
        removeDecodeErrorLayout()
    }

    /** Called when the page is downloading */
    private fun setDownloading() {
        progressContainer.isVisible = true
        progressBar.isVisible = true
        retryContainer?.isVisible = false
        removeDecodeErrorLayout()
    }

    /** Called when the page is ready. */
    private fun setImage() {
        progressContainer.isVisible = true
        progressBar.isVisible = true
        progressBar.completeAndFadeOut()
        retryContainer?.isVisible = false
        removeDecodeErrorLayout()

        unsubscribeReadImageHeader()
        val streamFn = page?.stream ?: return

        readImageHeaderSubscription =
            scope.launch(Dispatchers.IO) {
                var openStream: BufferedSource? = null
                try {
                    val stream = streamFn().source().buffer()
                    openStream = stream
                    val isAnimated = ImageUtil.isAnimatedAndSupported(stream)
                    openStream = process(stream)

                    withContext(Dispatchers.Main) {
                        openStream?.let {
                            frame.setImage(
                                it,
                                isAnimated,
                                ReaderPageImageView.Config(
                                    zoomDuration = viewer.config.doubleTapAnimDuration,
                                    minimumScaleType =
                                        SubsamplingScaleImageView.SCALE_TYPE_FIT_WIDTH,
                                    cropBorders =
                                        if (viewer.hasMargins) {
                                            viewer.config.verticalCropBorders
                                        } else {
                                            viewer.config.webtoonCropBorders
                                        },
                                ),
                            )
                        }
                    }

                    // Keep the coroutine alive to close the input stream only when cancelled
                    awaitCancellation()
                } catch (e: Exception) {
                    org.nekomanga.logging.TimberKt.e(e) { "Error loading webtoon page" }
                } finally {
                    runCatching { openStream?.close() }
                }
            }
    }

    private suspend fun checkTallImage(stream: BufferedSource): BufferedSource {
        if (!viewer.config.splitTallPages || regionHeight > 0 || page == null) {
            return stream
        }

        val imageBytes = stream.readByteArray()

        if (viewer.adapter.tallSplitPages.contains(page)) {
            val firstSplit =
                viewer.adapter.items
                    .filterIsInstance<ReaderPageSplit>()
                    .firstOrNull { it.page == page }
            if (firstSplit != null) {
                regionTop = 0
                regionHeight = firstSplit.topOffset
                return decodeRegion(imageBytes, 0, regionHeight)
            }
            return Buffer().write(imageBytes)
        }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        if (options.outHeight <= 0 || options.outWidth <= 0) {
            return Buffer().write(imageBytes)
        }

        if (options.outHeight / options.outWidth <= 3) {
            return Buffer().write(imageBytes)
        }

        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val displayMaxHeight = maxOf(viewer.recycler.height, screenHeight) * 2
        if (options.outHeight <= displayMaxHeight) {
            return Buffer().write(imageBytes)
        }

        val partCount = (options.outHeight - 1) / displayMaxHeight + 1
        val optimalSplitHeight = options.outHeight / partCount

        val insertPages = mutableListOf<ReaderPageSplit>()
        for (i in 1 until partCount) {
            val topOffset = i * optimalSplitHeight
            val splitH = minOf(optimalSplitHeight, options.outHeight - topOffset)
            insertPages.add(ReaderPageSplit(page!!, topOffset, splitH))
        }

        if (insertPages.isNotEmpty()) {
            withContext(Dispatchers.Main) { viewer.adapter.notifyPageSplit(page!!, insertPages) }
            regionTop = 0
            regionHeight = optimalSplitHeight
        }

        return decodeRegion(imageBytes, 0, optimalSplitHeight)
    }

    private fun decodeRegion(
        imageBytes: ByteArray,
        top: Int,
        height: Int,
    ): BufferedSource {
        val decoder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                BitmapRegionDecoder.newInstance(imageBytes, 0, imageBytes.size)
            } else {
                @Suppress("DEPRECATION")
                BitmapRegionDecoder.newInstance(imageBytes, 0, imageBytes.size, false)
            }

        val region = Rect(0, top, decoder.width, top + height)
        val bitmap =
            decoder.decodeRegion(
                region,
                BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 },
            )
        decoder.recycle()

        bitmap ?: return Buffer().write(imageBytes)

        return Buffer().write(ImageUtil.bitmapToBytes(bitmap))
    }

    private suspend fun process(imageStream: BufferedSource): BufferedSource {
        if (regionHeight > 0) {
            val split = splitPage
            if (split?.cachedBytes != null) {
                return Buffer().write(split.cachedBytes!!)
            }
            val imageBytes = imageStream.readByteArray()
            val result = decodeRegion(imageBytes, regionTop, regionHeight)
            if (split != null) {
                split.cachedBytes = result.readByteArray()
                return Buffer().write(split.cachedBytes!!)
            }
            return result
        }

        if (!viewer.config.splitPages) {
            return checkTallImage(imageStream)
        }

        val isDoublePage = ImageUtil.isWideImage(imageStream)
        if (!isDoublePage) {
            return checkTallImage(imageStream)
        }

        return ImageUtil.splitAndStackBitmap(
            imageStream,
            viewer.config.invertDoublePages,
            viewer.hasMargins,
        )
    }

    /** Called when the page has an error. */
    private fun setError() {
        progressContainer.isVisible = false
        initRetryLayout().isVisible = true
    }

    /** Called when the image is decoded and going to be displayed. */
    private fun onImageDecoded() {
        progressContainer.isVisible = false
        val h = frame.measuredHeight
        page?.renderedHeight = h
        splitPage?.displayedHeight = h
    }

    /** Called when the image fails to decode. */
    private fun onImageDecodeError() {
        progressContainer.isVisible = false
        initDecodeErrorLayout().isVisible = true
    }

    /** Creates a new progress bar. */
    @SuppressLint("PrivateResource")
    private fun createProgressBar(): ReaderProgressBar {
        progressContainer = FrameLayout(context)
        frame.addView(progressContainer, MATCH_PARENT, parentHeight)

        val progress =
            ReaderProgressBar(context).apply {
                val size = 48.dpToPx
                layoutParams =
                    FrameLayout.LayoutParams(size, size).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, parentHeight / 4, 0, 0)
                    }
            }
        progressContainer.addView(progress)
        return progress
    }

    /** Initializes a button to retry pages. */
    private fun initRetryLayout(): ViewGroup {
        if (retryContainer != null) return retryContainer!!

        retryContainer = FrameLayout(context)
        frame.addView(retryContainer, MATCH_PARENT, parentHeight)

        AppCompatButton(context).apply {
            layoutParams =
                FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setMargins(0, parentHeight / 4, 0, 0)
                }
            setText(R.string.retry)
            setOnClickListener { page?.let { it.chapter.pageLoader?.retryPage(it) } }

            retryContainer!!.addView(this)
        }
        return retryContainer!!
    }

    /** Initializes a decode error layout. */
    private fun initDecodeErrorLayout(): ViewGroup {
        if (decodeErrorLayout != null) return decodeErrorLayout!!

        val margins = 8.dpToPx

        val decodeLayout =
            LinearLayout(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(MATCH_PARENT, parentHeight).apply {
                        setMargins(0, parentHeight / 6, 0, 0)
                    }
                gravity = Gravity.CENTER_HORIZONTAL
                orientation = LinearLayout.VERTICAL
            }
        decodeErrorLayout = decodeLayout

        TextView(context).apply {
            layoutParams =
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    setMargins(0, margins, 0, margins)
                }
            gravity = Gravity.CENTER
            setText(R.string.decode_image_error)

            decodeLayout.addView(this)
        }

        AppCompatButton(context).apply {
            layoutParams =
                FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    setMargins(0, margins, 0, margins)
                }
            setText(R.string.retry)
            setOnClickListener { page?.let { it.chapter.pageLoader?.retryPage(it) } }

            decodeLayout.addView(this)
        }

        val imageUrl = page?.imageUrl
        if (imageUrl != null && imageUrl.startsWith("http")) {
            AppCompatButton(context).apply {
                layoutParams =
                    FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        setMargins(0, margins, 0, margins)
                    }
                setText(R.string.open_in_browser)
                setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, imageUrl.toUri())
                    context.startActivity(intent)
                }

                decodeLayout.addView(this)
            }
        }

        frame.addView(decodeLayout)
        return decodeLayout
    }

    /** Removes the decode error layout from the holder, if found. */
    private fun removeDecodeErrorLayout() {
        val layout = decodeErrorLayout
        if (layout != null) {
            frame.removeView(layout)
            decodeErrorLayout = null
        }
    }
}
