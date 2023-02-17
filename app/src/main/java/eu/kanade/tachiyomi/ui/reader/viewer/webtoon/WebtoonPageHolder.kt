package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
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
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressBar
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * Holder of the webtoon reader for a single page of a chapter.
 *
 * @param frame the root view for this holder.
 * @param viewer the webtoon viewer.
 * @constructor creates a new webtoon holder.
 */
class WebtoonPageHolder(
    private val frame: ReaderPageImageView,
    viewer: WebtoonViewer,
) : WebtoonBaseHolder(frame, viewer) {

    /**
     * Loading progress bar to indicate the current progress.
     */
    private val progressBar = createProgressBar()

    /**
     * Progress bar container. Needed to keep a minimum height size of the holder, otherwise the
     * adapter would create more views to fill the screen, which is not wanted.
     */
    private lateinit var progressContainer: ViewGroup

    /**
     * Retry button container used to allow retrying.
     */
    private var retryContainer: ViewGroup? = null

    /**
     * Error layout to show when the image fails to decode.
     */
    private var decodeErrorLayout: ViewGroup? = null

    /**
     * Getter to retrieve the height of the recycler view.
     */
    private val parentHeight
        get() = viewer.recycler.height

    /**
     * Page of a chapter.
     */
    private var page: ReaderPage? = null

    private val scope = MainScope()

    /**
     * Job for loading the page.
     */
    private var loadJob: Job? = null

    /**
     * Job for status changes of the page.
     */
    private var statusJob: Job? = null

    /**
     * Job for progress changes of the page.
     */
    private var progressJob: Job? = null

    /**
     * Subscription used to read the header of the image. This is needed in order to instantiate
     * the appropriate image view depending if the image is animated (GIF).
     */
    private var readImageHeaderSubscription: Subscription? = null

    init {
        refreshLayoutParams()
        frame.setBackgroundColor(Color.BLACK)

        frame.onImageLoaded = { onImageDecoded() }
        frame.onImageLoadError = { onImageDecodeError() }
        frame.onScaleChanged = { viewer.activity.hideMenu() }
    }

    /**
     * Binds the given [page] with this view holder, subscribing to its state.
     */
    fun bind(page: ReaderPage) {
        this.page = page
        launchLoadJob()
        refreshLayoutParams()
    }

    private fun refreshLayoutParams() {
        frame.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            val margin = Resources.getSystem().displayMetrics.widthPixels * (viewer.config.sidePadding / 100f)
            marginEnd = margin.toInt()
            marginStart = margin.toInt()
        }
        if (viewer.hasMargins) {
            frame.updatePaddingRelative(bottom = 15.dpToPx)
        }
    }

    /**
     * Called when the view is recycled and added to the view pool.
     */
    override fun recycle() {
        cancelLoadJob()
        cancelProgressJob()
        unsubscribeReadImageHeader()

        removeDecodeErrorLayout()
        frame.recycle()
        progressBar.setProgress(0)
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
        loadJob = scope.launch {
            loader.loadPage(page)
        }
        statusJob = scope.launch {
            page.statusFlow.collectLatest { processStatus(it) }
        }
    }

    /**
     * Observes the progress of the page and updates view.
     */
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

    /**
     * Cancels loading the page and processing changes to the page's status.
     */
    private fun cancelLoadJob() {
        loadJob?.cancel()
        loadJob = null
        statusJob?.cancel()
        statusJob = null
    }

    /**
     * Unsubscribes from the progress subscription.
     */
    private fun cancelProgressJob() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * Unsubscribes from the read image header subscription.
     */
    private fun unsubscribeReadImageHeader() {
        removeSubscription(readImageHeaderSubscription)
        readImageHeaderSubscription = null
    }

    /**
     * Called when the page is queued.
     */
    private fun setQueued() {
        progressContainer.isVisible = true
        progressBar.isVisible = true
        retryContainer?.isVisible = false
        removeDecodeErrorLayout()
    }

    /**
     * Called when the page is loading.
     */
    private fun setLoading() {
        progressContainer.isVisible = true
        progressBar.isVisible = true
        retryContainer?.isVisible = false
        removeDecodeErrorLayout()
    }

    /**
     * Called when the page is downloading
     */
    private fun setDownloading() {
        progressContainer.isVisible = true
        progressBar.isVisible = true
        retryContainer?.isVisible = false
        removeDecodeErrorLayout()
    }

    /**
     * Called when the page is ready.
     */
    private fun setImage() {
        progressContainer.isVisible = true
        progressBar.isVisible = true
        progressBar.completeAndFadeOut()
        retryContainer?.isVisible = false
        removeDecodeErrorLayout()

        unsubscribeReadImageHeader()
        val streamFn = page?.stream ?: return

        var openStream: InputStream? = null
        readImageHeaderSubscription = Observable
            .fromCallable {
                val stream = streamFn().buffered(16)
                openStream = process(stream)

                ImageUtil.isAnimatedAndSupported(stream)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { isAnimated ->
                frame.setImage(
                    openStream!!,
                    isAnimated,
                    ReaderPageImageView.Config(
                        zoomDuration = viewer.config.doubleTapAnimDuration,
                        minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_FIT_WIDTH,
                        cropBorders =
                        if (viewer.hasMargins) {
                            viewer.config.verticalCropBorders
                        } else {
                            viewer.config.webtoonCropBorders
                        },
                    ),
                )
            }
            // Keep the Rx stream alive to close the input stream only when unsubscribed
            .flatMap { Observable.never<Unit>() }
            .doOnUnsubscribe { openStream?.close() }
            .subscribe({}, {})

        addSubscription(readImageHeaderSubscription)
    }

    private fun process(imageStream: BufferedInputStream): InputStream {
        if (!viewer.config.splitPages) {
            return imageStream
        }

        val isDoublePage = ImageUtil.isWideImage(imageStream)
        if (!isDoublePage) {
            return imageStream
        }

        return ImageUtil.splitAndStackBitmap(imageStream, viewer.config.invertDoublePages, viewer.hasMargins)
    }

    /**
     * Called when the page has an error.
     */
    private fun setError() {
        progressContainer.isVisible = false
        initRetryLayout().isVisible = true
    }

    /**
     * Called when the image is decoded and going to be displayed.
     */
    private fun onImageDecoded() {
        progressContainer.isVisible = false
    }

    /**
     * Called when the image fails to decode.
     */
    private fun onImageDecodeError() {
        progressContainer.isVisible = false
        initDecodeErrorLayout().isVisible = true
    }

    /**
     * Creates a new progress bar.
     */
    @SuppressLint("PrivateResource")
    private fun createProgressBar(): ReaderProgressBar {
        progressContainer = FrameLayout(context)
        frame.addView(progressContainer, MATCH_PARENT, parentHeight)

        val progress = ReaderProgressBar(context).apply {
            val size = 48.dpToPx
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, parentHeight / 4, 0, 0)
            }
        }
        progressContainer.addView(progress)
        return progress
    }

    /**
     * Initializes a button to retry pages.
     */
    private fun initRetryLayout(): ViewGroup {
        if (retryContainer != null) return retryContainer!!

        retryContainer = FrameLayout(context)
        frame.addView(retryContainer, MATCH_PARENT, parentHeight)

        AppCompatButton(context).apply {
            layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, parentHeight / 4, 0, 0)
            }
            setText(R.string.retry)
            setOnClickListener {
                page?.let { it.chapter.pageLoader?.retryPage(it) }
            }

            retryContainer!!.addView(this)
        }
        return retryContainer!!
    }

    /**
     * Initializes a decode error layout.
     */
    private fun initDecodeErrorLayout(): ViewGroup {
        if (decodeErrorLayout != null) return decodeErrorLayout!!

        val margins = 8.dpToPx

        val decodeLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, parentHeight).apply {
                setMargins(0, parentHeight / 6, 0, 0)
            }
            gravity = Gravity.CENTER_HORIZONTAL
            orientation = LinearLayout.VERTICAL
        }
        decodeErrorLayout = decodeLayout

        TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                setMargins(0, margins, 0, margins)
            }
            gravity = Gravity.CENTER
            setText(R.string.decode_image_error)

            decodeLayout.addView(this)
        }

        AppCompatButton(context).apply {
            layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                setMargins(0, margins, 0, margins)
            }
            setText(R.string.retry)
            setOnClickListener {
                page?.let { it.chapter.pageLoader?.retryPage(it) }
            }

            decodeLayout.addView(this)
        }

        val imageUrl = page?.imageUrl
        if (imageUrl != null && imageUrl.startsWith("http")) {
            AppCompatButton(context).apply {
                layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
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

    /**
     * Removes the decode error layout from the holder, if found.
     */
    private fun removeDecodeErrorLayout() {
        val layout = decodeErrorLayout
        if (layout != null) {
            frame.removeView(layout)
            decodeErrorLayout = null
        }
    }
}
