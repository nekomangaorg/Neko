package eu.kanade.tachiyomi.ui.source

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.res.ColorStateList
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.databinding.SourceControllerBinding
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.extension.SettingsExtensionsController
import eu.kanade.tachiyomi.ui.main.BottomSheetController
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.setting.SettingsBrowseController
import eu.kanade.tachiyomi.ui.setting.SettingsSourcesController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.source.global_search.GlobalSearchController
import eu.kanade.tachiyomi.ui.source.latest.LatestUpdatesController
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.spToPx
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.isCollapsed
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.requestPermissionsSafe
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.extensions_bottom_sheet.*
import kotlinx.android.synthetic.main.extensions_bottom_sheet.ext_bottom_sheet
import kotlinx.android.synthetic.main.extensions_bottom_sheet.sheet_layout
import kotlinx.android.synthetic.main.extensions_bottom_sheet.view.*
import kotlinx.android.synthetic.main.filter_bottom_sheet.*
import kotlinx.android.synthetic.main.library_list_controller.*
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.recycler_with_scroller.view.*
import kotlinx.android.synthetic.main.rounded_category_hopper.*
import kotlinx.android.synthetic.main.source_controller.*
import kotlinx.android.synthetic.main.source_controller.shadow2
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.max
import kotlin.math.min

/**
 * This controller shows and manages the different catalogues enabled by the user.
 * This controller should only handle UI actions, IO actions should be done by [SourcePresenter]
 * [SourceAdapter.SourceListener] call function data on browse item click.
 * [SourceAdapter.OnLatestClickListener] call function data on latest item click
 */
class SourceController :
    NucleusController<SourceControllerBinding, SourcePresenter>(),
    FlexibleAdapter.OnItemClickListener,
    SourceAdapter.SourceListener,
    RootSearchInterface,
    BottomSheetController {

    /**
     * Application preferences.
     */
    private val preferences: PreferencesHelper = Injekt.get()

    /**
     * Adapter containing sources.
     */
    private var adapter: SourceAdapter? = null

    var extQuery = ""
        private set

    var headerHeight = 0

    var showingExtensions = false

    var snackbar: Snackbar? = null

    /**
     * Called when controller is initialized.
     */
    init {
        setHasOptionsMenu(true)
    }

    override fun getTitle(): String? {
        return if (showingExtensions) {
            view?.context?.getString(
                when (ext_bottom_sheet.tabs.selectedTabPosition) {
                    0 -> R.string.extensions
                    else -> R.string.source_migration
                }
            )
        } else view?.context?.getString(R.string.sources)
    }

    override fun createPresenter(): SourcePresenter {
        return SourcePresenter()
    }

    /**
     * Initiate the view with [R.layout.source_controller].
     *
     * @param inflater used to load the layout xml.
     * @param container containing parent views.
     * @return inflated view.
     */
    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.source_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        adapter = SourceAdapter(this)

        // Create recycler and set adapter.
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(view.context)
        recycler.adapter = adapter
        adapter?.isSwipeEnabled = true
        // recycler.addItemDecoration(SourceDividerItemDecoration(view.context))
        val attrsArray = intArrayOf(android.R.attr.actionBarSize)
        val array = view.context.obtainStyledAttributes(attrsArray)
        val appBarHeight = array.getDimensionPixelSize(0, 0)
        array.recycle()
        scrollViewWith(
            recycler,
            afterInsets = {
                headerHeight = it.systemWindowInsetTop + appBarHeight
                recycler.updatePaddingRelative(bottom = activity?.bottom_nav?.height ?: 0)
            },
            onBottomNavUpdate = {
                setBottomPadding()
            }
        )

        recycler?.post {
            setBottomSheetTabs(if (ext_bottom_sheet?.sheetBehavior.isCollapsed()) 0f else 1f)
        }

        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 301)
        ext_bottom_sheet.onCreate(this)

        ext_bottom_sheet.sheetBehavior?.addBottomSheetCallback(
            object : BottomSheetBehavior
            .BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, progress: Float) {
                    val recycler = recycler ?: return
                    shadow2?.alpha = (1 - max(0f, progress)) * 0.25f
                    activityBinding?.appBar?.elevation = min(
                        (1f - progress) * 15f,
                        if (recycler.canScrollVertically(-1)) 15f else 0f
                    )
                    activityBinding?.appBar?.y = max(activityBinding!!.appBar.y, -headerHeight * (1 - progress))
                    val oldShow = showingExtensions
                    showingExtensions = progress > 0.92f
                    if (oldShow != showingExtensions) {
                        setTitle()
                        activity?.invalidateOptionsMenu()
                    }
                    setBottomSheetTabs(max(0f, progress))
                }

                override fun onStateChanged(p0: View, state: Int) {
                    val extBottomSheet = ext_bottom_sheet ?: return
                    if (state == BottomSheetBehavior.STATE_EXPANDED) {
                        activityBinding?.appBar?.y = 0f
                    }
                    if (state == BottomSheetBehavior.STATE_EXPANDED ||
                        state == BottomSheetBehavior.STATE_COLLAPSED
                    ) {
                        showingExtensions = state == BottomSheetBehavior.STATE_EXPANDED
                        setTitle()
                        if (state == BottomSheetBehavior.STATE_EXPANDED) {
                            extBottomSheet.fetchOnlineExtensionsIfNeeded()
                        } else extBottomSheet.shouldCallApi = true
                        activity?.invalidateOptionsMenu()
                    }

                    retainViewMode = if (state == BottomSheetBehavior.STATE_EXPANDED) {
                        RetainViewMode.RETAIN_DETACH
                    } else RetainViewMode.RELEASE_DETACH
                    sheet_layout.isClickable = state == BottomSheetBehavior.STATE_COLLAPSED
                    sheet_layout.isFocusable = state == BottomSheetBehavior.STATE_COLLAPSED
                    if (state == BottomSheetBehavior.STATE_COLLAPSED || state == BottomSheetBehavior.STATE_EXPANDED) {
                        setBottomSheetTabs(if (state == BottomSheetBehavior.STATE_COLLAPSED) 0f else 1f)
                    }
                }
            }
        )

        if (showingExtensions) {
            ext_bottom_sheet.sheetBehavior?.expand()
        }
    }

    fun updateTitleAndMenu() {
        setTitle()
        activity?.invalidateOptionsMenu()
    }

    fun setBottomSheetTabs(progress: Float) {
        val bottomSheet = ext_bottom_sheet ?: return
        ext_bottom_sheet.tabs.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = ((activityBinding?.appBar?.height?.minus(9f.dpToPx) ?: 0f) * progress).toInt()
        }
        val selectedColor = ColorUtils.setAlphaComponent(
            ContextCompat.getColor(ext_bottom_sheet.tabs.context, R.color.colorAccent),
            (progress * 255).toInt()
        )
        val unselectedColor = ColorUtils.setAlphaComponent(
            bottomSheet.context.getResourceColor(R.attr.colorOnBackground),
            153
        )
        ext_bottom_sheet.sheet_layout.elevation = progress * 5
        ext_bottom_sheet.pager.alpha = progress * 10
        ext_bottom_sheet.tabs.setSelectedTabIndicatorColor(selectedColor)
        ext_bottom_sheet.tabs.setTabTextColors(
            ColorUtils.blendARGB(
                bottomSheet.context.getResourceColor(R.attr.actionBarTintColor),
                unselectedColor,
                progress
            ),
            ColorUtils.blendARGB(
                bottomSheet.context.getResourceColor(R.attr.actionBarTintColor),
                selectedColor,
                progress
            )
        )

        ext_bottom_sheet.sheet_layout.backgroundTintList = ColorStateList.valueOf(
            ColorUtils.blendARGB(
                bottomSheet.context.getResourceColor(R.attr.colorPrimaryVariant),
                bottomSheet.context.getResourceColor(R.attr.colorSecondary),
                progress
            )
        )
    }

    private fun setBottomPadding() {
        val bottomBar = activity?.bottom_nav ?: return
        ext_bottom_sheet ?: return
        val pad = bottomBar.translationY - bottomBar.height
        val padding = max(
            (-pad).toInt(),
            if (ext_bottom_sheet.sheetBehavior.isExpanded()) 0 else {
                view?.rootWindowInsets?.systemWindowInsetBottom ?: 0
            }
        )
        shadow2.translationY = pad
        ext_bottom_sheet.sheetBehavior?.peekHeight = 58.spToPx + padding
        ext_bottom_sheet.extensionFrameLayout.fast_scroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = -pad.toInt()
        }
        ext_bottom_sheet.migrationFrameLayout.fast_scroller.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = -pad.toInt()
        }
    }

    override fun showSheet() {
        ext_bottom_sheet.sheetBehavior?.expand()
    }

    override fun toggleSheet() {
        if (!ext_bottom_sheet.sheetBehavior.isCollapsed()) {
            ext_bottom_sheet.sheetBehavior?.collapse()
        } else {
            ext_bottom_sheet.sheetBehavior?.expand()
        }
    }

    override fun sheetIsExpanded(): Boolean = ext_bottom_sheet.sheetBehavior.isExpanded()

    override fun handleSheetBack(): Boolean {
        if (!ext_bottom_sheet.sheetBehavior.isCollapsed()) {
            if (ext_bottom_sheet.canGoBack()) {
                ext_bottom_sheet.sheetBehavior?.collapse()
            }
            return true
        }
        return false
    }

    override fun onDestroyView(view: View) {
        adapter = null
        super.onDestroyView(view)
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (!type.isPush) {
            ext_bottom_sheet.updateExtTitle()
            ext_bottom_sheet.presenter.refreshExtensions()
            presenter.updateSources()
        }
        if (!type.isEnter) {
            ext_bottom_sheet.canExpand = false
            activityBinding?.appBar?.elevation =
                when {
                    ext_bottom_sheet.sheetBehavior.isExpanded() -> 0f
                    recycler.canScrollVertically(-1) -> 15f
                    else -> 0f
                }
        } else {
            ext_bottom_sheet.presenter.refreshMigrations()
        }
        setBottomPadding()
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (type.isEnter) {
            ext_bottom_sheet.canExpand = true
            setBottomPadding()
        }
    }

    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        ext_bottom_sheet?.presenter?.refreshExtensions()
        ext_bottom_sheet?.presenter?.refreshMigrations()
        setBottomPadding()
    }

    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter?.getItem(position) as? SourceItem ?: return false
        val source = item.source
        // Open the catalogue view.
        openCatalogue(source, BrowseSourceController(source))
        return false
    }

    fun hideCatalogue(position: Int) {
        val source = (adapter?.getItem(position) as? SourceItem)?.source ?: return
        val current = preferences.hiddenSources().get()
        preferences.hiddenSources().set(current + source.id.toString())

        presenter.updateSources()

        snackbar = view?.snack(R.string.source_hidden, Snackbar.LENGTH_INDEFINITE) {
            anchorView = ext_bottom_sheet
            setAction(R.string.undo) {
                val newCurrent = preferences.hiddenSources().get()
                preferences.hiddenSources().set(newCurrent - source.id.toString())
                presenter.updateSources()
            }
        }
        (activity as? MainActivity)?.setUndoSnackBar(snackbar)
    }

    private fun pinCatalogue(source: Source, isPinned: Boolean) {
        val current = preferences.pinnedCatalogues().getOrDefault()
        if (isPinned) {
            preferences.pinnedCatalogues().set(current - source.id.toString())
        } else {
            preferences.pinnedCatalogues().set(current + source.id.toString())
        }

        presenter.updateSources()
    }

    /**
     * Called when browse is clicked in [SourceAdapter]
     */
    override fun onPinClick(position: Int) {
        val item = adapter?.getItem(position) as? SourceItem ?: return
        val isPinned = item.isPinned ?: item.header?.code?.equals(SourcePresenter.PINNED_KEY)
            ?: false
        pinCatalogue(item.source, isPinned)
    }

    /**
     * Called when latest is clicked in [SourceAdapter]
     */
    override fun onLatestClick(position: Int) {
        val item = adapter?.getItem(position) as? SourceItem ?: return
        openCatalogue(item.source, LatestUpdatesController(item.source))
    }

    /**
     * Opens a catalogue with the given controller.
     */
    private fun openCatalogue(source: CatalogueSource, controller: BrowseSourceController) {
        preferences.lastUsedCatalogueSource().set(source.id)
        router.pushController(controller.withFadeTransaction())
    }

    override fun expandSearch() {
        if (showingExtensions) ext_bottom_sheet.sheetBehavior?.collapse()
        else activity?.toolbar?.menu?.findItem(R.id.action_search)?.expandActionView()
    }

    /**
     * Adds items to the options menu.
     *
     * @param menu menu containing options.
     * @param inflater used to load the menu xml.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (onRoot) (activity as? MainActivity)?.setDismissIcon(showingExtensions)
        if (showingExtensions) {
            if (ext_bottom_sheet.tabs.selectedTabPosition == 0) {
                // Inflate menu
                inflater.inflate(R.menu.extension_main, menu)

                // Initialize search option.
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView

                // Change hint to show global search.
                searchView.queryHint = view?.context?.getString(R.string.search_extensions)

                // Create query listener which opens the global search view.
                setOnQueryTextChangeListener(searchView) {
                    extQuery = it ?: ""
                    ext_bottom_sheet.drawExtensions()
                    true
                }
            } else {
                inflater.inflate(R.menu.migration_main, menu)
            }
        } else {
            // Inflate menu
            inflater.inflate(R.menu.catalogue_main, menu)

            // Initialize search option.
            val searchItem = menu.findItem(R.id.action_search)
            val searchView = searchItem.actionView as SearchView

            // Change hint to show global search.
            searchView.queryHint = view?.context?.getString(R.string.global_search)

            // Create query listener which opens the global search view.
            setOnQueryTextChangeListener(searchView, true) {
                if (!it.isNullOrBlank()) performGlobalSearch(it)
                true
            }
        }
    }

    private fun performGlobalSearch(query: String) {
        router.pushController(GlobalSearchController(query).withFadeTransaction())
    }

    /**
     * Called when an option menu item has been selected by the user.
     *
     * @param item The selected item.
     * @return True if this event has been consumed, false if it has not.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Initialize option to open catalogue settings.
            R.id.action_filter -> {
                val controller =
                    if (showingExtensions) {
                        SettingsExtensionsController()
                    } else SettingsSourcesController()
                router.pushController(
                    (RouterTransaction.with(controller)).popChangeHandler(
                        SettingsSourcesFadeChangeHandler()
                    ).pushChangeHandler(FadeChangeHandler())
                )
            }
            R.id.action_migration_guide -> {
                activity?.openInBrowser(HELP_URL)
            }
            R.id.action_sources_settings -> {
                router.pushController(SettingsBrowseController().withFadeTransaction())
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Called to update adapter containing sources.
     */
    fun setSources(sources: List<IFlexible<*>>) {
        adapter?.updateDataSet(sources)
    }

    /**
     * Called to set the last used catalogue at the top of the view.
     */
    fun setLastUsedSource(item: SourceItem?) {
        adapter?.removeAllScrollableHeaders()
        if (item != null) {
            adapter?.addScrollableHeader(item)
            adapter?.addScrollableHeader(LangItem(SourcePresenter.LAST_USED_KEY))
        }
    }

    class SettingsSourcesFadeChangeHandler : FadeChangeHandler()

    @Parcelize
    data class SmartSearchConfig(val origTitle: String, val origMangaId: Long) : Parcelable

    companion object {
        const val HELP_URL = "https://tachiyomi.org/help/guides/source-migration/"
    }
}
