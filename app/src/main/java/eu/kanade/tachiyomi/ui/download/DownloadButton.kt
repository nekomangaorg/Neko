package eu.kanade.tachiyomi.ui.download

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import kotlinx.android.synthetic.main.download_button.view.*

class DownloadButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null)
    : FrameLayout(context, attrs) {

    private val activeColor = context.getResourceColor(R.attr.colorAccent)
    private val disabledColor = ContextCompat.getColor(context,
        R.color.material_on_surface_disabled)
    private val downloadedColor = ContextCompat.getColor(context,
        R.color.material_green_800)
    private val errorColor = ContextCompat.getColor(context,
        R.color.red_error)
    private val filledCircle = ContextCompat.getDrawable(context,
        R.drawable.filled_circle)?.mutate()
    private val borderCircle = ContextCompat.getDrawable(context,
        R.drawable.border_circle)?.mutate()


    fun setDownoadStatus(state: Int, progress: Int = 0) {
        when (state) {
            Download.NOT_DOWNLOADED -> {
                download_border.visible()
                download_progress.gone()
                download_progress_indeterminate.gone()
                download_border.setImageDrawable(borderCircle)
                download_border.drawable.setTint(activeColor)
                download_icon.drawable.setTint(activeColor)
            }
            Download.QUEUE -> {
                download_border.gone()
                download_progress.gone()
                download_progress_indeterminate.visible()
                download_progress.isIndeterminate = true
                download_icon.drawable.setTint(disabledColor)
            }
            Download.DOWNLOADING -> {
                download_border.visible()
                download_progress.visible()
                download_progress_indeterminate.gone()
                download_border.setImageDrawable(borderCircle)
                download_progress.isIndeterminate = false
                download_progress.progress = progress
                download_border.drawable.setTint(disabledColor)
                download_progress.progressDrawable?.setTint(downloadedColor)
                download_icon.drawable.setTint(disabledColor)
            }
            Download.DOWNLOADED -> {
                download_progress.gone()
                download_border.visible()
                download_progress_indeterminate.gone()
                download_border.setImageDrawable(filledCircle)
                download_border.drawable.setTint(downloadedColor)
                download_icon.drawable.setTint(Color.WHITE)
            }
            Download.ERROR -> {
                download_progress.gone()
                download_border.visible()
                download_progress_indeterminate.gone()
                download_border.setImageDrawable(borderCircle)
                download_border.drawable.setTint(errorColor)
                download_icon.drawable.setTint(errorColor)
            }
        }
    }
}