package eu.kanade.tachiyomi.ui.manga.track

import android.app.Dialog
import android.os.Bundle
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.jakewharton.rxbinding.widget.itemClicks
import com.jakewharton.rxbinding.widget.textChanges
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.databinding.TrackSearchDialogBinding
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.manga.MangaDetailsPresenter
import eu.kanade.tachiyomi.util.lang.plusAssign
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class TrackSearchDialog : DialogController {

    private var adapter: TrackSearchAdapter? = null

    private var selectedItem: Track? = null

    private val service: TrackService

    private var subscriptions = CompositeSubscription()

    private var searchTextSubscription: Subscription? = null

    private lateinit var bottomSheet: TrackingBottomSheet

    private var wasPreviouslyTracked: Boolean = false

    private lateinit var presenter: MangaDetailsPresenter
    lateinit var binding: TrackSearchDialogBinding

    constructor(target: TrackingBottomSheet, service: TrackService, wasTracked: Boolean) : super(
        Bundle()
            .apply {
                putInt(KEY_SERVICE, service.id)
            }
    ) {
        wasPreviouslyTracked = wasTracked
        bottomSheet = target
        presenter = target.presenter
        this.service = service
    }

    @Suppress("unused")
    constructor(bundle: Bundle) : super(bundle) {
        service = Injekt.get<TrackManager>().getService(bundle.getInt(KEY_SERVICE))!!
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val dialog = MaterialDialog(activity!!).apply {
            customView(viewRes = R.layout.track_search_dialog, scrollable = false)
            negativeButton(android.R.string.cancel)
        }

        binding = TrackSearchDialogBinding.bind(dialog.getCustomView())
        if (subscriptions.isUnsubscribed) {
            subscriptions = CompositeSubscription()
        }
        onViewCreated(savedViewState)

        return dialog
    }

    fun onViewCreated(savedState: Bundle?) {
        // Create adapter
        val adapter = TrackSearchAdapter(binding.root.context)
        this.adapter = adapter
        binding.trackSearchList.adapter = adapter

        // Set listeners
        selectedItem = null

        subscriptions += binding.trackSearchList.itemClicks().subscribe { position ->
            trackItem(position)
        }

        // Do an initial search based on the manga's title
        if (savedState == null) {
            val title = presenter.manga.title
            binding.trackSearch.append(title)
            search(title)
        }
    }

    private fun trackItem(position: Int) {
        selectedItem = adapter?.getItem(position)
        bottomSheet.refreshTrack(service)
        presenter.registerTracking(selectedItem, service)
        dismissDialog()
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        subscriptions.unsubscribe()
        adapter = null
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        searchTextSubscription = binding.trackSearch.textChanges()
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
        binding.progress.visibility = View.VISIBLE
        binding.trackSearchList.visibility = View.INVISIBLE
        presenter.trackSearch(query, service, wasPreviouslyTracked)
    }

    fun onSearchResults(results: List<TrackSearch>) {
        selectedItem = null
        binding.progress.visibility = View.INVISIBLE
        binding.trackSearchList.visibility = View.VISIBLE
        adapter?.setItems(results)
        if (results.size == 1 && !wasPreviouslyTracked) {
            trackItem(0)
        }
    }

    fun onSearchResultsError() {
        binding.progress.visibility = View.VISIBLE
        binding.trackSearchList.visibility = View.INVISIBLE
        adapter?.setItems(emptyList())
    }

    private companion object {
        const val KEY_SERVICE = "service_id"
    }
}
