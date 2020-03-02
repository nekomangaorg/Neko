package eu.kanade.tachiyomi.ui.manga

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ActionMode
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
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.ui.catalogue.CatalogueController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersAdapter
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.getText
import eu.kanade.tachiyomi.util.view.snack
import kotlinx.android.synthetic.main.big_manga_controller.*
import kotlinx.android.synthetic.main.main_activity.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaChaptersController : BaseController,
    ActionMode.Callback,
    FlexibleAdapter.OnItemClickListener,
    ChaptersAdapter.MangaHeaderInterface {

    constructor(manga: Manga?,
        fromCatalogue: Boolean = false,
        smartSearchConfig: CatalogueController.SmartSearchConfig? = null,
        update: Boolean = false) : super(Bundle().apply {
        putLong(MangaController.MANGA_EXTRA, manga?.id ?: 0)
        putBoolean(MangaController.FROM_CATALOGUE_EXTRA, fromCatalogue)
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

    /**
     * Adapter containing a list of chapters.
     */
    private var adapter: ChaptersAdapter? = null

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
        //setReadingDrawable()

        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(view.context)
        recycler.addItemDecoration(
            DividerItemDecoration(
                view.context,
                DividerItemDecoration.VERTICAL
            )
        )
        recycler.setHasFixedSize(true)

        presenter.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            recycler.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                val atTop =
                    ((recycler.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0)
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
                    //colorAnimation.startDelay = 150
                    colorAnimator?.addUpdateListener { animator ->
                        (activity as MainActivity).toolbar.setBackgroundColor(animator.animatedValue as Int)
                        //activity?.window?.statusBarColor = (animator.animatedValue as Int)
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
                        (recycler.findViewHolderForItemId(-1) as? MangaHeaderHolder)
                            ?.setBackDrop(backDropColor)
                        if (toolbarIsColored)
                            (activity as MainActivity).toolbar.setBackgroundColor(backDropColor)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) { }
            })
        //adapter?.fastScroller = fast_scroller
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type == ControllerChangeType.PUSH_ENTER || type == ControllerChangeType.POP_ENTER) {
            (activity as MainActivity).appbar.setBackgroundColor(Color.TRANSPARENT)
            (activity as MainActivity).toolbar.setBackgroundColor(Color.TRANSPARENT)
          /*  val colorFrom = ((activity as MainActivity).toolbar.background as ColorDrawable).color
            val colorTo = Color.TRANSPARENT
            colorAnimator = ValueAnimator.ofObject(
                ArgbEvaluator(), colorFrom, colorTo)
            colorAnimator?.duration = 250 // milliseconds
            //colorAnimation.startDelay = 150
            colorAnimator?.addUpdateListener { animator ->
                (activity as MainActivity).toolbar.setBackgroundColor(animator.animatedValue as Int)
                //activity?.window?.statusBarColor = (animator.animatedValue as Int)
            }
            colorAnimator?.start()*/

            /*activity!!.window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val insetTop = activity!!.window.decorView.rootWindowInsets.systemWindowInsetTop
                val insetBottom = activity!!.window.decorView.rootWindowInsets.stableInsetBottom
                (activity)?.appbar?.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    topMargin = insetTop
                }

                (activity)?.navigationView?.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    bottomMargin = insetBottom
                }
            }*/

            //
            //(activity as MainActivity).toolbar.setBackgroundColor(Color.TRANSPARENT)
            //(activity as MainActivity).appbar.gone()
        }
        else if (type == ControllerChangeType.PUSH_EXIT || type == ControllerChangeType.POP_EXIT) {
            colorAnimator?.cancel()

            (activity as MainActivity).toolbar.setBackgroundColor(activity?.getResourceColor(
                android.R.attr.colorPrimary
            ) ?: Color.BLACK)

           // activity!!.window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            activity?.window?.statusBarColor = activity?.getResourceColor(
                android.R.attr.colorPrimary
            ) ?: Color.BLACK
           /*(activity as MainActivity).appbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = 0
            }
            (activity as MainActivity).navigationView.updateLayoutParams<ConstraintLayout
            .LayoutParams> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    bottomMargin = 0
                }
            }*/
            //(activity as MainActivity).appbar.background = null
//            (activity as AppCompatActivity).supportActionBar?.show()
        }
    }


    fun updateChapters(chapters: List<ChapterItem>) {
        if (presenter.chapters.isEmpty()) {
            //initialFetchChapters()
        }
        adapter?.updateDataSet(listOf(ChapterItem(Chapter.create(), manga!!)) + chapters)
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val adapter = adapter ?: return false
        val chapter = adapter.getItem(position)?.chapter ?: return false
        if (!chapter.isRecognizedNumber) return false
        /*if (actionMode != null && adapter.mode == SelectableAdapter.Mode.MULTI) {
            lastClickPosition = position
            toggleSelection(position)
            return true
        } else {*/
            openChapter(chapter)
            return false
        //}
    }

    fun openChapter(chapter: Chapter, hasAnimation: Boolean = false) {
        val activity = activity ?: return
        val intent = ReaderActivity.newIntent(activity, manga!!, chapter)
        if (hasAnimation) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources!!.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources!!.getDimensionPixelSize(resourceId)
        }
        return result
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
        activity?.window?.statusBarColor = color
    }

    override fun coverColor(): Int? = coverColor

    override fun nextChapter(): Chapter? {
        return presenter.getNextUnreadChapter()
    }

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
        if (!chapter.isRecognizedNumber) return
        if (chapter.isDownloaded) {
            presenter.deleteChapters(listOf(chapter))
        }
        else presenter.downloadChapters(listOf(chapter))
    }
}