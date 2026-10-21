package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NormalizedRect

/**
 * Visualization of default state without any inversion +---+---+---+ | M | M | M | P: Previous
 * +---+---+---+ | P | N | N | M: Menu +---+---+---+ | P | N | N | N: Next +---+---+---+
 */
class KindlishNavigation : ViewerNavigation() {

    override var regions: List<Region> =
        listOf(
            Region(
                rect = NormalizedRect(0.33f, 0.33f, 1f, 1f),
                type = NavigationRegion.NEXT,
            ),
            Region(
                rect = NormalizedRect(0f, 0.33f, 0.33f, 1f),
                type = NavigationRegion.PREV,
            ),
        )
}
