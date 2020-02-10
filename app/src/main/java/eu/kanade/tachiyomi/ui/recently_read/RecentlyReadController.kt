package eu.kanade.tachiyomi.ui.recently_read

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.backup.BackupRestoreService
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.catalogue.browse.ProgressItem
import eu.kanade.tachiyomi.ui.manga.MangaController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import kotlinx.android.synthetic.main.recently_read_controller.*

/**
 * Fragment that shows recently read manga.
 * Uses R.layout.fragment_recently_read.
 * UI related actions should be called from here.
 */
class RecentlyReadController(bundle: Bundle? = null) : BaseController(bundle),
        FlexibleAdapter.OnUpdateListener,
        FlexibleAdapter.EndlessScrollListener,
        RecentlyReadAdapter.OnRemoveClickListener,
        RecentlyReadAdapter.OnResumeClickListener,
        RecentlyReadAdapter.OnCoverClickListener,
        RemoveHistoryDialog.Listener {

    init {
        setHasOptionsMenu(true)
    }
    /**
     * Adapter containing the recent manga.
     */
    var adapter: RecentlyReadAdapter? = null
        private set

    /**
     * Endless loading item.
     */
    private var progressItem: ProgressItem? = null
    private var observeLater:Boolean = false
    private var query = ""

    private var presenter = RecentlyReadPresenter(this)
    private var recentItems: MutableList<RecentlyReadItem>? = null


    override fun getTitle(): String? {
        return resources?.getString(R.string.label_recent_manga)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.recently_read_controller, container, false)
    }

    /**
     * Called when view is created
     *
     * @param view created view
     */
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        // Initialize adapter
        adapter = RecentlyReadAdapter(this)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(view.context)
        recycler.setHasFixedSize(true)
        recycler.setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
        resetProgressItem()

        if (recentItems != null)
            adapter?.updateDataSet(recentItems!!.toList())

        launchUI {
            val manga = presenter.refresh(query)
            recentItems = manga.toMutableList()
            adapter?.updateDataSet(manga)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (observeLater) {
            presenter.observe()
            observeLater = false
        }
    }
    /**
     * Populate adapter with chapters
     *
     * @param mangaHistory list of manga history
     */
    fun onNextManga(mangaHistory: List<RecentlyReadItem>) {
        val adapter = adapter ?: return
        adapter.updateDataSet(mangaHistory)
        adapter.onLoadMoreComplete(null)
        if (recentItems == null)
            resetProgressItem()
        recentItems = mangaHistory.toMutableList()
    }

    fun onAddPageError() {
        adapter?.onLoadMoreComplete(null)
        adapter?.endlessTargetCount = 1
    }

    override fun onUpdateEmptyView(size: Int) {
        if (size > 0) {
            empty_view?.hide()
        } else {
            empty_view.show(R.drawable.ic_glasses_black_128dp, R.string.information_no_recent_manga)
        }
    }

    /**
     * Sets a new progress item and reenables the scroll listener.
     */
    private fun resetProgressItem() {
        progressItem = ProgressItem()
        adapter?.endlessTargetCount = 0
        adapter?.setEndlessScrollListener(this, progressItem!!)
    }

    override fun onLoadMore(lastPosition: Int, currentPage: Int) {
        val view = view ?: return
        if (BackupRestoreService.isRunning(view.context.applicationContext)) {
            onAddPageError()
            return
        }
        presenter.requestNext(query)
    }

    override fun noMoreLoad(newItemsSize: Int) { }

    override fun onResumeClick(position: Int) {
        val activity = activity ?: return
        observeLater = true
        val (manga, chapter, _) = (adapter?.getItem(position) as? RecentlyReadItem)?.mch ?: return

        val nextChapter = presenter.getNextChapter(chapter, manga)
        if (nextChapter != null) {
            val intent = ReaderActivity.newIntent(activity, manga, nextChapter)
            startActivity(intent)
        } else {
            activity.toast(R.string.no_next_chapter)
        }
    }

    override fun onRemoveClick(position: Int) {
        val (manga, _, history) = (adapter?.getItem(position) as? RecentlyReadItem)?.mch ?: return
        RemoveHistoryDialog(this, manga, history).showDialog(router)
    }

    override fun onCoverClick(position: Int, lastTouchY: Float) {
        val manga = (adapter?.getItem(position) as? RecentlyReadItem)?.mch?.manga ?: return
        router.pushController(MangaController(manga, lastTouchY).withFadeTransaction())
    }

    override fun removeHistory(manga: Manga, history: History, all: Boolean) {
        presenter.lastCount = adapter?.itemCount ?: 25
        if (all) {
            // Reset last read of chapter to 0L
            presenter.removeAllFromHistory(manga.id!!)
        } else {
            // Remove all chapters belonging to manga from library
            presenter.removeFromHistory(history)
        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.recently_read, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        if (query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(query, true)
            searchView.clearFocus()
        }
        setOnQueryTextChangeListener(searchView) {
            if (query != it) {
                query = it ?: return@setOnQueryTextChangeListener false
                launchUI {
                    resetProgressItem()
                    presenter.lastCount = 25
                    val manga = presenter.refresh(query)
                    recentItems = manga.toMutableList()
                    adapter?.updateDataSet(manga)
                }
            }
            true
        }

        // Fixes problem with the overflow icon showing up in lieu of search
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                activity?.invalidateOptionsMenu()
                return true
            }
        })
    }
}
