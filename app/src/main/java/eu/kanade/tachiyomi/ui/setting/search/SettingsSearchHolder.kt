package eu.kanade.tachiyomi.ui.setting.search

import android.text.Html
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import eu.davidea.viewholders.FlexibleViewHolder
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.lang.highlightText
import kotlinx.android.synthetic.main.settings_search_controller_card.view.*
import kotlin.reflect.full.createInstance

/**
 * Holder that binds the [SettingsSearchItem] containing catalogue cards.
 *
 * @param view view of [SettingsSearchItem]
 * @param adapter instance of [SettingsSearchAdapter]
 */
class SettingsSearchHolder(view: View, val adapter: SettingsSearchAdapter) :
    FlexibleViewHolder(view, adapter) {


    init {
        view.title_wrapper.setOnClickListener {
            adapter.getItem(bindingAdapterPosition)?.let {
                val ctrl = it.settingsSearchResult.searchController::class.createInstance()
                ctrl.preferenceKey = it.settingsSearchResult.key

                // must pass a new Controller instance to avoid this error https://github.com/bluelinelabs/Conductor/issues/446
                adapter.titleClickListener.onTitleClick(ctrl)
            }
        }
    }

    /**
     * Show the loading of source search result.
     *
     * @param item item of card.
     */
    fun bind(item: SettingsSearchItem) {
        val color = ColorUtils.setAlphaComponent(ContextCompat.getColor(itemView.context, R.color.colorAccent), 75)
        itemView.search_result_pref_title.text = item.settingsSearchResult.title.highlightText(item.searchResult, color)
        itemView.search_result_pref_summary.text = item.settingsSearchResult.summary.highlightText(item.searchResult, color)
        itemView.search_result_pref_breadcrumb.text = item.settingsSearchResult.breadcrumb.highlightText(item.searchResult, color)
    }
}
