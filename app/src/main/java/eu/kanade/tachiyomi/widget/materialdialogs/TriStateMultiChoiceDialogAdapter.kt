package eu.kanade.tachiyomi.widget.materialdialogs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.databinding.ListitemTristatechoiceBinding
import eu.kanade.tachiyomi.widget.TriStateCheckBox

private object CheckPayload
private object InverseCheckPayload
private object UncheckPayload

internal typealias TriStateMultiChoiceListener = (
    adapter: TriStateMultiChoiceDialogAdapter,
    indices: IntArray,
    items: List<CharSequence>,
    selectedIndex: Int,
    selectedState: Int,
) -> Unit

internal class TriStateMultiChoiceDialogAdapter(
    private var dialog: MaterialAlertDialogBuilder,
    internal var items: List<CharSequence>,
    disabledItems: IntArray?,
    initialSelection: IntArray,
    private val skipChecked: Boolean = false,
    internal var listener: TriStateMultiChoiceListener?,
) : RecyclerView.Adapter<TriStateMultiChoiceViewHolder>() {

    private val states = TriStateCheckBox.State.values()
    private val defaultOrdinal
        get() = if (skipChecked) {
            TriStateCheckBox.State.IGNORE.ordinal
        } else {
            TriStateCheckBox.State.CHECKED.ordinal
        }

    private var currentSelection: IntArray = initialSelection
        set(value) {
            val previousSelection = field
            field = value
            previousSelection.forEachIndexed { index, previous ->
                val current = value[index]
                when {
                    current == TriStateCheckBox.State.CHECKED.ordinal && previous != TriStateCheckBox.State.CHECKED.ordinal -> {
                        // This value was selected
                        notifyItemChanged(index, CheckPayload)
                    }
                    current == TriStateCheckBox.State.IGNORE.ordinal && previous != TriStateCheckBox.State.IGNORE.ordinal -> {
                        // This value was inverse selected
                        notifyItemChanged(index, InverseCheckPayload)
                    }
                    current == TriStateCheckBox.State.UNCHECKED.ordinal && previous != TriStateCheckBox.State.UNCHECKED.ordinal -> {
                        // This value was unselected
                        notifyItemChanged(index, UncheckPayload)
                    }
                }
            }
        }
    private var disabledIndices: IntArray = disabledItems ?: IntArray(0)

    internal fun itemClicked(index: Int) {
        val newSelection = this.currentSelection.toMutableList()
        newSelection[index] = when (currentSelection[index]) {
            TriStateCheckBox.State.CHECKED.ordinal -> TriStateCheckBox.State.IGNORE.ordinal
            TriStateCheckBox.State.IGNORE.ordinal -> TriStateCheckBox.State.UNCHECKED.ordinal
            // UNCHECKED
            else -> defaultOrdinal
        }
        currentSelection = newSelection.toIntArray()
        val selectedItems = this.items.filterIndexed { i, _ ->
            currentSelection[i] != 0
        }
        listener?.invoke(this, currentSelection, selectedItems, index, newSelection[index])
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): TriStateMultiChoiceViewHolder {
        val listItemView: View = ListitemTristatechoiceBinding
            .inflate(LayoutInflater.from(dialog.context), parent, false).root
        return TriStateMultiChoiceViewHolder(
            itemView = listItemView,
            adapter = this,
        )
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(
        holder: TriStateMultiChoiceViewHolder,
        position: Int,
    ) {
        holder.isEnabled = !disabledIndices.contains(position)

        holder.controlView.state = states.getOrNull(currentSelection[position]) ?: TriStateCheckBox.State.UNCHECKED
        holder.controlView.text = items[position]
//        holder.itemView.background = dialog.getItemSelector()

//        if (dialog.bodyFont != null) {
//            holder.titleView.typeface = dialog.bodyFont
//        }
    }

    override fun onBindViewHolder(
        holder: TriStateMultiChoiceViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        when (payloads.firstOrNull()) {
            CheckPayload -> {
                holder.controlView.setState(TriStateCheckBox.State.CHECKED, true)
                return
            }
            InverseCheckPayload -> {
                holder.controlView.setState(TriStateCheckBox.State.IGNORE, true)
                return
            }
            UncheckPayload -> {
                holder.controlView.setState(TriStateCheckBox.State.UNCHECKED, true)
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    fun checkItems(indices: IntArray) {
        val newSelection = this.currentSelection.toMutableList()
        for (index in indices) {
            newSelection[index] = defaultOrdinal
        }
        this.currentSelection = newSelection.toIntArray()
    }

    fun uncheckItems(indices: IntArray) {
        val newSelection = this.currentSelection.toMutableList()
        for (index in indices) {
            newSelection[index] = TriStateCheckBox.State.UNCHECKED.ordinal
        }
        this.currentSelection = newSelection.toIntArray()
    }
}
