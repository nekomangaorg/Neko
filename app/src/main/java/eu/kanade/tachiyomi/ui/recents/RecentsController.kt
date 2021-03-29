package eu.kanade.tachiyomi.ui.recents

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadService
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateService
import eu.kanade.tachiyomi.databinding.RecentsControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.main.BottomSheetController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.recent_updates.RecentChaptersController
import eu.kanade.tachiyomi.ui.recently_read.RecentlyReadController
import eu.kanade.tachiyomi.ui.recently_read.RemoveHistoryDialog
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.spToPx
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.requestPermissionsSafe
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.setStyle
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlin.math.abs
import kotlin.math.max

/**
 * Fragment that shows recently read manga.
 * Uses R.layout.fragment_recently_read.
 * UI related actions should be called from here.
 */
class RecentsController(bundle: Bundle? = null) :
    BaseController<RecentsControllerBinding>(bundle),
    RecentMangaAdapter.RecentsInterface,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    FlexibleAdapter.OnItemMoveListener,
    RootSearchInterface,
    BottomSheetController,
    RemoveHistoryDialog.Listener {

    init {
        setHasOptionsMenu(true)
        retainViewMode = RetainViewMode.RETAIN_DETACH
    }

    /**
     * Adapter containing the recent manga.
     */
    private var adapter = RecentMangaAdapter(this)

    private var presenter = RecentsPresenter(this)
    private var snack: Snackbar? = null
    private var lastChapterId: Long? = null
    private var showingDownloads = false
    var headerHeight = 0

    override fun getTitle(): String? {
        return if (showingDownloads) {
            resources?.getString(R.string.download_queue)
        } else resources?.getString(R.string.recents)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        binding = RecentsControllerBinding.inflate(inflater)
        return binding.root
    }

    /**
     * Called when view is created
     *
     * @param view created view
     */
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        // Initialize adapter
        adapter = RecentMangaAdapter(this)
        binding.recycler.adapter = adapter
        binding.recycler.layoutManager = LinearLayoutManager(view.context)
        binding.recycler.setHasFixedSize(true)
        binding.recycler.recycledViewPool.setMaxRecycledViews(0, 0)
        binding.recycler.addItemDecoration(
            RecentMangaDivider(view.context)
        )
        adapter.isSwipeEnabled = true
        adapter.itemTouchHelperCallback.setSwipeFlags(
            ItemTouchHelper.LEFT
        )
        val attrsArray = intArrayOf(android.R.attr.actionBarSize)
        val array = view.context.obtainStyledAttributes(attrsArray)
        val appBarHeight = array.getDimensionPixelSize(0, 0)
        array.recycle()
        binding.swipeRefresh.setStyle()
        scrollViewWith(
            binding.recycler,
            swipeRefreshLayout = binding.swipeRefresh,
            afterInsets = {
                headerHeight = it.systemWindowInsetTop + appBarHeight
                binding.recycler.updatePaddingRelative(bottom = activityBinding?.bottomNav?.height ?: 0)
            },
            onBottomNavUpdate = {
                setBottomPadding()
            }
        )

        presenter.onCreate()
        if (presenter.recentItems.isNotEmpty()) {
            adapter.updateDataSet(presenter.recentItems)
            if (presenter.viewType > 0) {
                adapter.addScrollableHeader(presenter.generalHeader)
            }
        }

        binding.downloadBottomSheet.dlBottomSheet.onCreate(this)

        binding.shadow2.alpha =
            if (binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) 0.25f else 0f
        binding.shadow.alpha =
            if (binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) 0.5f else 0f

        binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.addBottomSheetCallback(
            object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, progress: Float) {
                    binding.shadow2.alpha = (1 - abs(progress)) * 0.25f
                    binding.shadow.alpha = (1 - abs(progress)) * 0.5f
                    if (progress >= 0) activityBinding?.appBar?.elevation = max(
                        progress * 15f,
                        if (binding.recycler.canScrollVertically(-1)) 15f else 0f
                    )
                    binding.downloadBottomSheet.sheetLayout.alpha = 1 - progress
                    activityBinding?.appBar?.y = max(activityBinding!!.appBar.y, -headerHeight * (1 - progress))
                    val oldShow = showingDownloads
                    showingDownloads = progress > 0.92f
                    if (oldShow != showingDownloads) {
                        setTitle()
                        activity?.invalidateOptionsMenu()
                    }
                }

                override fun onStateChanged(p0: View, state: Int) {
                    if (this@RecentsController.view == null) return
                    if (state == BottomSheetBehavior.STATE_EXPANDED) activityBinding?.appBar?.y = 0f
                    if (state == BottomSheetBehavior.STATE_EXPANDED || state == BottomSheetBehavior.STATE_COLLAPSED) {
                        binding.downloadBottomSheet.sheetLayout.alpha =
                            if (state == BottomSheetBehavior.STATE_COLLAPSED) 1f else 0f
                        showingDownloads = state == BottomSheetBehavior.STATE_EXPANDED
                        setTitle()
                        activity?.invalidateOptionsMenu()
                    }

                    if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                        if (hasQueue()) {
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable = false
                        } else {
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable = true
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    } else if (state == BottomSheetBehavior.STATE_HIDDEN) {
                        if (!hasQueue()) {
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.skipCollapsed = true
                        } else {
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.skipCollapsed = false
                            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }

                    if (state == BottomSheetBehavior.STATE_HIDDEN || state == BottomSheetBehavior.STATE_COLLAPSED) {
                        binding.shadow2.alpha = if (state == BottomSheetBehavior.STATE_COLLAPSED) 0.25f else 0f
                        binding.shadow.alpha = if (state == BottomSheetBehavior.STATE_COLLAPSED) 0.5f else 0f
                    }

                    binding.downloadBottomSheet.sheetLayout.isClickable = state == BottomSheetBehavior.STATE_COLLAPSED
                    binding.downloadBottomSheet.sheetLayout.isFocusable = state == BottomSheetBehavior.STATE_COLLAPSED
                    setPadding(binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == true)
                }
            }
        )
        binding.swipeRefresh.isRefreshing = LibraryUpdateService.isRunning()
        binding.swipeRefresh.setOnRefreshListener {
            if (!LibraryUpdateService.isRunning()) {
                LibraryUpdateService.start(view.context)
            }
        }

        if (showingDownloads) {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.expand()
        }
        setPadding(binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == true)
        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 301)
    }

    fun setBottomPadding() {
        val bottomBar = activityBinding?.bottomNav ?: return
        val pad = bottomBar.translationY - bottomBar.height
        val padding = max(
            (-pad).toInt(),
            if (binding.downloadBottomSheet.dlBottomSheet.sheetBehavior.isExpanded()) 0 else {
                view?.rootWindowInsets?.systemWindowInsetBottom ?: 0
            }
        )
        binding.shadow2.translationY = pad
        binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.peekHeight = 48.spToPx + padding
        binding.downloadBottomSheet.fastScroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = -pad.toInt()
        }
    }

    fun setRefreshing(refresh: Boolean) {
        binding.swipeRefresh.isRefreshing = refresh
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) { }

    override fun shouldMoveItem(fromPosition: Int, toPosition: Int) = true

    override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        binding.swipeRefresh.isEnabled = actionState != ItemTouchHelper.ACTION_STATE_SWIPE ||
            binding.swipeRefresh.isRefreshing
    }

    override fun handleSheetBack(): Boolean {
        if (binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
            binding.downloadBottomSheet.dlBottomSheet.dismiss()
            return true
        }
        return false
    }

    fun setPadding(sheetIsHidden: Boolean) {
        binding.recycler.updatePaddingRelative(bottom = if (sheetIsHidden) 0 else 20.dpToPx)
        binding.recycler.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = if (sheetIsHidden) 0 else 30.dpToPx
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        if (view != null) {
            refresh()
            binding.downloadBottomSheet.dlBottomSheet.update()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        snack?.dismiss()
        presenter.onDestroy()
        snack = null
    }

    fun refresh() = presenter.getRecents()

    fun showLists(recents: List<RecentMangaItem>) {
        if (view == null) return
        binding.swipeRefresh.isRefreshing = LibraryUpdateService.isRunning()
        adapter.updateItems(recents)
        adapter.removeAllScrollableHeaders()
        if (presenter.viewType > 0) {
            adapter.addScrollableHeader(presenter.generalHeader)
        }
        if (lastChapterId != null) {
            refreshItem(lastChapterId ?: 0L)
            lastChapterId = null
        }
    }

    fun updateChapterDownload(download: Download) {
        if (view == null) return
        binding.downloadBottomSheet.dlBottomSheet.update()
        binding.downloadBottomSheet.dlBottomSheet.onUpdateProgress(download)
        binding.downloadBottomSheet.dlBottomSheet.onUpdateDownloadedPages(download)
        val id = download.chapter.id ?: return
        val holder = binding.recycler.findViewHolderForItemId(id) as? RecentMangaHolder ?: return
        holder.notifyStatus(download.status, download.progress)
    }

    private fun refreshItem(chapterId: Long) {
        val recentItemPos = adapter.currentItems.indexOfFirst {
            it is RecentMangaItem &&
                it.mch.chapter.id == chapterId
        }
        if (recentItemPos > -1) adapter.notifyItemChanged(recentItemPos)
    }

    override fun downloadChapter(position: Int) {
        val view = view ?: return
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
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

    override fun onCoverClick(position: Int) {
        val manga = (adapter.getItem(position) as? RecentMangaItem)?.mch?.manga ?: return
        router.pushController(MangaDetailsController(manga).withFadeTransaction())
    }

    override fun showHistory() = router.pushController(RecentlyReadController().withFadeTransaction())
    override fun showUpdates() = router.pushController(RecentChaptersController().withFadeTransaction())

    override fun onItemClick(view: View?, position: Int): Boolean {
        val item = adapter.getItem(position) ?: return false
        if (item is RecentMangaItem) {
            if (item.mch.manga.id == null) {
                val headerItem = adapter.getHeaderOf(item) as? RecentMangaHeaderItem
                val controller: Controller = when (headerItem?.recentsType) {
                    RecentMangaHeaderItem.NEW_CHAPTERS -> RecentChaptersController()
                    RecentMangaHeaderItem.CONTINUE_READING -> RecentlyReadController()
                    else -> return false
                }
                router.pushController(controller.withFadeTransaction())
            } else {
                val activity = activity ?: return false
                val intent = ReaderActivity.newIntent(activity, item.mch.manga, item.chapter)
                startActivity(intent)
            }
        } else if (item is RecentMangaHeaderItem) return false
        return true
    }

    override fun onItemLongClick(position: Int) {
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
        val manga = item.mch.manga
        val history = item.mch.history
        val chapter = item.mch.chapter
        if (history.id != null) {
            RemoveHistoryDialog(this, manga, history, chapter).showDialog(router)
        }
    }

    override fun removeHistory(manga: Manga, history: History, all: Boolean) {
        if (all) {
            // Reset last read of chapter to 0L
            presenter.removeAllFromHistory(manga.id!!)
        } else {
            // Remove all chapters belonging to manga from library
            presenter.removeFromHistory(history)
        }
    }

    override fun markAsRead(position: Int) {
        val item = adapter.getItem(position) as? RecentMangaItem ?: return
        val chapter = item.chapter
        val manga = item.mch.manga
        val lastRead = chapter.last_page_read
        val pagesLeft = chapter.pages_left
        lastChapterId = chapter.id
        presenter.markChapterRead(chapter, true)
        snack = view?.snack(R.string.marked_as_read, Snackbar.LENGTH_INDEFINITE) {
            anchorView = activityBinding?.bottomNav
            var undoing = false
            setAction(R.string.undo) {
                presenter.markChapterRead(chapter, false, lastRead, pagesLeft)
                undoing = true
            }
            addCallback(
                object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (!undoing && presenter.preferences.removeAfterMarkedAsRead()) {
                            lastChapterId = chapter.id
                            presenter.deleteChapter(chapter, manga)
                        }
                    }
                }
            )
        }
        (activity as? MainActivity)?.setUndoSnackBar(snack)
    }

    override fun isSearching() = presenter.query.isNotEmpty()

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (onRoot) (activity as? MainActivity)?.setDismissIcon(showingDownloads)
        if (showingDownloads) {
            inflater.inflate(R.menu.download_queue, menu)
        } else {
            inflater.inflate(R.menu.recents, menu)

            when (presenter.viewType) {
                0 -> menu.findItem(R.id.action_group_all)
                1 -> menu.findItem(R.id.action_ungroup_all)
                2 -> menu.findItem(R.id.action_only_history)
                3 -> menu.findItem(R.id.action_only_updates)
                else -> null
            }?.isChecked = true

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
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (showingDownloads) binding.downloadBottomSheet.dlBottomSheet.prepareMenu(menu)
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            if (type == ControllerChangeType.POP_ENTER) presenter.onCreate()
            binding.downloadBottomSheet.dlBottomSheet.dismiss()
        } else {
            if (type == ControllerChangeType.POP_EXIT) presenter.onDestroy()
            snack?.dismiss()
        }
        setBottomPadding()
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (type == ControllerChangeType.POP_ENTER) setBottomPadding()
    }

    fun hasQueue() = presenter.downloadManager.hasQueue()

    override fun showSheet() {
        if (binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.isHideable == false || hasQueue()) {
            binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.expand()
        }
    }

    override fun toggleSheet() {
        if (showingDownloads) binding.downloadBottomSheet.dlBottomSheet.dismiss()
        else binding.downloadBottomSheet.dlBottomSheet.sheetBehavior?.expand()
    }

    override fun sheetIsExpanded(): Boolean = binding.downloadBottomSheet.dlBottomSheet.sheetBehavior.isExpanded()

    override fun expandSearch() {
        if (showingDownloads) {
            binding.downloadBottomSheet.dlBottomSheet.dismiss()
        } else {
            activityBinding?.toolbar?.menu?.findItem(R.id.action_search)?.expandActionView()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (showingDownloads) {
            return binding.downloadBottomSheet.dlBottomSheet.onOptionsItemSelected(item)
        }
        when (item.itemId) {
            R.id.action_group_all, R.id.action_ungroup_all, R.id.action_only_history,
            R.id.action_only_updates -> {
                presenter.toggleGroupRecents(
                    when (item.itemId) {
                        R.id.action_ungroup_all -> 1
                        R.id.action_only_history -> 2
                        R.id.action_only_updates -> 3
                        else -> 0
                    }
                )
                if (item.itemId == R.id.action_only_history) {
                    activity?.toast(R.string.press_and_hold_to_reset_history, Toast.LENGTH_LONG)
                }
                activity?.invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
