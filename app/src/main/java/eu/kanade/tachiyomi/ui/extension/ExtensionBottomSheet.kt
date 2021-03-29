package eu.kanade.tachiyomi.ui.extension

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.databinding.ExtensionsBottomSheetBinding
import eu.kanade.tachiyomi.databinding.RecyclerWithScrollerBinding
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.extension.details.ExtensionDetailsController
import eu.kanade.tachiyomi.ui.migration.MangaAdapter
import eu.kanade.tachiyomi.ui.migration.MangaItem
import eu.kanade.tachiyomi.ui.migration.SourceAdapter
import eu.kanade.tachiyomi.ui.migration.SourceItem
import eu.kanade.tachiyomi.ui.migration.manga.design.PreMigrationController
import eu.kanade.tachiyomi.ui.source.BrowseController
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.collapse
import eu.kanade.tachiyomi.util.view.doOnApplyWindowInsets
import eu.kanade.tachiyomi.util.view.expand
import eu.kanade.tachiyomi.util.view.isExpanded
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionBottomSheet @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs),
    ExtensionAdapter.OnButtonClickListener,
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnItemLongClickListener,
    ExtensionTrustDialog.Listener,
    SourceAdapter.OnAllClickListener {

    var sheetBehavior: BottomSheetBehavior<*>? = null

    var shouldCallApi = false

    /**
     * Adapter containing the list of extensions
     */
    private var adapter: FlexibleAdapter<IFlexible<*>>? = null
    private var migAdapter: FlexibleAdapter<IFlexible<*>>? = null

    val presenter = ExtensionBottomPresenter(this)

    private var extensions: List<ExtensionItem> = emptyList()
    var canExpand = false
    private lateinit var binding: ExtensionsBottomSheetBinding

    lateinit var controller: BrowseController

    val extensionFrameLayout = RecyclerWithScrollerBinding.inflate(LayoutInflater.from(context))
    val migrationFrameLayout = RecyclerWithScrollerBinding.inflate(LayoutInflater.from(context))

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = ExtensionsBottomSheetBinding.bind(this)
    }

    fun onCreate(controller: BrowseController) {
        // Initialize adapter, scroll listener and recycler views
        adapter = ExtensionAdapter(this)
        migAdapter = ExtensionAdapter(this)
        sheetBehavior = BottomSheetBehavior.from(this)
        // Create recycler and set adapter.
        val extRecyler = extensionFrameLayout.recycler
        val migRecyler = migrationFrameLayout.recycler

        extRecyler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        extRecyler.adapter = adapter
        extRecyler.setHasFixedSize(true)
        extRecyler.addItemDecoration(ExtensionDividerItemDecoration(context))

        migRecyler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        migRecyler.setHasFixedSize(true)

        adapter?.fastScroller = extensionFrameLayout.fastScroller
        this.controller = controller
        binding.pager.doOnApplyWindowInsets { _, _, _ ->
            val bottomBar = controller.activityBinding?.bottomNav
            extRecyler.updatePaddingRelative(bottom = bottomBar?.height ?: 0)
            migRecyler.updatePaddingRelative(bottom = bottomBar?.height ?: 0)
        }
        binding.pager.adapter = TabbedSheetAdapter()
        binding.tabs.setupWithViewPager(binding.pager)
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (canExpand) {
                    this@ExtensionBottomSheet.sheetBehavior?.expand()
                }
                this@ExtensionBottomSheet.controller.updateTitleAndMenu()
                when (tab?.position) {
                    0 -> extensionFrameLayout
                    else -> migrationFrameLayout
                }.recycler.isNestedScrollingEnabled = true
                when (tab?.position) {
                    0 -> extensionFrameLayout
                    else -> migrationFrameLayout
                }.recycler.requestLayout()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> extensionFrameLayout
                    else -> migrationFrameLayout
                }.recycler.isNestedScrollingEnabled = false
                if (tab?.position == 1) {
                    presenter.deselectSource()
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                this@ExtensionBottomSheet.sheetBehavior?.expand()
                when (tab?.position) {
                    0 -> extensionFrameLayout
                    else -> migrationFrameLayout
                }.recycler.isNestedScrollingEnabled = true
            }
        })
        presenter.onCreate()
        updateExtTitle()

        binding.sheetLayout.setOnClickListener {
            if (!sheetBehavior.isExpanded()) {
                sheetBehavior?.expand()
                fetchOnlineExtensionsIfNeeded()
            } else {
                sheetBehavior?.collapse()
            }
        }
        presenter.getExtensionUpdateCount()
    }

    fun updatedNestedRecyclers() {
        listOf(extensionFrameLayout, migrationFrameLayout).forEachIndexed { index, recyclerWithScrollerBinding ->
            recyclerWithScrollerBinding.recycler.isNestedScrollingEnabled = binding.pager.currentItem == index
        }
    }

    fun fetchOnlineExtensionsIfNeeded() {
        if (shouldCallApi) {
            presenter.findAvailableExtensions()
            shouldCallApi = false
        }
    }

    fun updateExtTitle() {
        val extCount = presenter.getExtensionUpdateCount()
        if (extCount > 0) binding.tabs.getTabAt(0)?.orCreateBadge
        else binding.tabs.getTabAt(0)?.removeBadge()
    }

    override fun onButtonClick(position: Int) {
        val extension = (adapter?.getItem(position) as? ExtensionItem)?.extension ?: return
        when (extension) {
            is Extension.Installed -> {
                if (!extension.hasUpdate) {
                    openDetails(extension)
                } else {
                    presenter.updateExtension(extension)
                }
            }
            is Extension.Available -> {
                presenter.installExtension(extension)
            }
            is Extension.Untrusted -> {
                openTrustDialog(extension)
            }
        }
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        when (binding.tabs.selectedTabPosition) {
            0 -> {
                val extension =
                    (adapter?.getItem(position) as? ExtensionItem)?.extension ?: return false
                if (extension is Extension.Installed) {
                    openDetails(extension)
                } else if (extension is Extension.Untrusted) {
                    openTrustDialog(extension)
                }
            }
            else -> {
                val item = migAdapter?.getItem(position) ?: return false

                if (item is MangaItem) {
                    PreMigrationController.navigateToMigration(
                        Injekt.get<PreferencesHelper>().skipPreMigration().getOrDefault(),
                        controller.router,
                        listOf(item.manga.id!!)
                    )
                } else if (item is SourceItem) {
                    presenter.setSelectedSource(item.source)
                }
            }
        }
        return false
    }

    override fun onItemLongClick(position: Int) {
        if (binding.tabs.selectedTabPosition == 0) {
            val extension = (adapter?.getItem(position) as? ExtensionItem)?.extension ?: return
            if (extension is Extension.Installed || extension is Extension.Untrusted) {
                uninstallExtension(extension.pkgName)
            }
        }
    }

    override fun onAllClick(position: Int) {
        val item = migAdapter?.getItem(position) as? SourceItem ?: return

        val sourceMangas =
            presenter.mangaItems[item.source.id]?.mapNotNull { it.manga.id }?.toList()
                ?: emptyList()
        PreMigrationController.navigateToMigration(
            Injekt.get<PreferencesHelper>().skipPreMigration().getOrDefault(),
            controller.router,
            sourceMangas
        )
    }

    private fun openDetails(extension: Extension.Installed) {
        val controller = ExtensionDetailsController(extension.pkgName)
        this.controller.router.pushController(controller.withFadeTransaction())
    }

    private fun openTrustDialog(extension: Extension.Untrusted) {
        ExtensionTrustDialog(this, extension.signatureHash, extension.pkgName)
            .showDialog(controller.router)
    }

    fun setExtensions(extensions: List<ExtensionItem>, updateController: Boolean = true) {
        this.extensions = extensions
        if (updateController) {
            controller.presenter.updateSources()
        }
        drawExtensions()
    }

    fun setMigrationSources(sources: List<SourceItem>) {
        val migRecyler = migrationFrameLayout.recycler
        if (migAdapter !is SourceAdapter) {
            migAdapter = SourceAdapter(this)
            migRecyler.adapter = migAdapter
            migAdapter?.fastScroller = migrationFrameLayout.fastScroller
        }
        migAdapter?.updateDataSet(sources, true)
    }

    fun setMigrationManga(manga: List<MangaItem>?) {
        val migRecyler = migrationFrameLayout.recycler
        if (migAdapter !is MangaAdapter) {
            migAdapter = MangaAdapter(this)
            migRecyler.adapter = migAdapter
            migAdapter?.fastScroller = migrationFrameLayout.fastScroller
        }
        migAdapter?.updateDataSet(manga, true)
    }

    fun drawExtensions() {
        if (controller.extQuery.isNotBlank()) {
            adapter?.updateDataSet(
                extensions.filter {
                    it.extension.name.contains(controller.extQuery, ignoreCase = true)
                }
            )
        } else {
            adapter?.updateDataSet(extensions)
        }
        updateExtTitle()
    }

    fun canGoBack(): Boolean {
        if (binding.tabs.selectedTabPosition == 1 && migAdapter is MangaAdapter) {
            presenter.deselectSource()
            return false
        }
        return true
    }

    fun downloadUpdate(item: ExtensionItem) {
        adapter?.updateItem(item, item.installStep)
    }

    override fun trustSignature(signatureHash: String) {
        presenter.trustSignature(signatureHash)
    }

    override fun uninstallExtension(pkgName: String) {
        presenter.uninstallExtension(pkgName)
    }

    private inner class TabbedSheetAdapter : ViewPagerAdapter() {

        override fun createView(container: ViewGroup, position: Int): View {
            return when (position) {
                0 -> extensionFrameLayout.root
                else -> migrationFrameLayout.root
            }
        }

        override fun getCount(): Int {
            return 2
        }

        override fun getPageTitle(position: Int): CharSequence {
            return context.getString(
                when (position) {
                    0 -> R.string.extensions
                    else -> R.string.migration
                }
            )
        }
    }
}
