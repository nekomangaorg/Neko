package eu.kanade.tachiyomi.ui.category.addtolibrary

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.widget.TriStateCheckBox
import org.nekomanga.R
import org.nekomanga.databinding.AddCategoryItemBinding

class AddCategoryItem(val category: Category) :
    AbstractItem<FastAdapter.ViewHolder<AddCategoryItem>>() {

    /** defines the type defining this item. must be unique. preferably an id */
    override val type: Int = R.id.category_checkbox

    /** defines the layout which will be used for this item in the list */
    override val layoutRes: Int = R.layout.add_category_item

    override var identifier = category.id?.toLong() ?: -1L

    var state: TriStateCheckBox.State = TriStateCheckBox.State.UNCHECKED
        set(value) {
            field = value
            isSelected = value != TriStateCheckBox.State.UNCHECKED
        }

    var skipInversed = false

    override fun getViewHolder(v: View): FastAdapter.ViewHolder<AddCategoryItem> {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<AddCategoryItem>(view) {

        val binding = AddCategoryItemBinding.bind(view)

        init {
            binding.categoryCheckbox.useIndeterminateForIgnore = true
        }

        override fun bindView(item: AddCategoryItem, payloads: List<Any>) {
            binding.categoryCheckbox.skipInversed = item.skipInversed
            binding.categoryCheckbox.text = item.category.name
            binding.categoryCheckbox.state = item.state
        }

        override fun unbindView(item: AddCategoryItem) {
            binding.categoryCheckbox.text = ""
            binding.categoryCheckbox.isChecked = false
        }
    }
}
