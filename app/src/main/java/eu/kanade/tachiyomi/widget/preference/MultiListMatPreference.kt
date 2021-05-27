package eu.kanade.tachiyomi.widget.preference

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference.SummaryProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.checkItem
import com.afollestad.materialdialogs.list.isItemChecked
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import com.afollestad.materialdialogs.list.uncheckItem
import eu.kanade.tachiyomi.data.preference.getOrDefault

class MultiListMatPreference @JvmOverloads constructor(
    activity: Activity?,
    context: Context,
    attrs: AttributeSet? =
        null
) :
    ListMatPreference(activity, context, attrs) {

    var allSelectionRes: Int? = null

    /** All item is always selected and uncheckabele */
    var allIsAlwaysSelected = false
        set(value) {
            field = value
            notifyChanged()
        }

    /** All Item is moved to bottom of list if true */
    var showAllLast = false
        set(value) {
            field = value
            notifyChanged()
        }

    var defValue: Set<String> = emptySet()

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        defValue = (defaultValue as? Collection<*>).orEmpty().mapNotNull { it as? String }.toSet()
    }

    override var customSummaryProvider: SummaryProvider<MatPreference>? = SummaryProvider<MatPreference> {
        var values = prefs.getStringSet(key, defValue).getOrDefault().mapNotNull { value ->
            entryValues.indexOf(value).takeUnless { it == -1 }
        }.toIntArray().sorted().map { entries[it] }
        allSelectionRes?.let { allRes ->
            when {
                values.isEmpty() -> values = listOf(context.getString(allRes))
                allIsAlwaysSelected && !showAllLast ->
                    values =
                        listOf(context.getString(allRes)) + values
                allIsAlwaysSelected -> values = values + context.getString(allRes)
            }
        }
        values.joinToString()
    }

    @SuppressLint("CheckResult")
    override fun MaterialDialog.setItems() {
        val set = prefs.getStringSet(key, defValue).getOrDefault()
        var default = set.mapNotNull {
            if (entryValues.indexOf(it) == -1) null
            else entryValues.indexOf(it) + if (allSelectionRes != null && !showAllLast) 1 else 0
        }
            .toIntArray()
        val items = if (allSelectionRes != null) {
            if (showAllLast) entries + listOf(context.getString(allSelectionRes!!))
            else listOf(context.getString(allSelectionRes!!)) + entries
        } else entries
        val allPos = if (showAllLast) items.size - 1 else 0
        if (allSelectionRes != null && default.isEmpty()) default = intArrayOf(allPos)
        else if (allSelectionRes != null && allIsAlwaysSelected) default += allPos
        positiveButton(android.R.string.ok) {
            val pos = mutableListOf<Int>()
            for (i in items.indices)
                if (!(allSelectionRes != null && i == allPos) && isItemChecked(i)) pos.add(i)
            var value = pos.mapNotNull {
                entryValues.getOrNull(it - if (allSelectionRes != null && !showAllLast) 1 else 0)
            }.toSet()
            if (allSelectionRes != null && !allIsAlwaysSelected && isItemChecked(0)) value = emptySet()
            prefs.getStringSet(key, emptySet()).set(value)
            callChangeListener(value)
            notifyChanged()
        }
        listItemsMultiChoice(
            items = items,
            allowEmptySelection = true,
            disabledIndices = if (allSelectionRes != null) intArrayOf(allPos) else null,
            waitForPositiveButton = false,
            initialSelection = default
        ) { _, pos, _ ->
            if (allSelectionRes != null && !allIsAlwaysSelected) {
                if (pos.isEmpty()) checkItem(allPos)
                else uncheckItem(allPos)
            }
        }
    }
}
