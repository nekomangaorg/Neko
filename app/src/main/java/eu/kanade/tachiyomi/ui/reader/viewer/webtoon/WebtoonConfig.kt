package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

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
import org.nekomanga.core.preferences.PreferenceValues
import org.nekomanga.domain.reader.ReaderPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Configuration used by webtoon viewers. */
class WebtoonConfig(
    scope: CoroutineScope,
    preferences: PreferencesHelper = Injekt.get(),
    readerPreferences: ReaderPreferences = Injekt.get(),
) : ViewerConfig(preferences, readerPreferences, scope) {

    var usePageTransitions = false
        private set

    var webtoonCropBorders = false
        private set

    var verticalCropBorders = true
        private set

    var sidePadding = 0
        private set

    var enableZoomOut = false
        private set

    var zoomPropertyChangedListener: ((Boolean) -> Unit)? = null

    var splitPages =
        readerPreferences.webtoonPageLayout().get() == PageLayout.SPLIT_PAGES.webtoonValue

    var invertDoublePages = false

    var menuThreshold = PreferenceValues.ReaderHideThreshold.LOW.threshold

    init {
        readerPreferences.animatedPageTransitionsWebtoon().register({ usePageTransitions = it })

        readerPreferences
            .navigationModeWebtoon()
            .register({ navigationMode = it }, { updateNavigation(it) })

        readerPreferences
            .webtoonNavInverted()
            .register(
                { tappingInverted = it },
                { navigator.invertMode = it },
            )

        readerPreferences
            .webtoonNavInverted()
            .changes()
            .drop(1)
            .onEach { navigationModeInvertedListener?.invoke() }
            .launchIn(scope)

        readerPreferences
            .cropBordersWebtoon()
            .register({ webtoonCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences
            .cropBorders()
            .register({ verticalCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences
            .webtoonSidePadding()
            .register({ sidePadding = it }, { imagePropertyChangedListener?.invoke() })

        readerPreferences
            .webtoonEnableZoomOut()
            .register({ enableZoomOut = it }, { zoomPropertyChangedListener?.invoke(it) })

        readerPreferences
            .webtoonPageLayout()
            .register(
                { splitPages = it == PageLayout.SPLIT_PAGES.webtoonValue },
                { imagePropertyChangedListener?.invoke() },
            )
        readerPreferences.webtoonReaderHideThreshold().register({ menuThreshold = it.threshold })
        readerPreferences
            .webtoonInvertDoublePages()
            .register({ invertDoublePages = it }, { imagePropertyChangedListener?.invoke() })

        navigationOverlayForNewUser = preferences.showNavigationOverlayNewUserWebtoon().get()
        if (navigationOverlayForNewUser) {
            preferences.showNavigationOverlayNewUserWebtoon().set(false)
        }
    }

    override var navigator: ViewerNavigation = defaultNavigation()
        set(value) {
            field = value.also { it.invertMode = tappingInverted }
        }

    override fun defaultNavigation(): ViewerNavigation {
        return LNavigation()
    }

    override fun updateNavigation(navigationMode: Int) {
        this.navigator =
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
}
