package eu.kanade.tachiyomi.widget.materialdialogs

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.widget.TriStateCheckBox

internal class TriStateMultiChoiceViewHolder(
    itemView: View,
    private val adapter: TriStateMultiChoiceDialogAdapter,
) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    val controlView: TriStateCheckBox = itemView.findViewById(R.id.md_tri_state_checkbox)

    init {
        itemView.setOnClickListener(this)
        controlView.isClickable = false
        controlView.isFocusable = false
        controlView.setCheckboxBackground(null)
    }

    var isEnabled: Boolean
        get() = itemView.isEnabled
        set(value) {
            itemView.isEnabled = value
            controlView.isEnabled = value
        }

    override fun onClick(view: View) = adapter.itemClicked(bindingAdapterPosition)
}
