package eu.kanade.tachiyomi.util.system

import android.content.Context
import androidx.annotation.AttrRes
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.size
import org.nekomanga.R

fun IIcon.create(
    context: Context,
    iconSize: Float = 18f,
    @AttrRes colorAttr: Int = R.attr.colorAccent,
): IconicsDrawable {
    return IconicsDrawable(context, this).apply {
        colorInt = context.getResourceColor(colorAttr)
        size = IconicsSize.dp(iconSize)
    }
}
