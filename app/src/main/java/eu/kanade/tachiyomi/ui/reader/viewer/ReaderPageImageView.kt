package eu.kanade.tachiyomi.ui.reader.viewer

import ColorEInk16Bit
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import coil3.asDrawable
import coil3.dispose
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.size.Size
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
import com.github.chrisbanes.photoview.PhotoView
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonSubsamplingImageView
import eu.kanade.tachiyomi.util.system.GLUtil
import eu.kanade.tachiyomi.util.system.ImageUtil
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import java.io.InputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.BufferedSource
import org.nekomanga.domain.reader.ReaderPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * A wrapper view for showing page image.
 *
 * Animated image will be drawn by [PhotoView] while [SubsamplingScaleImageView] will take
 * non-animated image.
 *
 * @param isWebtoon if true, [WebtoonSubsamplingImageView] will be used instead of
 *   [SubsamplingScaleImageView] and [AppCompatImageView] will be used instead of [PhotoView]
 */
open class ReaderPageImageView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttrs: Int = 0,
    @StyleRes defStyleRes: Int = 0,
    private val isWebtoon: Boolean = false,
    private val readerPreferences: ReaderPreferences = Injekt.get(),
) : FrameLayout(context, attrs, defStyleAttrs, defStyleRes) {

    protected var pageView: View? = null

    private var config: Config? = null

    var onImageLoaded: (() -> Unit)? = null
    var onImageLoadError: (() -> Unit)? = null
    var onScaleChanged: ((newScale: Float) -> Unit)? = null
    var onViewClicked: (() -> Unit)? = null

    open fun onNeedsLandscapeZoom() {}

    @CallSuper
    open fun onImageLoaded() {
        onImageLoaded?.invoke()
    }

    @CallSuper
    open fun onImageLoadError() {
        onImageLoadError?.invoke()
    }

    @CallSuper
    open fun onScaleChanged(newScale: Float) {
        onScaleChanged?.invoke(newScale)
    }

    @CallSuper
    open fun onViewClicked() {
        onViewClicked?.invoke()
    }

    fun setImage(inputStream: InputStream, isAnimated: Boolean, config: Config) {
        if (isAnimated) {
            prepareAnimatedImageView()
            setAnimatedImage(inputStream, config)
        } else {
            prepareNonAnimatedImageView()
            setNonAnimatedImage(inputStream, config)
        }
    }

    fun setImage(source: BufferedSource, isAnimated: Boolean, config: Config) {
        if (isAnimated) {
            prepareAnimatedImageView()
            setAnimatedImage(source, config)
        } else {
            prepareNonAnimatedImageView()
            setNonAnimatedImage(source, config)
        }
    }

    fun recycle() =
        pageView?.let {
            when (it) {
                is SubsamplingScaleImageView -> it.recycle()
                is AppCompatImageView -> it.dispose()
            }
            it.isVisible = false
        }

    private fun prepareNonAnimatedImageView() {
        if (pageView is SubsamplingScaleImageView) return
        removeView(pageView)

        pageView =
            if (isWebtoon) {
                    WebtoonSubsamplingImageView(context)
                } else {
                    SubsamplingScaleImageView(context)
                }
                .apply {
                    setMaxTileSize(GLUtil.maxTextureSize)
                    setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
                    setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
                    setMinimumTileDpi(180)
                    setOnStateChangedListener(
                        object : SubsamplingScaleImageView.OnStateChangedListener {
                            override fun onScaleChanged(newScale: Float, origin: Int) {
                                this@ReaderPageImageView.onScaleChanged(newScale)
                            }

                            override fun onCenterChanged(newCenter: PointF?, origin: Int) {
                                // Not used
                            }
                        }
                    )
                    setOnClickListener { this@ReaderPageImageView.onViewClicked() }
                }
        addView(pageView, MATCH_PARENT, MATCH_PARENT)
    }

    protected fun SubsamplingScaleImageView.setupZoom(config: Config?) {
        // 5x zoom
        maxScale = scale * MAX_ZOOM_SCALE
        setDoubleTapZoomScale(scale * 2)

        config ?: return

        var centerV = 0f
        when (config.zoomStartPosition) {
            PagerConfig.ZoomType.Left -> {
                setScaleAndCenter(scale, PointF(0f, 0f))
            }
            PagerConfig.ZoomType.Right -> {
                setScaleAndCenter(scale, PointF(sWidth.toFloat(), 0f))
                centerV = sWidth.toFloat()
            }
            PagerConfig.ZoomType.Center -> {
                setScaleAndCenter(scale, center.also { it?.y = 0f })
                centerV = center?.x ?: 0f
            }
        }
        val insetInfo = config.insetInfo ?: return
        val topInsets = insetInfo.topCutoutInset
        val bottomInsets = insetInfo.bottomCutoutInset
        if (
            insetInfo.cutoutBehavior == PagerConfig.CUTOUT_START_EXTENDED &&
                topInsets + bottomInsets > 0 &&
                insetInfo.scaleTypeIsFullFit
        ) {
            setScaleAndCenter(
                scale,
                PointF(centerV, (center?.y?.plus(topInsets)?.minus(bottomInsets) ?: 0f)),
            )
        }
    }

    private fun setNonAnimatedImage(image: InputStream, config: Config) =
        (pageView as? SubsamplingScaleImageView)?.apply {
            setDoubleTapZoomDuration(config.zoomDuration.getSystemScaledDuration())
            setMinimumScaleType(config.minimumScaleType)
            setMinimumDpi(1) // Just so that very small image will be fit for initial load
            setCropBorders(config.cropBorders)
            if (config.insetInfo != null) {
                val topInsets = config.insetInfo.topCutoutInset
                val bottomInsets = config.insetInfo.bottomCutoutInset
                setExtendPastCutout(
                    config.insetInfo.cutoutBehavior == PagerConfig.CUTOUT_START_EXTENDED &&
                        config.insetInfo.scaleTypeIsFullFit &&
                        topInsets + bottomInsets > 0
                )
                if (
                    (config.insetInfo.cutoutBehavior != PagerConfig.CUTOUT_IGNORE ||
                        !config.insetInfo.scaleTypeIsFullFit) &&
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                        config.insetInfo.isFullscreen
                ) {
                    val insets: WindowInsets? = config.insetInfo.insets
                    setExtraSpace(
                        0f,
                        insets?.displayCutout?.boundingRectTop?.height()?.toFloat() ?: 0f,
                        0f,
                        insets?.displayCutout?.boundingRectBottom?.height()?.toFloat() ?: 0f,
                    )
                }
            }
            setOnImageEventListener(
                object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                    override fun onReady() {
                        // 5x zoom
                        setupZoom(config)
                        this@ReaderPageImageView.onNeedsLandscapeZoom()
                        this@ReaderPageImageView.onImageLoaded()
                    }

                    override fun onImageLoadError(e: Exception) {
                        this@ReaderPageImageView.onImageLoadError()
                    }
                }
            )

            if (readerPreferences.colorEInk16bit().get()) {
                val original = BitmapFactory.decodeStream(image)
                val processed =
                    runBlocking(Dispatchers.IO) {
                        ColorEInk16Bit(readerPreferences.colorEInkDither().get())
                            .transform(original, Size(original.width, original.height))
                    }
                setImage(ImageSource.bitmap(processed))
            } else setImage(ImageSource.inputStream(image))

            isVisible = true
        }

    private fun setNonAnimatedImage(data: BufferedSource, config: Config) =
        (pageView as? SubsamplingScaleImageView)?.apply {
            setHardwareConfig(ImageUtil.canUseHardwareBitmap(data))
            setDoubleTapZoomDuration(config.zoomDuration.getSystemScaledDuration())
            setMinimumScaleType(config.minimumScaleType)
            setMinimumDpi(1) // Just so that very small image will be fit for initial load
            setCropBorders(config.cropBorders)
            if (config.insetInfo != null) {
                val topInsets = config.insetInfo.topCutoutInset
                val bottomInsets = config.insetInfo.bottomCutoutInset
                setExtendPastCutout(
                    config.insetInfo.cutoutBehavior == PagerConfig.CUTOUT_START_EXTENDED &&
                        config.insetInfo.scaleTypeIsFullFit &&
                        topInsets + bottomInsets > 0
                )
                if (
                    (config.insetInfo.cutoutBehavior != PagerConfig.CUTOUT_IGNORE ||
                        !config.insetInfo.scaleTypeIsFullFit) &&
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                        config.insetInfo.isFullscreen
                ) {
                    val insets: WindowInsets? = config.insetInfo.insets
                    setExtraSpace(
                        0f,
                        insets?.displayCutout?.boundingRectTop?.height()?.toFloat() ?: 0f,
                        0f,
                        insets?.displayCutout?.boundingRectBottom?.height()?.toFloat() ?: 0f,
                    )
                }
            }
            setOnImageEventListener(
                object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                    override fun onReady() {
                        // 5x zoom
                        setupZoom(config)
                        this@ReaderPageImageView.onNeedsLandscapeZoom()
                        this@ReaderPageImageView.onImageLoaded()
                    }

                    override fun onImageLoadError(e: Exception) {
                        this@ReaderPageImageView.onImageLoadError()
                    }
                }
            )

            if (readerPreferences.colorEInk16bit().get()) {
                val original = BitmapFactory.decodeStream(data.inputStream())
                val processed =
                    runBlocking(Dispatchers.IO) {
                        ColorEInk16Bit(readerPreferences.colorEInkDither().get())
                            .transform(original, Size(original.width, original.height))
                    }
                setImage(ImageSource.bitmap(processed))
            } else setImage(ImageSource.inputStream(data.inputStream()))

            isVisible = true
        }

    private fun prepareAnimatedImageView() {
        if (pageView is AppCompatImageView) return
        removeView(pageView)

        pageView =
            if (isWebtoon) {
                    AppCompatImageView(context)
                } else {
                    PhotoView(context)
                }
                .apply {
                    adjustViewBounds = true

                    if (this is PhotoView) {
                        setScaleLevels(1F, 2F, MAX_ZOOM_SCALE)
                        // Force 2 scale levels on double tap
                        setOnDoubleTapListener(
                            object : GestureDetector.SimpleOnGestureListener() {
                                override fun onDoubleTap(e: MotionEvent): Boolean {
                                    if (scale > 1F) {
                                        setScale(1F, e.x, e.y, true)
                                    } else {
                                        setScale(2F, e.x, e.y, true)
                                    }
                                    return true
                                }

                                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                                    this@ReaderPageImageView.onViewClicked()
                                    return super.onSingleTapConfirmed(e)
                                }
                            }
                        )
                        setOnScaleChangeListener { _, _, _ ->
                            this@ReaderPageImageView.onScaleChanged(scale)
                        }
                    }
                }
        addView(pageView, MATCH_PARENT, MATCH_PARENT)
    }

    private fun setAnimatedImage(image: InputStream, config: Config) =
        (pageView as? AppCompatImageView)?.apply {
            if (this is PhotoView) {
                setZoomTransitionDuration(config.zoomDuration.getSystemScaledDuration())
            }

            val request =
                ImageRequest.Builder(context)
                    .data(ByteBuffer.wrap(image.readBytes()))
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .target(
                        onSuccess = { result ->
                            val drawable = result.asDrawable(context.resources)
                            setImageDrawable(drawable)
                            (drawable as? Animatable)?.start()
                            isVisible = true
                            this@ReaderPageImageView.onImageLoaded()
                        },
                        onError = { this@ReaderPageImageView.onImageLoadError() },
                    )
                    .crossfade(false)
                    .allowHardware(false) // Disable hardware acceleration for GIFs
                    .build()
            context.imageLoader.enqueue(request)
        }

    private fun setAnimatedImage(data: BufferedSource, config: Config) =
        (pageView as? AppCompatImageView)?.apply {
            if (this is PhotoView) {
                setZoomTransitionDuration(config.zoomDuration.getSystemScaledDuration())
            }

            val request =
                ImageRequest.Builder(context)
                    .data(ByteBuffer.wrap(data.readByteArray()))
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .target(
                        onSuccess = { result ->
                            val drawable = result.asDrawable(context.resources)
                            setImageDrawable(drawable)
                            (drawable as? Animatable)?.start()
                            isVisible = true
                            this@ReaderPageImageView.onImageLoaded()
                        },
                        onError = { this@ReaderPageImageView.onImageLoadError() },
                    )
                    .crossfade(false)
                    .allowHardware(false) // Disable hardware acceleration for GIFs
                    .build()
            context.imageLoader.enqueue(request)
        }

    private fun Int.getSystemScaledDuration(): Int {
        return (this * context.animatorDurationScale).toInt().coerceAtLeast(1)
    }

    /** All of the config except [zoomDuration] will only be used for non-animated image. */
    data class Config(
        val zoomDuration: Int,
        val minimumScaleType: Int = SCALE_TYPE_CENTER_INSIDE,
        val cropBorders: Boolean = false,
        val zoomStartPosition: PagerConfig.ZoomType = PagerConfig.ZoomType.Center,
        val landscapeZoom: Boolean = false,
        val insetInfo: InsetInfo? = null,
    )

    data class InsetInfo(
        val cutoutBehavior: Int,
        val topCutoutInset: Float,
        val bottomCutoutInset: Float,
        val scaleTypeIsFullFit: Boolean,
        val isFullscreen: Boolean,
        val isSplitScreen: Boolean,
        val insets: WindowInsets?,
    )
}

private const val MAX_ZOOM_SCALE = 5F
