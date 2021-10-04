package eu.kanade.tachiyomi.ui.base

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.ColorUtils
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.widget.TachiyomiTextInputEditText.Companion.setIncognito
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MiniSearchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SearchView(context, attrs) {

    private var scope: CoroutineScope? = null
    private val searchTextView: SearchAutoComplete? = findViewById(androidx.appcompat.R.id.search_src_text)

    init {
        searchTextView?.setTextAppearance(android.R.style.TextAppearance_Material_Body1)
        val actionColorAlpha =
            ColorUtils.setAlphaComponent(context.getResourceColor(R.attr.actionBarTintColor), 200)
        searchTextView?.setTextColor(actionColorAlpha)
        searchTextView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        searchTextView?.setHintTextColor(actionColorAlpha)

        val clearButton = findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        clearButton?.imageTintList = ColorStateList.valueOf(context.getResourceColor(R.attr.actionBarTintColor))

        val searchPlateView = findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchPlateView?.setBackgroundColor(Color.TRANSPARENT)

        setIconifiedByDefault(false)

        val searchMagIconImageView = findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchMagIconImageView?.layoutParams = LinearLayout.LayoutParams(0, 0)
    }

    fun tintBar(color: Int) {
        val clearButton = findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        clearButton?.imageTintList = ColorStateList.valueOf(color)

        val actionColorAlpha =
            ColorUtils.setAlphaComponent(color, 200)
        val searchTextView =
            findViewById<SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        searchTextView?.setHintTextColor(actionColorAlpha)
        searchTextView?.setTextColor(actionColorAlpha)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        searchTextView?.setIncognito(scope!!)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope?.cancel()
        scope = null
    }

    override fun onActionViewExpanded() {
        super.onActionViewExpanded()
        layoutParams?.let {
            val params = it
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams = params
        }
        requestLayout()
    }
}
