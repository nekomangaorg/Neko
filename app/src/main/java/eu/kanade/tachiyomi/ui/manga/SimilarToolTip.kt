package eu.kanade.tachiyomi.ui.manga

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import com.github.florent37.viewtooltip.ViewTooltip
import com.mikepenz.iconics.view.IconicsImageView
import eu.kanade.tachiyomi.R

class SimilarToolTip(activity: Activity, context: Context, similarButton: IconicsImageView) {
    init {
        ViewTooltip.on(activity, similarButton).autoHide(true, 3000).align(ViewTooltip.ALIGN.CENTER)
            .position(ViewTooltip.Position.BOTTOM).text(R.string.tap_to_show_similar_manga)
            .color(context.getColor(R.color.neko_green))
            .textSize(TypedValue.COMPLEX_UNIT_SP, 15f).textColor(Color.BLACK).show()
    }
}