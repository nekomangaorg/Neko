package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NormalizedRect

/**
 * Visualization of default state without any inversion +---+---+---+ | N | N | N | P: Previous
 * +---+---+---+ | N | M | N | M: Menu +---+---+---+ | N | P | N | N: Next +---+---+---+
 */
class EdgeNavigation : ViewerNavigation() {

    override var regions: List<Region> =
        listOf(
            Region(
                rect = NormalizedRect(0f, 0f, 0.33f, 1f),
                type = NavigationRegion.NEXT,
            ),
            Region(
                rect = NormalizedRect(0.33f, 0.66f, 0.66f, 1f),
                type = NavigationRegion.PREV,
            ),
            Region(
                rect = NormalizedRect(0.66f, 0f, 1f, 1f),
                type = NavigationRegion.NEXT,
            ),
        )
}
