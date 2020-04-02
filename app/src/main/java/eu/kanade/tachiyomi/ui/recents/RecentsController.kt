package eu.kanade.tachiyomi.ui.recents

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.recent_updates.RecentChaptersController
import eu.kanade.tachiyomi.ui.recently_read.RecentlyReadController
import eu.kanade.tachiyomi.util.view.applyWindowInsetsForRootController
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.snack
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.recently_read_controller.*

/**
 * Fragment that shows recently read manga.
 * Uses R.layout.fragment_recently_read.
 * UI related actions should be called from here.
 */
class RecentsController(bundle: Bundle? = null) : BaseController(bundle),
    RecentMangaAdapter.RecentsInterface,
    RecentsAdapter.RecentsInterface,
    FlexibleAdapter.OnItemClickListener,
    RootSearchInterface {

    init {
        setHasOptionsMenu(true)
    }

    /**
     * Adapter containing the recent manga.
     */
    // private val adapter = RecentsAdapter(this)
    private var adapter = RecentMangaAdapter(this)

    private var presenter = RecentsPresenter(this)
    private var recentItems: List<RecentMangaItem>? = null
    private var snack: Snackbar? = null
    private var lastChapterId: Long? = null

    override fun getTitle(): String? {
        return resources?.getString(R.string.short_recents)
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
        view.applyWindowInsetsForRootController(activity!!.bottom_nav)
        // Initialize adapter
        adapter = RecentMangaAdapter(this)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(view.context)
        recycler.setHasFixedSize(true)
        recycler.recycledViewPool.setMaxRecycledViews(0, 0)
        adapter.isSwipeEnabled = true
        /*recycler.addItemDecoration(
            DividerItemDecoration(
                recycler.context, DividerItemDecoration.VERTICAL
            )
        )*/
        adapter.itemTouchHelperCallback.setSwipeFlags(
            ItemTouchHelper.LEFT
        )
        scrollViewWith(recycler, skipFirstSnap = true)

        if (recentItems != null) adapter.updateDataSet(recentItems!!.toList())
        presenter.onCreate()
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (view != null)
            refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        snack?.dismiss()
        presenter.onDestroy()
        snack = null
    }

    fun refresh() = presenter.getRecents()

    fun showLists(recents: List<RecentMangaItem>) {
        recentItems = recents
        adapter.updateDataSet(recents)
        if (lastChapterId != null) {
            refreshItem(lastChapterId ?: 0L)
            lastChapterId = null
        }
        // recycler.removeItemDecorationAt(0)
    }

    fun updateChapterDownload(download: Download) {
        if (view == null) return
        val id = download.chapter.id ?: return
        val holder = recycler.findViewHolderForItemId(id) as? RecentMangaHolder ?: return
        holder.notifyStatus(download.status, download.progress)
        /* (i in 0 until adapter.itemCount) {
            val holder = recycler.findViewHolderForAdapterPosition(i) as? RecentsHolder ?: continue
            if (holder.updateChapterDownload(download)) break
        }*/
    }

    private fun refreshItem(chapterId: Long) {
        val recentItemPos = adapter.currentItems.indexOfFirst {
            it is RecentMangaItem &&
            it.mch.chapter.id == chapterId }
        if (recentItemPos > -1) adapter.notifyItemChanged(recentItemPos)
        /*holder.notifyStatus(download.status, download.progress)
        for (i in 0 until adapter.itemCount) {
            val holder = recycler.findViewHolderForAdapterPosition(i) as? RecentsHolder ?: continue
            holder.refreshChapter(chapterId)
        }*/
    }

    override fun downloadChapter(position: Int) {
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
        downloadChapter(item)
    }

    override fun downloadChapter(item: RecentMangaItem) {
        val view = view ?: return
        val chapter = item.chapter
        val manga = item.mch.manga
        if (item.status != Download.NOT_DOWNLOADED && item.status != Download.ERROR) {
            presenter.deleteChapter(chapter, manga)
        } else {
            if (item.status == Download.ERROR) DownloadService.start(view.context)
            else presenter.downloadChapter(manga, chapter)
        }
    }

    override fun startDownloadNow(position: Int) {
        val chapter = (adapter.getItem(position) as? RecentMangaItem)?.chapter ?: return
        presenter.startDownloadChapterNow(chapter)
    }

    override fun downloadChapterNow(chapter: Chapter) {
        presenter.startDownloadChapterNow(chapter)
    }

    override fun onCoverClick(position: Int) {
        val manga = (adapter.getItem(position) as? RecentMangaItem)?.mch?.manga ?: return
        router.pushController(MangaDetailsController(manga).withFadeTransaction())
    }

    override fun showManga(manga: Manga) = router.pushController(MangaDetailsController(manga).withFadeTransaction())

    override fun onItemClick(view: View?, position: Int): Boolean {
        val item = adapter.getItem(position) ?: return false
        if (item is RecentMangaItem) {
            if (item.mch.manga.id == null) {
                val headerItem = adapter.getHeaderOf(item) as? RecentMangaHeaderItem
                val controller: Controller = when (headerItem?.recentsType) {
                    RecentsItem.NEW_CHAPTERS -> RecentChaptersController()
                    RecentsItem.CONTINUE_READING -> RecentlyReadController()
                    else -> return false
                }
                router.pushController(controller.withFadeTransaction())
            } else resumeManga(item.mch.manga, item.chapter)
        } else if (item is RecentMangaHeaderItem) return false // onHeaderClick(position)
        return true
    }

    override fun resumeManga(manga: Manga, chapter: Chapter) {
        val activity = activity ?: return
        val intent = ReaderActivity.newIntent(activity, manga, chapter)
        startActivity(intent)
    }

    override fun markAsRead(position: Int) {
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
        markAsRead(item.mch.manga, item.chapter)
    }

    override fun markAsRead(manga: Manga, chapter: Chapter) {
        val lastRead = chapter.last_page_read
        val pagesLeft = chapter.pages_left
        lastChapterId = chapter.id
        presenter.markChapterRead(chapter, true)
        snack =
            view?.snack(R.string.marked_as_read, Snackbar.LENGTH_INDEFINITE) {
                anchorView = activity?.bottom_nav
                var undoing = false
                setAction(R.string.action_undo) {
                    presenter.markChapterRead(chapter, false, lastRead, pagesLeft)
                    undoing = true
                }
                addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (!undoing && presenter.preferences.removeAfterMarkedAsRead()) {
                            lastChapterId = chapter.id
                            presenter.deleteChapter(chapter, manga)
                        }
                    }
                })
            }
        (activity as? MainActivity)?.setUndoSnackBar(snack)
    }

    override fun isSearching() = presenter.query.isNotEmpty()

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.recents, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = view?.context?.getString(R.string.search_recents)
        if (presenter.query.isNotEmpty()) {
            searchItem.expandActionView()
            searchView.setQuery(presenter.query, true)
            searchView.clearFocus()
        }
        setOnQueryTextChangeListener(searchView) {
            if (presenter.query != it) {
                presenter.query = it ?: return@setOnQueryTextChangeListener false
                refresh()
            }
            true
        }
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            if (type == ControllerChangeType.POP_EXIT) {
                presenter.onCreate()
            }
            setHasOptionsMenu(true)
        } else {
            snack?.dismiss()
            setHasOptionsMenu(false)
        }
    }

    override fun setCover(manga: Manga, view: ImageView) {
        val activity = activity ?: return
        GlideApp.with(activity).load(manga).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(manga.id!!).toString())).into(view)
    }

    override fun onHeaderClick(position: Int) {
        val recentsType = (adapter.getItem(position) as? RecentMangaHeaderItem)?.recentsType
        ?: return
        val controller: Controller = when (recentsType) {
            RecentsItem.NEW_CHAPTERS -> RecentChaptersController()
            RecentsItem.CONTINUE_READING -> RecentlyReadController()
            else -> return
        }
        router.pushController(controller.withFadeTransaction())
    }

    override fun viewAll(position: Int) {
        /*val recentsType = (adapter.getItem(position) as? RecentsItem)?.recentType ?: return
        val controller: Controller = when (recentsType) {
            RecentsItem.NEW_CHAPTERS -> RecentChaptersController()
            RecentsItem.CONTINUE_READING -> RecentlyReadController()
            else -> return
        }
        router.pushController(controller.withFadeTransaction())*/
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                if (!LibraryUpdateService.isRunning()) {
                    val view = view ?: return true
                    LibraryUpdateService.start(view.context)
                    snack = view.snack(R.string.updating_library) {
                        anchorView = (activity as? MainActivity)?.bottom_nav
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
