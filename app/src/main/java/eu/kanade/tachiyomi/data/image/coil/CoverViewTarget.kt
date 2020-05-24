package eu.kanade.tachiyomi.data.image.coil

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import coil.Coil
import coil.request.CachePolicy
import coil.request.LoadRequest
import coil.target.ImageViewTarget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible

class CoverViewTarget(
    view: ImageView,
    val progress: View? = null,
    val errorUrl: String? = null,
    val scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
) : ImageViewTarget(view) {

    override fun onError(error: Drawable?) {
        progress?.gone()
        if (errorUrl == null) {
            view.scaleType = ImageView.ScaleType.CENTER
            val vector = VectorDrawableCompat.create(
                view.context.resources, R.drawable.ic_broken_image_24dp, null
            )
            vector?.setTint(view.context.getResourceColor(android.R.attr.textColorSecondary))
            view.setImageDrawable(vector)
        } else {
            val request = LoadRequest.Builder(view.context).data(errorUrl).memoryCachePolicy(CachePolicy.ENABLED)
                .target(CoverViewTarget(view, progress)).build()
            Coil.imageLoader(view.context).execute(request)
        }
    }

    override fun onStart(placeholder: Drawable?) {
        progress?.visible()
        view.scaleType = scaleType
        super.onStart(placeholder)
    }

    override fun onSuccess(result: Drawable) {
        progress?.gone()
        view.scaleType = scaleType
        super.onSuccess(result)
    }
}
