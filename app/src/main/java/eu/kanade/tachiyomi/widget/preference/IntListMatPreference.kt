package eu.kanade.tachiyomi.widget.preference

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class IntListMatPreference @JvmOverloads constructor(
    activity: Activity?,
    context: Context,
    attrs:
        AttributeSet? =
            null,
) :
    MatPreference(activity, context, attrs) {
    var entryValues: List<Int> = emptyList()
    var entryRange: IntRange
        get() = 0..0
        set(value) { entryValues = value.toList() }
    var entriesRes: Array<Int>
        get() = emptyArray()
        set(value) { entries = value.map { context.getString(it) } }
    private var defValue: Int = 0
    var entries: List<String> = emptyList()
    var customSelectedValue: Int? = null

    override var customSummaryProvider: SummaryProvider<MatPreference>? = SummaryProvider<MatPreference> {
        val index = entryValues.indexOf(prefs.getInt(key, defValue).get())
        if (entries.isEmpty() || index == -1) ""
        else entries[index]
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        defValue = defaultValue as? Int ?: defValue
    }

    override fun dialog(): MaterialAlertDialogBuilder {
        return super.dialog().apply {
            val default = entryValues.indexOf(customSelectedValue ?: prefs.getInt(key, defValue).get())
            setSingleChoiceItems(entries.toTypedArray(), default) { dialog, pos ->
                val value = entryValues[pos]
                if (key != null) {
                    prefs.getInt(key, defValue).set(value)
                }
                callChangeListener(value)
                notifyChanged()
                dialog.dismiss()
            }
        }
    }
}
