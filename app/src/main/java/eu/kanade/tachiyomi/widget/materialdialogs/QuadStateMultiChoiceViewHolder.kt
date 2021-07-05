package eu.kanade.tachiyomi.widget.materialdialogs

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.R

internal class QuadStateMultiChoiceViewHolder(
    itemView: View,
    private val adapter: QuadStateMultiChoiceDialogAdapter
) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
    init {
        itemView.setOnClickListener(this)
    }

    val controlView: QuadStateCheckBox = itemView.findViewById(R.id.md_quad_state_control)
    val titleView: TextView = itemView.findViewById(R.id.md_quad_state_title)

    var isEnabled: Boolean
        get() = itemView.isEnabled
        set(value) {
            itemView.isEnabled = value
            controlView.isEnabled = value
            titleView.isEnabled = value
            titleView.alpha = if (value) 1f else 0.75f
            controlView.alpha = if (value) 1f else 0.75f
        }

    override fun onClick(view: View) = adapter.itemClicked(bindingAdapterPosition)
}
