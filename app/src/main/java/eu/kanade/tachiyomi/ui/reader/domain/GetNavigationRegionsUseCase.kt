package eu.kanade.tachiyomi.ui.reader.domain

import androidx.compose.ui.graphics.Color
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.screens.reader.NavigationRegionUiModel

/**
 * Pure domain use case that transforms viewer navigation regions into immutable
 * [NavigationRegionUiModel]s by applying coordinate inversions, directional mappings (LTR/RTL), and
 * resolving region colors.
 */
class GetNavigationRegionsUseCase {

    operator fun invoke(
        navigation: ViewerNavigation?,
        invertMode: ViewerNavigation.TappingInvertMode =
            navigation?.invertMode ?: ViewerNavigation.TappingInvertMode.NONE,
        isLtr: Boolean = true,
        colorProvider: (Int) -> Color = ::defaultNavigationRegionColor,
    ): List<NavigationRegionUiModel> {
        if (navigation == null || navigation is DisabledNavigation) return emptyList()

        return navigation.regions.map { regionItem ->
            val region = regionItem.invert(invertMode)
            val rect = region.rectF
            val directional = region.type.directionalRegion(isLtr)
            NavigationRegionUiModel(
                id = "${region.type.name}_${rect.left}_${rect.top}",
                leftFraction = rect.left,
                topFraction = rect.top,
                rightFraction = rect.right,
                bottomFraction = rect.bottom,
                label = UiText.StringResource(directional.nameRes),
                color = colorProvider(directional.colorRes),
            )
        }
    }

    companion object {
        fun defaultNavigationRegionColor(colorRes: Int): Color {
            return when (colorRes) {
                R.color.navigation_menu -> Color(0xCC95818D)
                R.color.navigation_next -> Color(0xCCFF124A)
                R.color.navigation_prev -> Color(0xCCFFD9DF)
                else -> Color.Transparent
            }
        }
    }
}
