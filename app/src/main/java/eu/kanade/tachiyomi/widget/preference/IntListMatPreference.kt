package eu.kanade.tachiyomi.widget.preference

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.setting.defaultValue
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class IntListMatPreference @JvmOverloads constructor(activity: Activity?, context: Context,
    attrs:
AttributeSet? =
    null) :
    MatPreference(activity, context, attrs) {
    var entryValues:List<Int> = emptyList()
    var entryRange:IntRange
        get() = 0..0
        set(value) { entryValues = value.toList() }
    var entriesRes:Array<Int>
        get() = emptyArray()
        set(value) { entries = value.map { context.getString(it) } }
    private var defValue:Int = 0
    var entries:List<String> = emptyList()

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        defValue = defaultValue as? Int ?: defValue
    }
    override fun getSummary(): CharSequence {
        val index = entryValues.indexOf(prefs.getInt(key, defValue).getOrDefault())
        return if (entries.isEmpty() || index == -1) ""
        else entries[index]
    }

    override fun dialog(): MaterialDialog {
        return super.dialog().apply {
            val default = entryValues.indexOf(prefs.getInt(key, defValue).getOrDefault())
            listItemsSingleChoice(items = entries,
                waitForPositiveButton = false,
                initialSelection = default) {
                    _, pos, _ ->
                val value = entryValues[pos]
                prefs.getInt(key, defValue).set(value)
                callChangeListener(value)
                this@IntListMatPreference.summary = this@IntListMatPreference.summary
                dismiss()
            }
        }
    }
}