package eu.kanade.tachiyomi.util.view

import android.content.Context
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.sizeDp

class DrawableHelper {

    companion object{
        fun standardIcon(context: Context, icon : IIcon) : IconicsDrawable{
            return IconicsDrawable(context, icon).apply { sizeDp = 18 }
        }
    }
}