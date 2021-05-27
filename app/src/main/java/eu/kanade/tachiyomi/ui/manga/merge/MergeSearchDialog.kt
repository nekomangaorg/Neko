package eu.kanade.tachiyomi.ui.manga.merge

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.jakewharton.rxbinding.widget.itemClicks
import com.jakewharton.rxbinding.widget.textChanges
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.MergeSearchDialogBinding
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsPresenter
import eu.kanade.tachiyomi.util.lang.plusAssign
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.util.concurrent.TimeUnit

class MergeSearchDialog : DialogController {

    private var dialogView: View? = null

    private var adapter: MergeSearchAdapter? = null

    private var selectedItem: SManga? = null

    private var subscriptions = CompositeSubscription()

    private var searchTextSubscription: Subscription? = null

    private lateinit var presenter: MangaDetailsPresenter

    lateinit var binding: MergeSearchDialogBinding

    constructor(detailsController: MangaDetailsController) : super(Bundle()) {
        presenter = detailsController.presenter
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!).apply {
            customView(viewRes = R.layout.merge_search_dialog, scrollable = false, noVerticalPadding = true)
            negativeButton(android.R.string.cancel)
        }

        binding = MergeSearchDialogBinding.bind(dialog.getCustomView())

        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog.window!!.setLayout(width, height)

        if (subscriptions.isUnsubscribed) {
            subscriptions = CompositeSubscription()
        }

        dialogView = dialog.view
        onViewCreated(dialog.view, savedViewState)

        return dialog
    }

    fun onViewCreated(view: View, savedState: Bundle?) {
        // Create adapter
        val adapter = MergeSearchAdapter(view.context)
        this.adapter = adapter


        binding.mergeSearchList.adapter = adapter

        // Set listeners
        selectedItem = null

        subscriptions += binding.mergeSearchList.itemClicks().subscribe { position ->
            MangaItem(position)
        }

        // Do an initial search based on the manga's title
        if (savedState == null) {
            val title = presenter.manga.title
            binding.mergeSearch.append(title)
            search(title)
        }
    }

    private fun MangaItem(position: Int) {
        selectedItem = adapter?.getItem(position)
        dismissDialog()
        presenter.attachMergeManga(selectedItem)
        presenter.refreshAll()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        subscriptions.unsubscribe()
        dialogView = null
        adapter = null
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        searchTextSubscription = binding.mergeSearch.textChanges()
            .skip(1)
            .debounce(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .map { it.toString() }
            .filter(String::isNotBlank)
            .subscribe { search(it) }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        searchTextSubscription?.unsubscribe()
    }

    private fun search(query: String) {
        val view = dialogView ?: return
        binding.progress.visibility = View.VISIBLE
        binding.mergeSearchList.visibility = View.INVISIBLE
        presenter.mergeSearch(query)
    }

    fun onSearchResults(results: List<SManga>) {
        selectedItem = null
        binding.progress.visibility = View.GONE
        if (results.isEmpty()) {
            noResults()
        } else {
            binding.emptyView.visibility = View.INVISIBLE
            binding.mergeSearchList.visibility = View.VISIBLE
            adapter?.setItems(results)
        }
    }

    private fun noResults() {
        binding.progress.visibility = View.VISIBLE
        binding.mergeSearchList.visibility = View.INVISIBLE
        binding.emptyView.showMedium(
            CommunityMaterial.Icon.cmd_compass_off, binding.root.context.getString(R.string.no_results_found)
        )
    }

    fun onSearchResultsError() {
        binding.progress.visibility = View.INVISIBLE
        noResults()
    }

    companion object {
        const val TAG = "merge_manga_id"
    }
}
