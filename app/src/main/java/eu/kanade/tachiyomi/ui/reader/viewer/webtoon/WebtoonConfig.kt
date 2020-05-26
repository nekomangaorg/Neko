package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Configuration used by webtoon viewers.
 */
class WebtoonConfig(preferences: PreferencesHelper = Injekt.get()) : ViewerConfig(preferences) {

    var webtoonCropBorders = false
        private set

    var verticalCropBorders = false
        private set

    var sidePadding = 0
        private set

    var enableZoomOut = false
        private set
    var zoomPropertyChangedListener: ((Boolean) -> Unit)? = null

    init {
        preferences.cropBordersWebtoon()
            .register({ webtoonCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        preferences.cropBorders()
            .register({ verticalCropBorders = it }, { imagePropertyChangedListener?.invoke() })

        preferences.webtoonSidePadding()
            .register({ sidePadding = it }, { imagePropertyChangedListener?.invoke() })

        preferences.webtoonEnableZoomOut()
            .register({ enableZoomOut = it }, { zoomPropertyChangedListener?.invoke(it) })
    }
}
