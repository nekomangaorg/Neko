package eu.kanade.tachiyomi.ui.reader.viewer.pager

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.domain.reader.ReaderPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Configuration used by pager viewers. */
class PagerConfig(
    scope: CoroutineScope,
    private val viewer: PagerViewer,
    preferences: PreferencesHelper = Injekt.get(),
    readerPreferences: ReaderPreferences = Injekt.get(),
) : ViewerConfig(preferences, readerPreferences, scope) {

    var usePageTransitions = false
        private set

    var imageScaleType = 1
        private set

    var imageZoomType = ZoomType.Left
        private set

    var imageCropBorders = false
        private set

    var navigateToPan = false
        private set

    var landscapeZoom = false
        private set

    var readerTheme = 0
        private set

    var cutoutBehavior = 0
        private set

    var isFullscreen = true
        private set

    var shiftDoublePage = false

    var doublePages = readerPreferences.pageLayout().get() == PageLayout.DOUBLE_PAGES.value
        set(value) {
            field = value
            if (!value) {
                shiftDoublePage = false
            }
        }

    var invertDoublePages = false

    var doublePageGap = 0

    var autoDoublePages = readerPreferences.pageLayout().get() == PageLayout.AUTOMATIC.value

    var splitPages = readerPreferences.pageLayout().get() == PageLayout.SPLIT_PAGES.value
    var autoSplitPages = readerPreferences.automaticSplitsPage().get()

    init {
        readerPreferences.animatedPageTransitions().register({ usePageTransitions = it })

        readerPreferences.fullscreen().register({ isFullscreen = it })

        readerPreferences
            .imageScaleType()
            .register({ imageScaleType = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences
            .navigationModePager()
            .register({ navigationMode = it }, { updateNavigation(navigationMode) })

        readerPreferences
            .pagerNavInverted()
            .register(
                { tappingInverted = it },
                { navigator.invertMode = it },
            )

        readerPreferences
            .pagerNavInverted()
            .changes()
            .drop(1)
            .onEach { navigationModeInvertedListener?.invoke() }
            .launchIn(scope)

        readerPreferences
            .pagerCutoutBehavior()
            .register({ cutoutBehavior = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences
            .zoomStart()
            .register({ zoomTypeFromPreference(it) }, { imagePropertyChangedListener?.invoke() })

        readerPreferences
            .cropBorders()
            .register({ imageCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences.navigateToPan().register({ navigateToPan = it })

        readerPreferences
            .landscapeZoom()
            .register({ landscapeZoom = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences
            .readerTheme()
            .register({ readerTheme = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences
            .invertDoublePages()
            .register({ invertDoublePages = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences
            .doublePageGap()
            .register(
                { doublePageGap = it },
                { imagePropertyChangedListener?.invoke() },
            )

        readerPreferences
            .doublePageRotate()
            .register(
                { doublePageRotate = it },
                { imagePropertyChangedListener?.invoke() },
            )

        readerPreferences
            .doublePageRotateReverse()
            .register(
                { doublePageRotateReverse = it },
                { imagePropertyChangedListener?.invoke() },
            )

        readerPreferences
            .pageLayout()
            .changes()
            .drop(1)
            .onEach {
                autoDoublePages = it == PageLayout.AUTOMATIC.value
                splitPages = it == PageLayout.SPLIT_PAGES.value
                if (!autoDoublePages) {
                    doublePages = it == PageLayout.DOUBLE_PAGES.value
                }
                reloadChapterListener?.invoke(doublePages)
            }
            .launchIn(scope)
        readerPreferences
            .pageLayout()
            .register(
                {
                    autoDoublePages = it == PageLayout.AUTOMATIC.value
                    if (!autoDoublePages) {
                        doublePages = it == PageLayout.DOUBLE_PAGES.value
                        splitPages = it == PageLayout.SPLIT_PAGES.value
                    }
                },
            )

        readerPreferences.automaticSplitsPage().register({ autoSplitPages = it })
        navigationOverlayForNewUser = preferences.showNavigationOverlayNewUser().get()
        if (navigationOverlayForNewUser) {
            preferences.showNavigationOverlayNewUser().set(false)
        }
    }

    private fun zoomTypeFromPreference(value: Int) {
        imageZoomType =
            when (value) {
                // Auto
                1 ->
                    when (viewer) {
                        is L2RPagerViewer -> ZoomType.Left
                        is R2LPagerViewer -> ZoomType.Right
                        else -> ZoomType.Center
                    }
                // Left
                2 -> ZoomType.Left
                // Right
                3 -> ZoomType.Right
                // Center
                else -> ZoomType.Center
            }
    }

    override var navigator: ViewerNavigation = defaultNavigation()
        set(value) {
            field = value.also { it.invertMode = this.tappingInverted }
        }

    override fun defaultNavigation(): ViewerNavigation {
        return when (viewer) {
            is VerticalPagerViewer -> LNavigation()
            else -> RightAndLeftNavigation()
        }
    }

    fun scaleTypeIsFullFit(): Boolean {
        return when (imageScaleType) {
            SubsamplingScaleImageView.SCALE_TYPE_FIT_HEIGHT,
            SubsamplingScaleImageView.SCALE_TYPE_SMART_FIT,
            SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP, -> true
            else -> false
        }
    }

    override fun updateNavigation(navigationMode: Int) {
        navigator =
            when (navigationMode) {
                0 -> defaultNavigation()
                1 -> LNavigation()
                2 -> KindlishNavigation()
                3 -> EdgeNavigation()
                4 -> RightAndLeftNavigation()
                5 -> DisabledNavigation()
                else -> defaultNavigation()
            }
        navigationModeChangedListener?.invoke()
    }

    enum class ZoomType {
        Left,
        Center,
        Right
    }

    companion object {
        const val CUTOUT_PAD = 0
        const val CUTOUT_START_EXTENDED = 1
        const val CUTOUT_IGNORE = 2
    }
}
