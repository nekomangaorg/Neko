package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.util.AttributeSet
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.checkItem
import com.afollestad.materialdialogs.list.isItemChecked
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.uncheckItem
import eu.kanade.tachiyomi.data.preference.getOrDefault

class MultiListMatPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? =
    null) :
    ListMatPreference(context, attrs) {

    var allSelectionRes:Int? = null
    var customSummaryRes:Int
        get() = 0
        set(value) { customSummary = context.getString(value) }
    var customSummary:String? = null
    var positions:IntArray? = null

    override fun getSummary(): CharSequence {
        return customSummary ?: super.getSummary()
    }

    override fun dialog():MaterialDialog {
        return MaterialDialog(context).apply {
            title(text = title.toString())
            negativeButton(android.R.string.cancel)
            positiveButton(android.R.string.ok) {
                var value = positions?.map {
                    entryValues[it - if (allSelectionRes != null) 1 else 0] }?.toSet() ?: emptySet()
                if (allSelectionRes != null && isItemChecked(0)) value = emptySet()
                prefs.getStringSet(key, emptySet()).set(value)
                callChangeListener(value)
                this@MultiListMatPreference.summary = this@MultiListMatPreference.summary
            }
            val set = prefs.getStringSet(key, emptySet()).getOrDefault()
            var default = set.map {
                entryValues.indexOf(it) + if (allSelectionRes != null) 1 else 0 }
                .toIntArray()
            if (allSelectionRes != null && default.isEmpty()) default = intArrayOf(0)
            val items = if (allSelectionRes != null)
                (listOf(context.getString(allSelectionRes!!)) + entries) else entries
            listItemsMultiChoice(items = items,
                allowEmptySelection = true,
                disabledIndices = if (allSelectionRes != null) intArrayOf(0) else null,
                waitForPositiveButton = false,
                initialSelection = default) { _, pos, _ ->
                positions = pos
                if (allSelectionRes != null) {
                    if (pos.isEmpty()) checkItem(0)
                    else uncheckItem(0)
                }
                callChangeListener(positions)
            }
        }
    }
}