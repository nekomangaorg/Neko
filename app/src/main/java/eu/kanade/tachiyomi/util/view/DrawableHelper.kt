package eu.kanade.tachiyomi.util.view

import android.content.Context
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.sizeDp

class DrawableHelper {

    companion object {

        fun standardIcon32(context: Context, icon: IIcon): IconicsDrawable = icon(context, icon, 32)
        fun standardIcon24(context: Context, icon: IIcon) = icon(context, icon, 24)
        fun standardIcon20(context: Context, icon: IIcon) = icon(context, icon, 20)
        fun standardIcon18(context: Context, icon: IIcon) = icon(context, icon, 18)

        private fun icon(context: Context, icon: IIcon, dp: Int) = IconicsDrawable(context, icon).apply { sizeDp = dp }
    }
}
