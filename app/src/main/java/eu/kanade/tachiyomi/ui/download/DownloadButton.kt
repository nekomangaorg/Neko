package eu.kanade.tachiyomi.ui.download

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.databinding.DownloadButtonBinding
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.widget.EndAnimatorListener

class DownloadButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private val activeColor = ColorUtils.blendARGB(
        context.getResourceColor(R.attr.colorAccent),
        ContextCompat.getColor(
            context,
            R.color.download
        ),
        0.3f
    )
    private val progressBGColor = ContextCompat.getColor(
        context,
        R.color.divider
    )
    private val disabledColor = ContextCompat.getColor(
        context,
        R.color.material_on_surface_disabled
    )
    private val downloadedColor = context.getResourceColor(R.attr.colorAccent)
    private val downloadedTextColor = context.getResourceColor(R.attr.colorOnAccent)
    private val errorColor = ContextCompat.getColor(
        context,
        R.color.material_red_500
    )
    private val filledCircle = ContextCompat.getDrawable(
        context,
        R.drawable.filled_circle
    )?.mutate()
    private val borderCircle = ContextCompat.getDrawable(
        context,
        R.drawable.border_circle
    )?.mutate()
    private val downloadDrawable = ContextCompat.getDrawable(
        context,
        R.drawable.ic_arrow_downward_24dp
    )?.mutate()
    private val checkDrawable = ContextCompat.getDrawable(
        context,
        R.drawable.ic_check_24dp
    )?.mutate()
    private val filledAnim = AnimatedVectorDrawableCompat.create(
        context,
        R.drawable.anim_outline_to_filled
    )
    private val checkAnim = AnimatedVectorDrawableCompat.create(
        context,
        R.drawable.anim_dl_to_check_to_dl
    )
    private var isAnimating = false
    private var iconAnimation: ObjectAnimator? = null

    lateinit var binding: DownloadButtonBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = DownloadButtonBinding.bind(this)
    }

    fun setDownloadStatus(state: Int, progress: Int = 0, animated: Boolean = false) {
        if (state != Download.DOWNLOADING) {
            iconAnimation?.cancel()
            binding.downloadIcon.alpha = 1f
            isAnimating = false
        }
        binding.downloadIcon.setImageDrawable(
            if (state == Download.CHECKED) {
                checkDrawable
            } else downloadDrawable
        )
        when (state) {
            Download.CHECKED -> {
                binding.downloadProgress.gone()
                binding.downloadBorder.visible()
                binding.downloadProgressIndeterminate.gone()
                binding.downloadBorder.setImageDrawable(filledCircle)
                binding.downloadBorder.drawable.setTint(activeColor)
                binding.downloadIcon.drawable.setTint(Color.WHITE)
            }
            Download.NOT_DOWNLOADED -> {
                binding.downloadBorder.visible()
                binding.downloadProgress.gone()
                binding.downloadProgressIndeterminate.gone()
                binding.downloadBorder.setImageDrawable(borderCircle)
                binding.downloadBorder.drawable.setTint(activeColor)
                binding.downloadIcon.drawable.setTint(activeColor)
            }
            Download.QUEUE -> {
                binding.downloadBorder.gone()
                binding.downloadProgress.gone()
                binding.downloadProgressIndeterminate.visible()
                binding.downloadProgress.isIndeterminate = true
                binding.downloadIcon.drawable.setTint(disabledColor)
            }
            Download.DOWNLOADING -> {
                binding.downloadBorder.visible()
                binding.downloadProgress.visible()
                binding.downloadProgressIndeterminate.gone()
                binding.downloadBorder.setImageDrawable(borderCircle)
                binding.downloadProgress.isIndeterminate = false
                binding.downloadProgress.progress = progress
                binding.downloadBorder.drawable.setTint(progressBGColor)
                binding.downloadProgress.progressDrawable?.setTint(downloadedColor)
                binding.downloadIcon.drawable.setTint(disabledColor)
                if (!isAnimating) {
                    iconAnimation = ObjectAnimator.ofFloat(binding.downloadIcon, "alpha", 1f, 0f).apply {
                        duration = 1000
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ObjectAnimator.REVERSE
                    }
                    iconAnimation?.start()
                    isAnimating = true
                }
            }
            Download.DOWNLOADED -> {
                binding.downloadProgress.gone()
                binding.downloadBorder.visible()
                binding.downloadProgressIndeterminate.gone()
                binding.downloadBorder.drawable.setTint(downloadedColor)
                if (animated) {
                    binding.downloadBorder.setImageDrawable(filledAnim)
                    binding.downloadIcon.setImageDrawable(checkAnim)
                    filledAnim?.start()
                    val alphaAnimation = ValueAnimator.ofArgb(disabledColor, downloadedTextColor)
                    alphaAnimation.addUpdateListener { valueAnimator ->
                        binding.downloadIcon.drawable.setTint(valueAnimator.animatedValue as Int)
                    }
                    alphaAnimation.addListener(
                        EndAnimatorListener {
                            binding.downloadIcon.drawable.setTint(downloadedTextColor)
                            checkAnim?.start()
                        }
                    )
                    alphaAnimation.duration = 150
                    alphaAnimation.start()
                    binding.downloadBorder.drawable.setTint(downloadedColor)
                } else {
                    binding.downloadBorder.setImageDrawable(filledCircle)
                    binding.downloadIcon.drawable.setTint(downloadedTextColor)
                }
            }
            Download.ERROR -> {
                binding.downloadProgress.gone()
                binding.downloadBorder.visible()
                binding.downloadProgressIndeterminate.gone()
                binding.downloadBorder.setImageDrawable(borderCircle)
                binding.downloadBorder.drawable.setTint(errorColor)
                binding.downloadIcon.drawable.setTint(errorColor)
            }
        }
    }
}
