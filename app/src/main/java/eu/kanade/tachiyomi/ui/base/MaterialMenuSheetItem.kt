package eu.kanade.tachiyomi.ui.base

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MenuSheetItemBinding

class MaterialMenuSheetItem(val sheetItem: MaterialMenuSheet.MenuSheetItem) : AbstractItem<MaterialMenuSheetItem.ViewHolder>() {

    /** defines the type defining this item. must be unique. preferably an id */
    override val type: Int = R.id.item_text_view

    /**
     * Returns the layout resource for this item.
     */
    override val layoutRes: Int = R.layout.menu_sheet_item
    override var identifier = sheetItem.id.toLong()

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MaterialMenuSheetItem>(view) {

        private val binding = MenuSheetItemBinding.bind(view)
        override fun bindView(item: MaterialMenuSheetItem, payloads: List<Any>) {
            val sheetItem = item.sheetItem
            with(binding.root) {
                if (sheetItem.text != null) {
                    text = sheetItem.text
                } else {
                    setText(sheetItem.textRes)
                }
                setIcon(sheetItem.drawable)
                if (sheetItem.drawable == 0) {
                    textSize = 14f
                }

                isSelected = this.isSelected
                if (isSelected) {
                    selectWithEndIcon(sheetItem.endDrawableRes)
                }
            }
        }

        override fun unbindView(item: MaterialMenuSheetItem) {
        }
    }
}
