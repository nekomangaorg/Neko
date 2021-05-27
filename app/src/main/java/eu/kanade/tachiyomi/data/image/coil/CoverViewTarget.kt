package eu.kanade.tachiyomi.data.image.coil

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.target.ImageViewTarget
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.util.system.iconicsDrawableLarge

class CoverViewTarget(
    view: ImageView,
    val progress: View? = null,
    val errorUrl: String? = null,
    val scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
) : ImageViewTarget(view) {

    override fun onError(error: Drawable?) {
        progress?.isVisible = false
        if (errorUrl == null) {
            view.scaleType = ImageView.ScaleType.CENTER
            view.setImageDrawable(view.context.iconicsDrawableLarge(MaterialDesignDx.Icon.gmf_broken_image, color = android.R.attr.textColorSecondary))
        } else {
            val request = ImageRequest.Builder(view.context).data(errorUrl).memoryCachePolicy(CachePolicy.ENABLED)
                .target(CoverViewTarget(view, progress)).build()
            Coil.imageLoader(view.context).enqueue(request)
        }
    }

    override fun onStart(placeholder: Drawable?) {
        progress?.isVisible = true
        view.scaleType = scaleType
        super.onStart(placeholder)
    }

    override fun onSuccess(result: Drawable) {
        progress?.isVisible = false
        view.scaleType = scaleType
        super.onSuccess(result)
    }
}
