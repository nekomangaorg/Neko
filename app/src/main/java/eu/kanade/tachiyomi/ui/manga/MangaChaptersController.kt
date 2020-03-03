package eu.kanade.tachiyomi.ui.manga

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.catalogue.CatalogueController
import eu.kanade.tachiyomi.ui.library.ChangeMangaCategoriesDialog
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.manga.MangaController.Companion.FROM_CATALOGUE_EXTRA
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterMatHolder
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersAdapter
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.doOnApplyWindowInsets
import eu.kanade.tachiyomi.util.view.getText
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import kotlinx.android.synthetic.main.big_manga_controller.*
import kotlinx.android.synthetic.main.big_manga_controller.swipe_refresh
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.manga_info_controller.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaChaptersController : BaseController,
    ActionMode.Callback,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    ChaptersAdapter.MangaHeaderInterface,
    ChangeMangaCategoriesDialog.Listener {

    constructor(manga: Manga?,
        fromCatalogue: Boolean = false,
        smartSearchConfig: CatalogueController.SmartSearchConfig? = null,
        update: Boolean = false) : super(Bundle().apply {
        putLong(MangaController.MANGA_EXTRA, manga?.id ?: 0)
        putBoolean(FROM_CATALOGUE_EXTRA, fromCatalogue)
        putParcelable(MangaController.SMART_SEARCH_CONFIG_EXTRA, smartSearchConfig)
        putBoolean(MangaController.UPDATE_EXTRA, update)
    }) {
        this.manga = manga
        if (manga != null) {
            source = Injekt.get<SourceManager>().getOrStub(manga.source)
        }
    }

    constructor(mangaId: Long) : this(
        Injekt.get<DatabaseHelper>().getManga(mangaId).executeAsBlocking())

    constructor(bundle: Bundle) : this(bundle.getLong(MangaController.MANGA_EXTRA)) {
        val notificationId = bundle.getInt("notificationId", -1)
        val context = applicationContext ?: return
        if (notificationId > -1) NotificationReceiver.dismissNotification(
            context, notificationId, bundle.getInt("groupId", 0)
        )
    }

    private var manga: Manga? = null
    private var source: Source? = null
    var colorAnimator:ValueAnimator? = null
    lateinit var presenter:MangaPresenter
    var coverColor:Int? = null
    var toolbarIsColored = false
    private var snack: Snackbar? = null
    private val fromCatalogue = args.getBoolean(FROM_CATALOGUE_EXTRA, false)
    /**
     * Adapter containing a list of chapters.
     */
    private var adapter: ChaptersAdapter? = null

    var headerHeight = 0

    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return if (toolbarIsColored) manga?.currentTitle() else null
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        coverColor = null
        if (!::presenter.isInitialized) presenter = MangaPresenter(this, manga!!, source!!)

        // Init RecyclerView and adapter
        adapter = ChaptersAdapter(this, view.context)

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(view.context)
        recycler.addItemDecoration(
            DividerItemDecoration(
                view.context,
                DividerItemDecoration.VERTICAL
            )
        )
        recycler.setHasFixedSize(true)
        adapter?.fastScroller = fast_scroller
        val attrsArray = intArrayOf(android.R.attr.actionBarSize)
        val array = view.context.obtainStyledAttributes(attrsArray)
        val appbarHeight = array.getDimensionPixelSize(0, 0)
        array.recycle()
        val offset = 20.dpToPx

        recycler.doOnApplyWindowInsets { v, insets, _ ->
            headerHeight = appbarHeight + insets.systemWindowInsetTop + offset
            swipe_refresh.setProgressViewOffset(false, (-40).dpToPx, headerHeight)
            (recycler.findViewHolderForAdapterPosition(0) as? MangaHeaderHolder)
                ?.setTopHeight(headerHeight)
            fast_scroller?.updateLayoutParams<ViewGroup.MarginLayoutParams>  {
                topMargin = appbarHeight + insets.systemWindowInsetTop
                bottomMargin = insets.systemWindowInsetBottom
            }
            // offset the recycler by the fab's inset + some inset on top
            v.updatePaddingRelative(bottom = insets.systemWindowInsetBottom)
        }

        presenter.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recycler.setOnScrollChangeListener { _, _, _, _, _ ->
                val atTop = !recycler.canScrollVertically(-1)
                if ((!atTop && !toolbarIsColored) || (atTop && toolbarIsColored)) {
                    toolbarIsColored = !atTop
                    colorAnimator?.cancel()
                    val color =
                        coverColor ?: activity!!.getResourceColor(android.R.attr.colorPrimary)
                    val colorFrom = ColorUtils.setAlphaComponent(
                        color, if (toolbarIsColored) 0 else 255
                    )
                    val colorTo = ColorUtils.setAlphaComponent(
                        color, if (toolbarIsColored) 255 else 0
                    )
                    colorAnimator = ValueAnimator.ofObject(
                        ArgbEvaluator(), colorFrom, colorTo
                    )
                    colorAnimator?.duration = 250 // milliseconds
                    colorAnimator?.addUpdateListener { animator ->
                        (activity as MainActivity).toolbar.setBackgroundColor(animator.animatedValue as Int)
                        activity?.window?.statusBarColor = (animator.animatedValue as Int)
                    }
                    colorAnimator?.start()
                    val isCurrentController = router?.backstack?.lastOrNull()?.controller() == this
                    if (isCurrentController) setTitle()
                }
            }
        }
        GlideApp.with(view.context).load(manga)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(manga!!.id!!).toString()))
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    Palette.from(
                        (resource as BitmapDrawable).bitmap).generate {
                        if (recycler == null) return@generate
                        val currentNightMode =
                            recycler.resources!!.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                        val colorBack = view.context.getResourceColor(
                            android.R.attr.colorBackground
                        )
                        val backDropColor =
                            (if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) it?.getLightMutedColor(
                                colorBack
                            )
                            else it?.getDarkMutedColor(colorBack)) ?: colorBack
                        onCoverLoaded(backDropColor)
                        (recycler.findViewHolderForAdapterPosition(0) as? MangaHeaderHolder)
                            ?.setBackDrop(backDropColor)
                        if (toolbarIsColored) {
                            (activity as MainActivity).toolbar.setBackgroundColor(backDropColor)
                            activity?.window?.statusBarColor = backDropColor
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) { }
            })

        swipe_refresh.setOnRefreshListener {
            presenter.refreshAll()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        presenter.fetchChapters()
    }

    fun showError(message: String) {
        swipe_refresh?.isRefreshing = false
        view?.snack(message)
    }

    fun updateChapterDownload(download: Download) {
        getHolder(download.chapter)?.notifyStatus(download.status, presenter.isLockedFromSearch,
            download.progress)
    }

    private fun getHolder(chapter: Chapter): ChapterMatHolder? {
        return recycler?.findViewHolderForItemId(chapter.id!!) as? ChapterMatHolder
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type == ControllerChangeType.PUSH_ENTER || type == ControllerChangeType.POP_ENTER) {
            (activity as MainActivity).appbar.setBackgroundColor(Color.TRANSPARENT)
            (activity as MainActivity).toolbar.setBackgroundColor(Color.TRANSPARENT)
            activity?.window?.statusBarColor = Color.TRANSPARENT
        }
        else if (type == ControllerChangeType.PUSH_EXIT || type == ControllerChangeType.POP_EXIT) {
            colorAnimator?.cancel()

            (activity as MainActivity).toolbar.setBackgroundColor(activity?.getResourceColor(
                android.R.attr.colorPrimary
            ) ?: Color.BLACK)

            activity?.window?.statusBarColor = activity?.getResourceColor(
                android.R.attr.colorPrimary
            ) ?: Color.BLACK
        }
    }

    fun setRefresh(enabled: Boolean) {
        swipe_refresh.isRefreshing = enabled
    }

    fun updateHeader() {
        if (presenter.chapters.isEmpty()) {
            adapter?.updateDataSet(listOf(ChapterItem(Chapter.createH(), presenter.manga)))
        }
        else {
            swipe_refresh?.isRefreshing = false
            adapter?.updateDataSet(
                listOf(ChapterItem(Chapter.createH(), presenter.manga)) + presenter.chapters
            )
        }
    }


    fun updateChapters(chapters: List<ChapterItem>) {
        swipe_refresh?.isRefreshing = false
        if (presenter.chapters.isEmpty() && fromCatalogue && !presenter.hasRequested) {
            launchUI { swipe_refresh?.isRefreshing = true }
            presenter.fetchChaptersFromSource()
        }
        adapter?.updateDataSet(listOf(ChapterItem(Chapter.createH(), presenter.manga)) + chapters)
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val adapter = adapter ?: return false
        val chapter = adapter.getItem(position)?.chapter ?: return false
        if (chapter.isHeader) return false
        /*if (actionMode != null && adapter.mode == SelectableAdapter.Mode.MULTI) {
            lastClickPosition = position
            toggleSelection(position)
            return true
        } else {*/
            openChapter(chapter)
            return false
        //}
    }

    override fun onItemLongClick(position: Int) {
        val adapter = adapter ?: return
        val item = adapter.getItem(position) ?: return
        val itemView = getHolder(item)?.itemView ?: return
        val popup = PopupMenu(itemView.context, itemView, Gravity.END)

        // Inflate our menu resource into the PopupMenu's Menu
        popup.menuInflater.inflate(R.menu.chapters_mat_single, popup.menu)

        // Hide bookmark if bookmark
        popup.menu.findItem(R.id.action_bookmark).isVisible = !item.bookmark
        popup.menu.findItem(R.id.action_remove_bookmark).isVisible = item.bookmark

        // Hide mark as unread when the chapter is unread
        if (!item.read && item.last_page_read == 0) {
            popup.menu.findItem(R.id.action_mark_as_unread).isVisible = false
        }

        // Hide mark as read when the chapter is read
        if (item.read) {
            popup.menu.findItem(R.id.action_mark_as_read).isVisible = false
        }

        // Set a listener so we are notified if a menu item is clicked
        popup.setOnMenuItemClickListener { menuItem ->
            val chapters = listOf(item)
            when (menuItem.itemId) {
                R.id.action_bookmark -> bookmarkChapters(chapters, true)
                R.id.action_remove_bookmark -> bookmarkChapters(chapters, false)
                R.id.action_mark_as_read -> markAsRead(chapters)
                R.id.action_mark_as_unread -> markAsUnread(chapters)
            }
            true
        }

        // Finally show the PopupMenu
        popup.show()
    }

    private fun bookmarkChapters(chapters: List<ChapterItem>, bookmarked: Boolean) {
        //destroyActionModeIfNeeded()
        presenter.bookmarkChapters(chapters, bookmarked)
    }

    private fun markAsRead(chapters: List<ChapterItem>) {
        presenter.markChaptersRead(chapters, true)
        if (presenter.preferences.removeAfterMarkedAsRead()) {
            presenter.deleteChapters(chapters)
        }
    }

    private fun markAsUnread(chapters: List<ChapterItem>) {
        presenter.markChaptersRead(chapters, false)
    }

    private fun openChapter(chapter: Chapter, hasAnimation: Boolean = false) {
        val activity = activity ?: return
        val intent = ReaderActivity.newIntent(activity, manga!!, chapter)
        if (hasAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    override fun onDestroyView(view: View) {
        snack?.dismiss()
        presenter.onDestroy()
        super.onDestroyView(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.chapters, menu)
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.big_manga_controller, container, false)
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true

    }

    override fun onDestroyActionMode(mode: ActionMode?) {

    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true

    }

    fun onCoverLoaded(color: Int) {
        if (view == null) return
        coverColor = color
        //activity?.window?.statusBarColor = color
    }

    override fun coverColor(): Int? = coverColor
    override fun topCoverHeight(): Int = headerHeight

    override fun nextChapter(): Chapter? = presenter.getNextUnreadChapter()
    override fun mangaSource(): Source = presenter.source

    override fun readNextChapter() {
        if (activity is SearchActivity && presenter.isLockedFromSearch) {
            SecureActivityDelegate.promptLockIfNeeded(activity)
            return
        }
        val item = presenter.getNextUnreadChapter()
        if (item != null) {
                openChapter(item.chapter)
        } else if (snack == null || snack?.getText() != view?.context?.getString(
                R.string.no_next_chapter)) {
            snack = view?.snack(R.string.no_next_chapter, Snackbar.LENGTH_LONG) {
                addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        if (snack == transientBottomBar) snack = null
                    }
                })
            }
        }
    }

    override fun downloadChapter(position: Int) {
        val adapter = adapter ?: return
        val chapter = adapter.getItem(position) ?: return
        if (chapter.isHeader) return
        if (chapter.status != Download.NOT_DOWNLOADED) {
            presenter.deleteChapters(listOf(chapter))
        }
        else presenter.downloadChapters(listOf(chapter))
    }

    override fun tagClicked(text: String) {
        val firstController = router.backstack.first()?.controller()
        if (firstController is LibraryController && router.backstack.size == 2) {
            router.handleBack()
            firstController.search(text)
        }
    }

    override fun showChapterFilter() {
        ChaptersSortBottomSheet(this).show()
    }

    override fun chapterCount():Int = presenter.chapters.size

    override fun favoriteManga(longPress: Boolean) {
        val manga = presenter.manga
        if (longPress) {
            if (!manga.favorite) {
                presenter.toggleFavorite()
                showAddedSnack()
            }
            val categories = presenter.getCategories()
            if (categories.isEmpty()) {
                // no categories exist, display a message about adding categories
                snack = view?.snack(R.string.action_add_category)
            } else {
                val ids = presenter.getMangaCategoryIds(manga)
                val preselected = ids.mapNotNull { id ->
                    categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
                }.toTypedArray()

                ChangeMangaCategoriesDialog(this, listOf(manga), categories, preselected)
                    .showDialog(router)
            }
        }
        else {
            if (presenter.toggleFavorite()) {
                val categories = presenter.getCategories()
                val defaultCategoryId = presenter.preferences.defaultCategory()
                val defaultCategory = categories.find { it.id == defaultCategoryId }
                when {
                    defaultCategory != null -> presenter.moveMangaToCategory(manga, defaultCategory)
                    defaultCategoryId == 0 || categories.isEmpty() -> // 'Default' or no category
                        presenter.moveMangaToCategory(manga, null)
                    else -> {
                        val ids = presenter.getMangaCategoryIds(manga)
                        val preselected = ids.mapNotNull { id ->
                            categories.indexOfFirst { it.id == id }.takeIf { it != -1 }
                        }.toTypedArray()

                        ChangeMangaCategoriesDialog(
                            this,
                            listOf(manga),
                            categories,
                            preselected
                        ).showDialog(router)
                    }
                }
                showAddedSnack()
            } else {
                showRemovedSnack()
            }
        }
    }

    private fun showAddedSnack() {
        val view = view ?: return
        snack?.dismiss()
        snack = view.snack(view.context.getString(R.string.manga_added_library))
    }

    private fun showRemovedSnack() {
        val view = view ?: return
        snack?.dismiss()
        snack = view.snack(
            view.context.getString(R.string.manga_removed_library),
            Snackbar.LENGTH_INDEFINITE
        ) {
            setAction(R.string.action_undo) {
                presenter.setFavorite(true)
            }
            addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (!presenter.manga.favorite) presenter.confirmDeletion()
                }
            })
        }
        (activity as? MainActivity)?.setUndoSnackBar(snack, fab_favorite)
    }

    override fun updateCategoriesForMangas(mangas: List<Manga>, categories: List<Category>) {
        val manga = mangas.firstOrNull() ?: return
        presenter.moveMangaToCategories(manga, categories)
    }
}