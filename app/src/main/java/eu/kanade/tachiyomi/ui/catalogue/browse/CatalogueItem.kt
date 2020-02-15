package eu.kanade.tachiyomi.ui.catalogue.browse

import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.f2prateek.rx.preferences.Preference
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.widget.AutofitRecyclerView
import kotlinx.android.synthetic.main.catalogue_grid_item.view.*

class CatalogueItem(val manga: Manga, private val catalogueAsList: Preference<Boolean>) :
        AbstractFlexibleItem<CatalogueHolder>() {

    override fun getLayoutRes(): Int {
        return if (catalogueAsList.getOrDefault())
            R.layout.catalogue_list_item
        else
            R.layout.catalogue_grid_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): CatalogueHolder {
        val parent = adapter.recyclerView
        return if (parent is AutofitRecyclerView) {
            view.apply {
                val coverHeight = (parent.itemWidth / 3 * 4f).toInt()
                constraint_layout.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT,
                    WRAP_CONTENT)
                val marginParams = card.layoutParams as ConstraintLayout.LayoutParams
                marginParams.bottomMargin = 10.dpToPx
                card.layoutParams = marginParams
                constraint_layout.minHeight = 0
                cover_thumbnail.adjustViewBounds = false
                cover_thumbnail.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, coverHeight)
            }
            CatalogueGridHolder(view, adapter)
        } else {
            CatalogueListHolder(view, adapter)
        }
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: CatalogueHolder,
                                position: Int,
                                payloads: MutableList<Any?>?) {

        holder.onSetValues(manga)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is CatalogueItem) {
            return manga.id!! == other.manga.id!!
        }
        return false
    }

    override fun hashCode(): Int {
        return manga.id!!.hashCode()
    }

}
