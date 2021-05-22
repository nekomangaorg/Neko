package eu.kanade.tachiyomi.ui.source.global_search

import android.view.View
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.SourceGlobalSearchControllerCardBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.migration.SearchController
import eu.kanade.tachiyomi.util.system.LocaleHelper

/**
 * Holder that binds the [GlobalSearchItem] containing catalogue cards.
 *
 * @param view view of [GlobalSearchItem]
 * @param adapter instance of [GlobalSearchAdapter]
 */
class GlobalSearchHolder(view: View, val adapter: GlobalSearchAdapter) :
    BaseFlexibleViewHolder(view, adapter) {

    /**
     * Adapter containing manga from search results.
     */
    private val mangaAdapter = GlobalSearchCardAdapter(adapter.controller)

    private var lastBoundResults: List<GlobalSearchMangaItem>? = null

    private val binding = SourceGlobalSearchControllerCardBinding.bind(view)

    init {
        // Set layout horizontal.
        binding.recycler.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(view.context, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        binding.recycler.adapter = mangaAdapter

        binding.titleMoreIcon.isVisible = adapter.controller !is SearchController && adapter.controller.extensionFilter == null
        if (binding.titleMoreIcon.isVisible) {
            binding.titleWrapper.setOnClickListener {
                adapter.getItem(bindingAdapterPosition)?.let {
                    adapter.titleClickListener.onTitleClick(it.source)
                }
            }
        }
    }

    /**
     * Show the loading of source search result.
     *
     * @param item item of card.
     */
    fun bind(item: GlobalSearchItem) {
        val source = item.source
        val results = item.results

        val titlePrefix = if (item.highlighted) "▶" else ""
        val langSuffix = if (source.lang.isNotEmpty()) " (${source.lang})" else ""

        // Set Title with country code if available.
        binding.title.text = titlePrefix + source.name + langSuffix
        binding.subtitle.isVisible = source !is LocalSource
        binding.subtitle.text = LocaleHelper.getDisplayName(source.lang)

        when {
            results == null -> {
                binding.progress.isVisible = true
                showHolder()
            }
            results.isEmpty() -> {
                binding.progress.isVisible = false
                binding.noResults.isVisible = true
                binding.sourceCard.isVisible = false
            }
            else -> {
                binding.progress.isVisible = false
                showHolder()
            }
        }
        if (results !== lastBoundResults) {
            mangaAdapter.updateDataSet(results)
            lastBoundResults = results
        }
    }

    /**
     * Called from the presenter when a manga is initialized.
     *
     * @param manga the initialized manga.
     */
    fun setImage(manga: Manga) {
        getHolder(manga)?.setImage(manga)
    }

    /**
     * Returns the view holder for the given manga.
     *
     * @param manga the manga to find.
     * @return the holder of the manga or null if it's not bound.
     */
    private fun getHolder(manga: Manga): GlobalSearchMangaHolder? {
        mangaAdapter.allBoundViewHolders.forEach { holder ->
            val item = mangaAdapter.getItem(holder.flexibleAdapterPosition)
            if (item != null && item.manga.id!! == manga.id!!) {
                return holder as GlobalSearchMangaHolder
            }
        }

        return null
    }

    private fun showHolder() {
        binding.sourceCard.isVisible = true
        binding.noResults.isVisible = false
    }
}
