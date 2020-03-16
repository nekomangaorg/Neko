package eu.kanade.tachiyomi.ui.catalogue

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.google.android.material.bottomsheet.BottomSheetBehavior
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.ui.base.controller.NucleusController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.catalogue.browse.BrowseCatalogueController
import eu.kanade.tachiyomi.ui.catalogue.global_search.CatalogueSearchController
import eu.kanade.tachiyomi.ui.catalogue.latest.LatestUpdatesController
import eu.kanade.tachiyomi.ui.extension.SettingsExtensionsController
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import eu.kanade.tachiyomi.ui.setting.SettingsSourcesController
import eu.kanade.tachiyomi.util.view.applyWindowInsetsForRootController
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.widget.preference.SourceLoginDialog
import kotlin.math.max
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.catalogue_main_controller.*
import kotlinx.android.synthetic.main.extensions_bottom_sheet.*
import kotlinx.android.synthetic.main.main_activity.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This controller shows and manages the different catalogues enabled by the user.
 * This controller should only handle UI actions, IO actions should be done by [CataloguePresenter]
 * [SourceLoginDialog.Listener] refreshes the adapter on successful login of catalogues.
 * [CatalogueAdapter.OnBrowseClickListener] call function data on browse item click.
 * [CatalogueAdapter.OnLatestClickListener] call function data on latest item click
 */
class CatalogueController : NucleusController<CataloguePresenter>(),
        SourceLoginDialog.Listener,
        FlexibleAdapter.OnItemClickListener,
        CatalogueAdapter.OnBrowseClickListener,
        RootSearchInterface,

        CatalogueAdapter.OnLatestClickListener {

    /**
     * Application preferences.
     */
    private val preferences: PreferencesHelper = Injekt.get()

    /**
     * Adapter containing sources.
     */
    private var adapter: CatalogueAdapter? = null

    var extQuery = ""
        private set

    var headerHeight = 0

    var customTitle = ""

    var showingExtenions = false

    /**
     * Called when controller is initialized.
     */
    init {
        // Enable the option menu
        setHasOptionsMenu(true)
    }

    /**
     * Set the title of controller.
     *
     * @return title.
     */
    override fun getTitle(): String? {
        return if (showingExtenions)
            applicationContext?.getString(R.string.label_extensions)
        else applicationContext?.getString(R.string.label_catalogues)
    }

    /**
     * Create the [CataloguePresenter] used in controller.
     *
     * @return instance of [CataloguePresenter]
     */
    override fun createPresenter(): CataloguePresenter {
        return CataloguePresenter()
    }

    /**
     * Initiate the view with [R.layout.catalogue_main_controller].
     *
     * @param inflater used to load the layout xml.
     * @param container containing parent views.
     * @return inflated view.
     */
    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.catalogue_main_controller, container, false)
    }

    /**
     * Called when the view is created
     *
     * @param view view of controller
     */
    override fun onViewCreated(view: View) {
        super.onViewCreated(view)
        view.applyWindowInsetsForRootController(activity!!.bottom_nav)

        adapter = CatalogueAdapter(this)

        // Create recycler and set adapter.
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(view.context)
        recycler.adapter = adapter
        recycler.addItemDecoration(SourceDividerItemDecoration(view.context))
        val attrsArray = intArrayOf(android.R.attr.actionBarSize)
        val array = view.context.obtainStyledAttributes(attrsArray)
        val appBarHeight = array.getDimensionPixelSize(0, 0)
        array.recycle()
        scrollViewWith(recycler) {
            headerHeight = it.systemWindowInsetTop + appBarHeight
        }

        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 301)
        ext_bottom_sheet.onCreate(this)

        ext_bottom_sheet.sheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior
        .BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, progress: Float) {
                shadow2.alpha = (1 - max(0f, progress)) * 0.25f
                sheet_layout.alpha = 1 - progress
                activity?.appbar?.y = max(activity!!.appbar.y, -headerHeight * (1 - progress))
                val oldShow = showingExtenions
                showingExtenions = progress > 0.92f
                if (oldShow != showingExtenions) {
                    setTitle()
                    activity?.invalidateOptionsMenu()
                }
            }

            override fun onStateChanged(p0: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_EXPANDED) activity?.appbar?.y = 0f
                if (state == BottomSheetBehavior.STATE_EXPANDED ||
                    state == BottomSheetBehavior.STATE_COLLAPSED) {
                    sheet_layout.alpha =
                        if (state == BottomSheetBehavior.STATE_COLLAPSED) 1f else 0f
                    showingExtenions = state == BottomSheetBehavior.STATE_EXPANDED
                    setTitle()
                    activity?.invalidateOptionsMenu()
                }

                retainViewMode = if (state == BottomSheetBehavior.STATE_EXPANDED)
                    RetainViewMode.RETAIN_DETACH else RetainViewMode.RELEASE_DETACH
                sheet_layout.isClickable = state == BottomSheetBehavior.STATE_COLLAPSED
                sheet_layout.isFocusable = state == BottomSheetBehavior.STATE_COLLAPSED
            }
        })

        if (showingExtenions) {
            ext_bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    fun showExtensions() {
        ext_bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun toggleExtensions() {
        if (ext_bottom_sheet.sheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
            ext_bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            ext_bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun handleRootBack(): Boolean {
        if (ext_bottom_sheet.sheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
            ext_bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
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
        if (!type.isPush && handler is SettingsSourcesFadeChangeHandler) {
            ext_bottom_sheet.updateExtTitle()
            presenter.updateSources()
        }
    }

    /**
     * Called when login dialog is closed, refreshes the adapter.
     *
     * @param source clicked item containing source information.
     */
    override fun loginDialogClosed(source: LoginSource) {
        if (source.isLogged()) {
            adapter?.clear()
            presenter.loadSources()
        }
    }

    /**
     * Called when item is clicked
     */
    override fun onItemClick(view: View, position: Int): Boolean {
        val item = adapter?.getItem(position) as? SourceItem ?: return false
        val source = item.source
        if (source is LoginSource && !source.isLogged()) {
            val dialog = SourceLoginDialog(source)
            dialog.targetController = this
            dialog.showDialog(router)
        } else {
            // Open the catalogue view.
            openCatalogue(source, BrowseCatalogueController(source))
        }
        return false
    }

    /**
     * Called when browse is clicked in [CatalogueAdapter]
     */
    override fun onBrowseClick(position: Int) {
        onItemClick(view!!, position)
    }

    /**
     * Called when latest is clicked in [CatalogueAdapter]
     */
    override fun onLatestClick(position: Int) {
        val item = adapter?.getItem(position) as? SourceItem ?: return
        openCatalogue(item.source, LatestUpdatesController(item.source))
    }

    /**
     * Opens a catalogue with the given controller.
     */
    private fun openCatalogue(source: CatalogueSource, controller: BrowseCatalogueController) {
        preferences.lastUsedCatalogueSource().set(source.id)
        router.pushController(controller.withFadeTransaction())
    }

    /**
     * Adds items to the options menu.
     *
     * @param menu menu containing options.
     * @param inflater used to load the menu xml.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (showingExtenions) {
            // Inflate menu
            inflater.inflate(R.menu.extension_main, menu)

            // Initialize search option.
            val searchItem = menu.findItem(R.id.action_search)
            val searchView = searchItem.actionView as SearchView

            // Change hint to show global search.
            searchView.queryHint = applicationContext?.getString(R.string.search_extensions)

            // Create query listener which opens the global search view.
            setOnQueryTextChangeListener(searchView) {
                extQuery = it ?: ""
                ext_bottom_sheet.drawExtensions()
                true
            }
        } else {
            // Inflate menu
            inflater.inflate(R.menu.catalogue_main, menu)

            // Initialize search option.
            val searchItem = menu.findItem(R.id.action_search)
            val searchView = searchItem.actionView as SearchView

            // Change hint to show global search.
            searchView.queryHint = applicationContext?.getString(R.string.action_global_search_hint)

            // Create query listener which opens the global search view.
            setOnQueryTextChangeListener(searchView, true) {
                if (!it.isNullOrBlank()) performGlobalSearch(it)
                true
            }
        }
    }

    private fun performGlobalSearch(query: String) {
        router.pushController(CatalogueSearchController(query).withFadeTransaction())
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
                    if (showingExtenions)
                        SettingsExtensionsController()
                    else SettingsSourcesController()
                router.pushController(
                    (RouterTransaction.with(controller)).popChangeHandler(
                        SettingsSourcesFadeChangeHandler()
                    ).pushChangeHandler(FadeChangeHandler())
                    )
            }
            R.id.action_dismiss -> {
                ext_bottom_sheet.sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
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
        }
    }

    class SettingsSourcesFadeChangeHandler : FadeChangeHandler()

    @Parcelize
    data class SmartSearchConfig(val origTitle: String, val origMangaId: Long) : Parcelable
}
