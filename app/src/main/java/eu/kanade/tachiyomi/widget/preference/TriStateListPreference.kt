package eu.kanade.tachiyomi.widget.preference

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.core.text.buildSpannedString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.setTriStateItems
import eu.kanade.tachiyomi.widget.TriStateCheckBox

class TriStateListPreference @JvmOverloads constructor(
    activity: Activity?,
    context: Context,
    attrs: AttributeSet? =
        null,
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
        var includedStrings = prefs.getStringSet(key, defValue).get().mapNotNull { value ->
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
            prefs.getStringSet(it, defValue).get().mapNotNull { value ->
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
    override fun MaterialAlertDialogBuilder.setListItems() {
        val set = prefs.getStringSet(key, defValue).get()
        val items = if (allSelectionRes != null) {
            if (showAllLast) entries + listOf(context.getString(allSelectionRes!!))
            else listOf(context.getString(allSelectionRes!!)) + entries
        } else entries
        val allPos = if (showAllLast) items.size - 1 else 0
        val excludedSet = excludeKey?.let {
            prefs.getStringSet(it, defValue).get()
        }.orEmpty()
        val allValue = intArrayOf(
            if (set.isEmpty()) TriStateCheckBox.State.CHECKED.ordinal
            else TriStateCheckBox.State.UNCHECKED.ordinal,
        )
        val preselected =
            if (allSelectionRes != null && !showAllLast) { allValue } else { intArrayOf() } + entryValues
                .map {
                    when (it) {
                        in set -> TriStateCheckBox.State.CHECKED.ordinal
                        in excludedSet -> TriStateCheckBox.State.IGNORE.ordinal
                        else -> TriStateCheckBox.State.UNCHECKED.ordinal
                    }
                }
                .toIntArray() +
                if (allSelectionRes != null && showAllLast) { allValue } else { intArrayOf() }
        var includedItems = set
        var excludedItems = excludedSet
        setPositiveButton(android.R.string.ok) { _, _ ->
            prefs.getStringSet(key, emptySet()).set(includedItems)
            excludeKey?.let { prefs.getStringSet(it, emptySet()).set(excludedItems) }
            callChangeListener(includedItems to excludedItems)
            notifyChanged()
        }
        setTriStateItems(
            items = items,
            disabledIndices = if (allSelectionRes != null) intArrayOf(allPos) else null,
            initialSelection = preselected,
        ) { adapter, sels, _, _, _ ->
            val selections = sels.filterIndexed { index, _ -> allSelectionRes == null || index != allPos }
            includedItems = selections
                .mapIndexed { index, value -> if (value == TriStateCheckBox.State.CHECKED.ordinal) index else null }
                .filterNotNull()
                .map { entryValues[it] }
                .toSet()
            excludedItems = selections
                .mapIndexed { index, value -> if (value == TriStateCheckBox.State.IGNORE.ordinal) index else null }
                .filterNotNull()
                .map { entryValues[it] }
                .toSet()

            if (allSelectionRes != null && !allIsAlwaysSelected) {
                if (includedItems.isEmpty()) adapter.checkItems(intArrayOf(allPos))
                else adapter.uncheckItems(intArrayOf(allPos))
            }
        }
    }
}
