package eu.kanade.tachiyomi.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.TintTypedArray
import androidx.core.view.ViewCompat
import com.google.android.material.R
import com.google.android.material.internal.ScrimInsetsFrameLayout
import com.google.android.material.textfield.TextInputLayout
import eu.kanade.tachiyomi.util.view.inflate
import kotlin.math.min
import org.nekomanga.R as TR

@Suppress("LeakingThis")
@SuppressLint("PrivateResource", "RestrictedApi")
open class SimpleNavigationView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ScrimInsetsFrameLayout(context, attrs, defStyleAttr) {

    /** Max width of the navigation view. */
    private var maxWidth: Int

    /** Recycler view containing all the items. */
    val recycler = androidx.recyclerview.widget.RecyclerView(context)

    init {
        // Custom attributes
        val a =
            TintTypedArray.obtainStyledAttributes(
                context,
                attrs,
                R.styleable.NavigationView,
                defStyleAttr,
                R.style.Widget_Design_NavigationView,
            )

        ViewCompat.setBackground(
            this,
            a.getDrawable(R.styleable.NavigationView_android_background),
        )

        if (a.hasValue(R.styleable.NavigationView_elevation)) {
            ViewCompat.setElevation(
                this,
                a.getDimensionPixelSize(
                        R.styleable.NavigationView_elevation,
                        0,
                    )
                    .toFloat(),
            )
        }

        @Suppress("DEPRECATION")
        ViewCompat.setFitsSystemWindows(
            this,
            a.getBoolean(R.styleable.NavigationView_android_fitsSystemWindows, false),
        )

        maxWidth = a.getDimensionPixelSize(R.styleable.NavigationView_android_maxWidth, 0)

        a.recycle()

        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        recycler.clipToPadding = false
    }

    /** Overriden to measure the width of the navigation view. */
    @SuppressLint("SwitchIntDef")
    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val width =
            when (MeasureSpec.getMode(widthSpec)) {
                MeasureSpec.AT_MOST ->
                    MeasureSpec.makeMeasureSpec(
                        min(MeasureSpec.getSize(widthSpec), maxWidth),
                        MeasureSpec.EXACTLY,
                    )
                MeasureSpec.UNSPECIFIED ->
                    MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY)
                else -> widthSpec
            }
        // Let super sort out the height
        super.onMeasure(width, heightSpec)
    }

    /** Base view holder. */
    abstract class Holder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view)

    /** Separator view holder. */
    class SeparatorHolder(parent: ViewGroup) :
        Holder(parent.inflate(R.layout.design_navigation_item_separator))

    /** Header view holder. */
    class HeaderHolder(parent: ViewGroup) :
        Holder(parent.inflate(TR.layout.navigation_view_group)) {

        val title: TextView = itemView.findViewById(TR.id.title)
    }

    /** Clickable view holder. */
    abstract class ClickableHolder(view: View, listener: View.OnClickListener?) : Holder(view) {
        init {
            itemView.setOnClickListener(listener)
        }
    }

    /** Radio view holder. */
    class RadioHolder(parent: ViewGroup, listener: View.OnClickListener?) :
        ClickableHolder(parent.inflate(TR.layout.navigation_view_radio), listener) {

        val radio: RadioButton = itemView.findViewById(TR.id.nav_view_item)
    }

    /** Checkbox view holder. */
    class CheckboxHolder(parent: ViewGroup, listener: View.OnClickListener?) :
        ClickableHolder(parent.inflate(TR.layout.navigation_view_checkbox), listener) {

        val check: CheckBox = itemView.findViewById(TR.id.nav_view_item)
    }

    /** Multi state view holder. */
    class MultiStateHolder(parent: ViewGroup, listener: View.OnClickListener?) :
        ClickableHolder(parent.inflate(TR.layout.navigation_view_checkedtext), listener) {

        val text: TriStateCheckBox = itemView.findViewById(TR.id.nav_view_item)
    }

    class SpinnerHolder(parent: ViewGroup, listener: OnClickListener? = null) :
        ClickableHolder(parent.inflate(TR.layout.navigation_view_spinner), listener) {

        val spinnerView: MaterialSpinnerView = itemView.findViewById(TR.id.nav_view_item)
    }

    class EditTextHolder(parent: ViewGroup) :
        Holder(parent.inflate(TR.layout.navigation_view_text)) {

        val wrapper: TextInputLayout = itemView.findViewById(TR.id.nav_view_item_wrapper)
        val edit: EditText = itemView.findViewById(TR.id.nav_view_item)
    }

    protected companion object {
        const val VIEW_TYPE_HEADER = 100
        const val VIEW_TYPE_SEPARATOR = 101
        const val VIEW_TYPE_RADIO = 102
        const val VIEW_TYPE_CHECKBOX = 103
        const val VIEW_TYPE_MULTISTATE = 104
        const val VIEW_TYPE_TEXT = 105
        const val VIEW_TYPE_LIST = 106
    }
}
