package eu.kanade.tachiyomi.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.PaintDrawable
import android.graphics.drawable.RippleDrawable
import androidx.core.content.res.ResourcesCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import me.saket.cascade.CascadePopupMenu

class RoundedRectDrawable(color: Int, radius: Float) : PaintDrawable(color) {
    init {
        setCornerRadius(radius)
    }
}

fun cascadeMenuStyler(context: Context): CascadePopupMenu.Styler {
    val rippleDrawable = {
        RippleDrawable(
            ColorStateList.valueOf(context.getResourceColor(R.attr.colorAccent)),
            null,
            ColorDrawable(Color.BLACK)
        )
    }

    return CascadePopupMenu.Styler(
        background = {
            RoundedRectDrawable(
                context.getResourceColor(R.attr.colorPrimaryVariant),
                radius = 8f.dpToPx
            )
        },
        menuTitle = {
            it.titleView.typeface =
                ResourcesCompat.getFont(context, R.font.montserrat_regular)
            it.itemView.background = rippleDrawable()
        },
        menuItem = {
            it.titleView.typeface =
                ResourcesCompat.getFont(context, R.font.montserrat_regular)
            it.contentView.background = rippleDrawable()
            it.setGroupDividerColor(context.getResourceColor(R.attr.colorAccent))
        },
    )
}
