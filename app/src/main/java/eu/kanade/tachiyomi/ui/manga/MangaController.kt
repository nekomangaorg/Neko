package eu.kanade.tachiyomi.ui.manga

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.support.RouterPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.PublishRelay
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.RxController
import eu.kanade.tachiyomi.ui.base.controller.TabbedController
import eu.kanade.tachiyomi.ui.base.controller.requestPermissionsSafe
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersController
import eu.kanade.tachiyomi.ui.manga.info.MangaInfoController
import eu.kanade.tachiyomi.ui.manga.related.RelatedController
import eu.kanade.tachiyomi.ui.manga.track.TrackController
import eu.kanade.tachiyomi.util.toast
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.android.synthetic.main.manga_controller.*
import rx.Subscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.*

class MangaController : RxController, TabbedController {

    constructor(manga: Manga?, fromCatalogue: Boolean = false) : super(Bundle().apply {
        putLong(MANGA_EXTRA, manga?.id ?: 0)
        putBoolean(FROM_CATALOGUE_EXTRA, fromCatalogue)
    }) {
        this.manga = manga
        if (manga != null) {
            source = Injekt.get<SourceManager>().getSource(manga.source)
        }
    }

    constructor(mangaId: Long) : this(
            Injekt.get<DatabaseHelper>().getManga(mangaId).executeAsBlocking())

    @Suppress("unused")
    constructor(bundle: Bundle) : this(bundle.getLong(MANGA_EXTRA))

    var manga: Manga? = null
        private set

    var source: Source? = null
        private set

    private var adapter: MangaDetailAdapter? = null

    private val preferences by injectLazy<PreferencesHelper>()

    val fromCatalogue = args.getBoolean(FROM_CATALOGUE_EXTRA, false)

    val lastUpdateRelay: BehaviorRelay<Date> = BehaviorRelay.create()

    val chapterCountRelay: BehaviorRelay<Float> = BehaviorRelay.create()

    val mangaFavoriteRelay: PublishRelay<Boolean> = PublishRelay.create()

    private val trackingIconRelay: BehaviorRelay<Boolean> = BehaviorRelay.create()

    private var trackingIconSubscription: Subscription? = null

    override fun getTitle(): String? {
        return manga?.title
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
        return inflater.inflate(R.layout.manga_controller, container, false)
    }

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        if (manga == null || source == null) return

        requestPermissionsSafe(arrayOf(WRITE_EXTERNAL_STORAGE), 301)

        adapter = MangaDetailAdapter()
        manga_pager.offscreenPageLimit = 4
        manga_pager.adapter = adapter

        if (!fromCatalogue) {
            manga_pager.currentItem = CHAPTERS_CONTROLLER
        }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        adapter = null
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeStarted(handler, type)
        if (type.isEnter) {
            activity?.tabs?.setupWithViewPager(manga_pager)
            checkInitialTrackState()
            trackingIconSubscription = trackingIconRelay.subscribe { setTrackingIconInternal(it) }
        }
    }

    private fun checkInitialTrackState() {
        val manga = manga ?: return
        val loggedServices by lazy { Injekt.get<TrackManager>().services.filter { it.isLogged } }
        val db = Injekt.get<DatabaseHelper>()
        val tracks = db.getTracks(manga).executeAsBlocking()

        if (loggedServices.any { service -> tracks.any { it.sync_id == service.id } }) {
            setTrackingIcon(true)
        }
    }

    override fun onChangeEnded(handler: ControllerChangeHandler, type: ControllerChangeType) {
        super.onChangeEnded(handler, type)
        if (manga == null || source == null) {
            activity?.toast(R.string.manga_not_in_db)
            router.popController(this)
        }
    }

    override fun configureTabs(tabs: TabLayout) {
        with(tabs) {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_FIXED
        }
    }

    override fun cleanupTabs(tabs: TabLayout) {
        trackingIconSubscription?.unsubscribe()
        setTrackingIconInternal(false)
    }

    fun setTrackingIcon(visible: Boolean) {
        trackingIconRelay.call(visible)
    }

    private fun setTrackingIconInternal(visible: Boolean) {
        if (!Injekt.get<TrackManager>().hasLoggedServices()) return
        val tab = activity?.tabs?.getTabAt(adapter!!.getCount()-1) ?: return
        val drawable = if (visible)
            IconicsDrawable(applicationContext!!)
                    .icon(CommunityMaterial.Icon.cmd_check)
                    .colorInt(Color.WHITE)
                    .sizeDp(12)
        else null

        tab.icon = drawable
    }

    private inner class MangaDetailAdapter : RouterPagerAdapter(this@MangaController) {

        private val tabCount = 2 +
                (if (preferences.relatedShowTab()) 1 else 0) +
                (if (Injekt.get<TrackManager>().hasLoggedServices()) 1 else 0)

        private val tabTitles = listOf(
                R.string.manga_detail_tab,
                R.string.manga_chapters_tab,
                if (preferences.relatedShowTab()) R.string.manga_related_tab else R.string.manga_external_tab,
                R.string.manga_external_tab)
                .map { resources!!.getString(it) }

        override fun getCount(): Int {
            return tabCount
        }

        override fun configureRouter(router: Router, position: Int) {
            if (!router.hasRootController()) {

                // First two tabs are always the info page and chapters
                if(position == INFO_CONTROLLER) {
                    router.setRoot(RouterTransaction.with(MangaInfoController()))
                    return
                }
                if(position == CHAPTERS_CONTROLLER) {
                    router.setRoot(RouterTransaction.with(ChaptersController()))
                    return
                }

                // For the 3rd tab display the related if enabled, else show the tracked
                if(position == TAB_3 && preferences.relatedShowTab()) {
                    router.setRoot(RouterTransaction.with(RelatedController(manga!!, source!!)))
                    return
                }
                if(position == TAB_3
                        && !preferences.relatedShowTab()
                        && Injekt.get<TrackManager>().hasLoggedServices()) {
                    router.setRoot(RouterTransaction.with(TrackController()))
                    return
                }

                // If we are at 4, then we should have all tabs enabled as our max count
                if(position == TAB_4 && position <= tabCount) {
                    router.setRoot(RouterTransaction.with(TrackController()))
                    return
                }

                // Else we should error
                error("Wrong position $position")
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return tabTitles[position]
        }

    }

    companion object {

        const val FROM_CATALOGUE_EXTRA = "from_catalogue"
        const val MANGA_EXTRA = "manga"

        const val INFO_CONTROLLER = 0
        const val CHAPTERS_CONTROLLER = 1
        const val TAB_3 = 2
        const val TAB_4 = 3

    }


}
