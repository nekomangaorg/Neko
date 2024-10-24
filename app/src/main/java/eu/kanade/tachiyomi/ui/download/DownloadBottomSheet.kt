package eu.kanade.tachiyomi.ui.download

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.recents.RecentsController
import eu.kanade.tachiyomi.util.system.withUIContext
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.doOnApplyWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.hide
import eu.kanade.tachiyomi.util.view.isCollapsed
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.isHidden
import eu.kanade.tachiyomi.util.view.toolbarHeight
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.R
import org.nekomanga.databinding.DownloadBottomSheetBinding
import uy.kohesive.injekt.injectLazy

class DownloadBottomSheet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs),
    DownloadAdapter.DownloadItemListener,
    FlexibleAdapter.OnActionStateListener {
    var controller: RecentsController? = null
    var sheetBehavior: BottomSheetBehavior<*>? = null

    /** Adapter containing the active downloads. */
    private var adapter: DownloadAdapter? = null

    private val presenter = DownloadBottomPresenter()

    val preferences: PreferencesHelper by injectLazy()

    private var activity: Activity? = null

    lateinit var binding: DownloadBottomSheetBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = DownloadBottomSheetBinding.bind(this)
    }

    fun onCreate(controller: RecentsController) {
        // Initialize adapter, scroll listener and recycler views
        presenter.attachView(this)
        presenter.onCreate()
        presenter.queueState
            .onEach { downloads ->
                withUIContext {
                    val emptyQueue = downloads.isEmpty()
                    binding.downloadFab.isInvisible = emptyQueue
                    setInformationView(downloads.firstOrNull(), emptyQueue)
                }
            }
            .launchIn(presenter.presenterScope)

        presenter.downloadManager.isDownloaderRunning
            .onEach { running ->
                withUIContext {
                    if (running) {
                        prepareMenu()
                    }
                    updateFab(running)
                }
            }
            .launchIn(presenter.presenterScope)

        adapter = DownloadAdapter(this)
        sheetBehavior = BottomSheetBehavior.from(this)
        activity = controller.activity
        // Create recycler and set adapter.
        binding.dlRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.dlRecycler.adapter = adapter
        adapter?.isHandleDragEnabled = true
        adapter?.isSwipeEnabled = true
        adapter?.fastScroller = binding.fastScroller
        binding.fastScroller.controller = controller
        binding.dlRecycler.setHasFixedSize(true)
        this.controller = controller

        val headerHeight = (activity as? MainActivity)?.toolbarHeight ?: 0
        binding.recyclerLayout.doOnApplyWindowInsetsCompat { v, windowInsets, _ ->
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin =
                    windowInsets.getInsets(systemBars()).top +
                        (controller.toolbarHeight ?: headerHeight) - binding.sheetLayout.height
            }
        }
        binding.sheetLayout.setOnClickListener {
            if (!sheetBehavior.isExpanded()) {
                sheetBehavior?.expand()
            } else {
                sheetBehavior?.collapse()
            }
        }
        binding.downloadFab.setOnClickListener { presenter.flipDownloads() }
    }

    private fun updateDLTitle(download: Download?) {
        binding.titleText.text =
            if (download != null) {
                resources.getString(
                    R.string.downloading_,
                    download.chapterItem.name,
                )
            } else {
                ""
            }
    }

    /**
     * Called from the presenter to assign the downloads for the adapter.
     *
     * @param downloads the downloads from the queue.
     */
    fun onNextDownloads(downloads: List<DownloadHeaderItem>) {
        adapter?.updateDataSet(downloads)
        if (!preferences.shownDownloadSwipeTutorial().get() && downloads.isNotEmpty()) {
            adapter?.addItem(DownloadSwipeTutorialItem())
        }
    }

    override fun onActionStateChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            controller?.isDragging = true
        }
    }

    /**
     * Called when the progress of a download changes.
     *
     * @param download the download whose progress has changed.
     */
    fun onUpdateProgress(download: Download) {
        getHolder(download)?.notifyProgress()
    }

    /**
     * Called when a page of a download is downloaded.
     *
     * @param download the download whose page has been downloaded.
     */
    fun onUpdateDownloadedPages(download: Download) {
        getHolder(download)?.notifyDownloadedPages()
    }

    /**
     * Returns the holder for the given download.
     *
     * @param download the download to find.
     * @return the holder of the download or null if it's not bound.
     */
    private fun getHolder(download: Download): DownloadHolder? {
        return binding.dlRecycler.findViewHolderForItemId(download.chapterItem.id!!)
            as? DownloadHolder
    }

    /** Set information view when queue is empty */
    private fun setInformationView(download: Download?, queueIsEmpty: Boolean) {
        updateDLTitle(download)
        setBottomSheet(queueIsEmpty)
        when (queueIsEmpty) {
            true ->
                binding.emptyView.show(
                    CommunityMaterial.Icon.cmd_download_off,
                    R.string.nothing_is_downloading,
                )
            false -> binding.emptyView.hide()
        }
    }

    private fun prepareMenu() {
        val menu = binding.sheetToolbar.menu
        // Set clear button visibility.
        menu.findItem(R.id.clear_queue)?.isVisible =
            presenter.downloadManager.queueState.value.isNotEmpty()

        // Set reorder button visibility.
        menu.findItem(R.id.reorder)?.isVisible =
            presenter.downloadManager.queueState.value.isNotEmpty()
    }

    private fun updateFab(isRunning: Boolean) {
        binding.downloadFab.text =
            context.getString(if (isRunning) R.string.pause else R.string.resume)
        binding.downloadFab.setIconResource(
            if (isRunning) R.drawable.ic_pause_24dp else R.drawable.ic_play_arrow_24dp
        )
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        val context = activity ?: return false
        when (item.itemId) {
            R.id.clear_queue -> {
                presenter.clearQueue()
            }
            R.id.newest,
            R.id.oldest -> {
                reorderQueue({ it.download.chapterItem.dateUpload }, item.itemId == R.id.newest)
            }
            R.id.asc,
            R.id.desc -> {
                reorderQueue({ it.download.chapterItem.chapterNumber }, item.itemId == R.id.desc)
            }
        }
        return true
    }

    private fun <R : Comparable<R>> reorderQueue(
        selector: (DownloadItem) -> R,
        reverse: Boolean = false,
    ) {
        val adapter = adapter ?: return
        val newDownloads = mutableListOf<Download>()
        adapter.headerItems.forEach { headerItem ->
            headerItem as DownloadHeaderItem
            headerItem.subItems =
                headerItem.subItems.sortedBy(selector).toMutableList().apply {
                    if (reverse) {
                        reverse()
                    }
                }
            newDownloads.addAll(headerItem.subItems.map { it.download })
        }
        presenter.reorder(newDownloads)
    }

    fun dismiss() {
        if (sheetBehavior?.isHideable == true) {
            sheetBehavior?.hide()
        } else {
            sheetBehavior?.collapse()
        }
    }

    private fun setBottomSheet(queueIsEmpty: Boolean) {
        if (!queueIsEmpty) {
            sheetBehavior?.skipCollapsed = false
            if (sheetBehavior.isHidden()) sheetBehavior?.collapse()
        } else {
            sheetBehavior?.isHideable = true
            sheetBehavior?.skipCollapsed = true
            if (sheetBehavior.isCollapsed()) sheetBehavior?.hide()
        }
        controller?.setPadding(queueIsEmpty)
    }

    /**
     * Called when an item is released from a drag.
     *
     * @param position The position of the released item.
     */
    override fun onItemReleased(position: Int) {
        controller?.isDragging = false
        val adapter = adapter ?: return
        val downloads =
            adapter.headerItems.flatMap { header ->
                adapter.getSectionItems(header).map { item -> (item as DownloadItem).download }
            }
        presenter.reorder(downloads)
    }

    override fun onItemRemoved(position: Int) {
        val item = adapter?.getItem(position)
        if (item is DownloadSwipeTutorialItem) {
            preferences.shownDownloadSwipeTutorial().set(true)
            adapter?.removeItem(position)
            return
        }
        val download = (item as? DownloadItem)?.download ?: return
        presenter.cancelDownload(download)

        adapter?.removeItem(position)
        val adapter = adapter ?: return
        val downloads =
            adapter.headerItems.flatMap { header ->
                adapter.getSectionItems(header).map { item -> (item as DownloadItem).download }
            }
        presenter.reorder(downloads)
        controller?.updateChapterDownload(download, false)
    }

    /**
     * Called when the menu item of a download is pressed
     *
     * @param position The position of the item
     * @param menuItem The menu Item pressed
     */
    override fun onMenuItemClick(position: Int, menuItem: MenuItem) {
        val item = adapter?.getItem(position) ?: return
        if (item is DownloadItem) {
            when (menuItem.itemId) {
                R.id.move_to_top,
                R.id.move_to_bottom -> {
                    val headerItems = adapter?.headerItems ?: return
                    val newDownloads = mutableListOf<Download>()
                    headerItems.forEach { headerItem ->
                        headerItem as DownloadHeaderItem
                        if (headerItem == item.header) {
                            headerItem.removeSubItem(item)
                            if (menuItem.itemId == R.id.move_to_top) {
                                headerItem.addSubItem(0, item)
                            } else {
                                headerItem.addSubItem(item)
                            }
                        }
                        newDownloads.addAll(headerItem.subItems.map { it.download })
                    }
                    presenter.reorder(newDownloads)
                }
                R.id.move_to_top_series -> {
                    val (selectedSeries, otherSeries) =
                        adapter
                            ?.currentItems
                            ?.filterIsInstance<DownloadItem>()
                            ?.map(DownloadItem::download)
                            ?.partition { item.download.mangaItem.id == it.mangaItem.id }
                            ?: Pair(listOf<Download>(), listOf<Download>())
                    presenter.reorder(selectedSeries + otherSeries)
                }
                R.id.cancel_series -> {
                    val allDownloadsForSeries =
                        adapter
                            ?.currentItems
                            ?.filterIsInstance<DownloadItem>()
                            ?.filter { item.download.mangaItem.id == it.download.mangaItem.id }
                            ?.map(DownloadItem::download)
                    if (!allDownloadsForSeries.isNullOrEmpty()) {
                        presenter.cancelDownloads(allDownloadsForSeries)
                    }
                }
            }
        }
    }

    fun onDestroy() {
        presenter.onDestroy()
        binding.fastScroller.controller = null
    }
}
