package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import com.crazylegend.kotlinextensions.themeAttrColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.padding
import com.mikepenz.iconics.utils.size
import eu.kanade.tachiyomi.R

fun IIcon.create(
    context: Context,
    iconSize: Float = 18f,
    @AttrRes colorAttr: Int = R.attr.colorAccent,
): IconicsDrawable {
    return IconicsDrawable(context, this).apply {
        colorInt = context.themeAttrColor(colorAttr)
        size = IconicsSize.dp(iconSize)
    }
}

fun IIcon.createWithColorRes(
    context: Context,
    iconSize: Float = 18f,
    @ColorRes colorRes: Int = R.color.material_on_background_emphasis_medium,
): IconicsDrawable {
    return IconicsDrawable(context, this).apply {
        colorInt = context.getColor(colorRes)
        size = IconicsSize.dp(iconSize)
    }
}

fun IIcon.actionBar(context: Context): IconicsDrawable {
    return create(context, 24f, R.attr.colorOnPrimarySurface).apply {
        padding = IconicsSize.dp(2f)
    }
}
