package eu.kanade.tachiyomi.widget.preference

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.core.text.buildSpannedString
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.checkItem
import com.afollestad.materialdialogs.list.uncheckItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.widget.materialdialogs.QuadStateCheckBox
import eu.kanade.tachiyomi.widget.materialdialogs.listItemsQuadStateMultiChoice

class TriStateListPreference @JvmOverloads constructor(
    activity: Activity?,
    context: Context,
    attrs: AttributeSet? =
        null
) :
    ListMatPreference(activity, context, attrs) {

    var allSelectionRes: Int? = null
    var excludeKey: String? = null

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

    private var defValue: Set<String> = emptySet()

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        defValue = (defaultValue as? Collection<*>).orEmpty().mapNotNull { it as? String }.toSet()
    }

    override var customSummaryProvider: SummaryProvider<MatPreference>? = SummaryProvider<MatPreference> {
        var includedStrings = prefs.getStringSet(key, defValue).getOrDefault().mapNotNull { value ->
            entryValues.indexOf(value).takeUnless { it == -1 }
        }.toIntArray().sorted().map { entries[it] }
        allSelectionRes?.let { allRes ->
            when {
                includedStrings.isEmpty() -> includedStrings = listOf(context.getString(allRes))
                allIsAlwaysSelected && !showAllLast ->
                    includedStrings =
                        listOf(context.getString(allRes)) + includedStrings
                allIsAlwaysSelected -> includedStrings = includedStrings + context.getString(allRes)
            }
        }
        val excludedStrings = excludeKey?.let {
            prefs.getStringSet(it, defValue).getOrDefault().mapNotNull { value ->
                entryValues.indexOf(value).takeUnless {
                    it == -1
                }
            }
        }?.toIntArray()?.sorted()?.map { entries[it] }?.takeIf { it.isNotEmpty() }
            ?: listOf(context.getString(R.string.none))
        buildSpannedString {
            append(context.getString(R.string.include_, includedStrings.joinToString()))
            appendLine()
            append(context.getString(R.string.exclude_, excludedStrings.joinToString()))
        }
    }

    @SuppressLint("CheckResult")
    override fun MaterialDialog.setItems() {
        val set = prefs.getStringSet(key, defValue).getOrDefault()
        val items = if (allSelectionRes != null) {
            if (showAllLast) entries + listOf(context.getString(allSelectionRes!!))
            else listOf(context.getString(allSelectionRes!!)) + entries
        } else entries
        val allPos = if (showAllLast) items.size - 1 else 0
        val excludedSet = excludeKey?.let {
            prefs.getStringSet(it, defValue).getOrDefault()
        }.orEmpty()
        val allValue = intArrayOf(
            if (set.isEmpty()) QuadStateCheckBox.State.CHECKED.ordinal
            else QuadStateCheckBox.State.UNCHECKED.ordinal
        )
        val preselected =
            if (allSelectionRes != null && !showAllLast) { allValue } else { intArrayOf() } + entryValues
                .map {
                    when (it) {
                        in set -> QuadStateCheckBox.State.CHECKED.ordinal
                        in excludedSet -> QuadStateCheckBox.State.INVERSED.ordinal
                        else -> QuadStateCheckBox.State.UNCHECKED.ordinal
                    }
                }
                .toIntArray() +
                if (allSelectionRes != null && showAllLast) { allValue } else { intArrayOf() }
        var includedItems = set
        var excludedItems = excludedSet
        positiveButton(android.R.string.ok) {
            prefs.getStringSet(key, emptySet()).set(includedItems)
            excludeKey?.let { prefs.getStringSet(it, emptySet()).set(excludedItems) }
            callChangeListener(includedItems to excludedItems)
            notifyChanged()
        }
        listItemsQuadStateMultiChoice(
            items = items,
            disabledIndices = if (allSelectionRes != null) intArrayOf(allPos) else null,
            initialSelection = preselected
        ) { _, sels, _ ->
            val selections = sels.filterIndexed { index, i -> allSelectionRes == null || index != allPos }
            includedItems = selections
                .mapIndexed { index, value -> if (value == QuadStateCheckBox.State.CHECKED.ordinal) index else null }
                .filterNotNull()
                .map { entryValues[it] }
                .toSet()
            excludedItems = selections
                .mapIndexed { index, value -> if (value == QuadStateCheckBox.State.INVERSED.ordinal) index else null }
                .filterNotNull()
                .map { entryValues[it] }
                .toSet()

            if (allSelectionRes != null && !allIsAlwaysSelected) {
                if (includedItems.isEmpty()) checkItem(allPos)
                else uncheckItem(allPos)
            }
        }
    }
}
