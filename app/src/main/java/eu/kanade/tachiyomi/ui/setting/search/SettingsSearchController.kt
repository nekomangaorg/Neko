package eu.kanade.tachiyomi.ui.setting.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.tachiyomi.ui.base.SmallToolbarInterface
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.main.FloatingSearchInterface
import eu.kanade.tachiyomi.ui.setting.SettingsController
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.liftAppbarWith
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import org.nekomanga.R
import org.nekomanga.databinding.SettingsSearchControllerBinding

/**
 * This controller shows and manages the different search result in settings search.
 * [SettingsSearchAdapter.OnTitleClickListener] called when preference is clicked in settings search
 */
class SettingsSearchController :
    BaseController<SettingsSearchControllerBinding>(),
    FloatingSearchInterface,
    SmallToolbarInterface,
    SettingsSearchAdapter.OnTitleClickListener {

    /** Adapter containing search results grouped by lang. */
    private var adapter: SettingsSearchAdapter? = null
    private var searchView: SearchView? = null
    var query: String = ""

    init {
        setHasOptionsMenu(true)
    }

    override fun createBinding(inflater: LayoutInflater) =
        SettingsSearchControllerBinding.inflate(inflater)

    override fun getTitle(): String = query

    /**
     * Adds items to the options menu.
     *
     * @param menu menu containing options.
     * @param inflater used to load the menu xml.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate menu.
        inflater.inflate(R.menu.settings_main, menu)

        val searchItem = activityBinding?.searchToolbar?.searchItem
        searchView = activityBinding?.searchToolbar?.searchView

        activityBinding
            ?.searchToolbar
            ?.setQueryHint(applicationContext?.getString(R.string.search_settings), false)

        searchItem?.expandActionView()
        setItems(getResultSet())

        searchView?.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    setItems(getResultSet(query))
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (!newText.isNullOrBlank()) {
                        query = newText
                    }
                    setItems(getResultSet(newText))
                    return false
                }
            },
        )

        searchView?.setQuery(query, true)
    }

    override fun onActionViewCollapse(item: MenuItem?) {
        router.popCurrentController()
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = SettingsSearchAdapter(this)

        liftAppbarWith(binding.recycler, true)
        // Create recycler and set adapter.
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.adapter = adapter

        // load all search results
        SettingsSearchHelper.initPreferenceSearchResultCollection(view.context)
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onSaveViewState(view: View, outState: Bundle) {
        super.onSaveViewState(view, outState)
        adapter?.onSaveInstanceState(outState)
    }

    override fun onRestoreViewState(view: View, savedViewState: Bundle) {
        super.onRestoreViewState(view, savedViewState)
        adapter?.onRestoreInstanceState(savedViewState)
    }

    /**
     * returns a list of `SettingsSearchItem` to be shown as search results Future update: should we
     * add a minimum length to the query before displaying results? Consider other languages.
     */
    fun getResultSet(query: String? = null): List<SettingsSearchItem> {
        if (!query.isNullOrBlank()) {
            return SettingsSearchHelper.getFilteredResults(query).map {
                SettingsSearchItem(it, null, query)
            }
        }

        return mutableListOf()
    }

    /**
     * Add search result to adapter.
     *
     * @param searchResult result of search.
     */
    fun setItems(searchResult: List<SettingsSearchItem>) {
        adapter?.updateDataSet(searchResult)
    }

    /** Opens a catalogue with the given search. */
    override fun onTitleClick(ctrl: SettingsController) {
        searchView?.query.let { query = it.toString() }

        router.pushController(ctrl.withFadeTransaction())
    }

    companion object {
        var lastSearch = ""
    }
}
