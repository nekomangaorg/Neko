package eu.kanade.tachiyomi.ui.reader.settings

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.annotation.ArrayRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.get
import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.R
import kotlinx.android.synthetic.main.reader_preference.view.*


class ReaderSpinnerView @JvmOverloads constructor(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {


    private var entries = emptyList<String>()
    private var selectedPosition = 0
    private var pref: Preference<Int>? = null
    private var prefOffset = 0
    var onItemSelectedListener: ((Int) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                val popup = makeSettingsPopup()
                setOnTouchListener(popup.dragToOpenListener)
                setOnClickListener {
                    popup.show()
                }
            }
        }

    init {
        inflate(context, R.layout.reader_preference, this)
        val a = context.obtainStyledAttributes(attrs, R.styleable.ReaderPreferenceView, 0, 0)

        val str = a.getString(R.styleable.ReaderPreferenceView_title) ?: ""
        title_view.text = str

        val entries = (a.getTextArray(R.styleable.ReaderPreferenceView_android_entries) ?: emptyArray()).map { it.toString() }
        this.entries = entries

        detail_view.text = entries.firstOrNull().orEmpty()

        a.recycle()
    }

    fun setSelection(selection: Int) {
        selectedPosition = selection
        detail_view.text = entries.getOrNull(selection).orEmpty()
    }

    fun bindToPreference(pref: Preference<Int>, offset: Int = 0, block: ((Int) -> Unit)? = null) {
        setSelection(pref.get() - offset)
        this.pref = pref
        prefOffset = offset
        val popup = makeSettingsPopup(pref, prefOffset, block)
        setOnTouchListener(popup.dragToOpenListener)
        setOnClickListener {
            popup.show()
        }
    }

    fun bindToIntPreference(pref: Preference<Int>, @ArrayRes intValuesResource: Int, block: ((Int) -> Unit)? = null) {
        setSelection(pref.get())
        this.pref = pref
        prefOffset = 0
        val intValues = resources.getStringArray(intValuesResource).map { it.toIntOrNull() }
        val popup = makeSettingsPopup(pref, intValues, block)
        setOnTouchListener(popup.dragToOpenListener)
        setOnClickListener {
            popup.show()
        }
    }

    private fun makeSettingsPopup(preference: Preference<Int>, intValues: List<Int?>, block: ((Int) -> Unit)? = null): PopupMenu {
        val popup = popup()
        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            val pos = popup.menuClicked(menuItem)
            preference.set(intValues[pos] ?: 0)
            block?.invoke(pos)
            true
        }
        return popup
    }

    private fun makeSettingsPopup(preference: Preference<Int>, offset: Int = 0, block: ((Int) -> Unit)? = null): PopupMenu {
        val popup = popup()
        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            val pos = popup.menuClicked(menuItem)
            preference.set(pos + offset)
            block?.invoke(pos)
            true
        }
        return popup
    }

    private fun makeSettingsPopup(): PopupMenu {
        val popup = popup()

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            val pos = popup.menuClicked(menuItem)
            onItemSelectedListener?.invoke(pos)
            true
        }
        return popup
    }

    private fun PopupMenu.menuClicked(menuItem: MenuItem): Int {
        val pos = menuItem.itemId
        menu[selectedPosition].isCheckable = false
        menu[selectedPosition].isChecked = false
        setSelection(pos)
        menu[pos].isCheckable = true
        menu[pos].isChecked = true
        return pos
    }

    private fun popup(): PopupMenu {
        val popup = PopupMenu(context, this, Gravity.END)
        entries.forEachIndexed {  index, entry ->
            popup.menu.add(0, index, 0, entry)
        }
        popup.menu[selectedPosition].isCheckable = true
        popup.menu[selectedPosition].isChecked = true
        return popup
    }
}