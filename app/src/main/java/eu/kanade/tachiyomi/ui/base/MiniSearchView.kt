package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor

class MiniSearchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SearchView(context, attrs) {

    init {
        val searchTextView =
            findViewById<SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        searchTextView?.setTextAppearance(android.R.style.TextAppearance_Material_Body1)
        val actionColorAlpha =
            ColorUtils.setAlphaComponent(context.getResourceColor(R.attr.actionBarTintColor), 200)
        searchTextView?.setTextColor(actionColorAlpha)
        searchTextView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        searchTextView?.setHintTextColor(actionColorAlpha)

        val clearButton = findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        clearButton?.imageTintList = ColorStateList.valueOf(context.getResourceColor(R.attr.actionBarTintColor))
    }
}
