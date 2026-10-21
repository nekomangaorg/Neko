package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation.NormalizedRect

/**
 * Visualization of default state without any inversion +---+---+---+ | N | M | P | P: Move Right
 * +---+---+---+ | N | M | P | M: Menu +---+---+---+ | N | M | P | N: Move Left +---+---+---+
 */
class RightAndLeftNavigation : ViewerNavigation() {

    override var regions: List<Region> =
        listOf(
            Region(
                rect = NormalizedRect(0f, 0f, 0.25f, 1f),
                type = NavigationRegion.LEFT,
            ),
            Region(
                rect = NormalizedRect(0.75f, 0f, 1f, 1f),
                type = NavigationRegion.RIGHT,
            ),
        )
}
