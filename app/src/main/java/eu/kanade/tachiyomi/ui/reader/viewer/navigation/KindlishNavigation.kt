package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion +---+---+---+ | M | M | M | P: Previous
 * +---+---+---+ | P | N | N | M: Menu +---+---+---+ | P | N | N | N: Next +---+---+---+
 */
class KindlishNavigation : ViewerNavigation() {

    override var regions: List<Region> =
        listOf(
            Region(
                rectF =
                    RectF(0.33f, 0.33f, 1f, 1f).apply {
                        left = 0.33f
                        top = 0.33f
                        right = 1f
                        bottom = 1f
                    },
                type = NavigationRegion.NEXT,
            ),
            Region(
                rectF =
                    RectF(0f, 0.33f, 0.33f, 1f).apply {
                        left = 0f
                        top = 0.33f
                        right = 0.33f
                        bottom = 1f
                    },
                type = NavigationRegion.PREV,
            ),
        )
}
