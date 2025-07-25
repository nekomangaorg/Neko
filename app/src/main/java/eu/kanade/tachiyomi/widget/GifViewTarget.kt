package eu.kanade.tachiyomi.widget

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import coil3.Image
import coil3.target.ImageViewTarget

class GifViewTarget(
    view: ImageView,
    private val progressBar: View?,
    private val decodeErrorLayout: ViewGroup?,
) : ImageViewTarget(view) {

    override fun onError(error: Image?) {
        progressBar?.isVisible = false
        decodeErrorLayout?.isVisible = true
    }

    override fun onSuccess(result: Image) {
        progressBar?.isVisible = false
        decodeErrorLayout?.isVisible = false
        super.onSuccess(result)
    }
}
