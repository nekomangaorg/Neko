package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.RectF
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

abstract class ViewerNavigation {

    sealed class NavigationRegion(@StringRes val nameRes: Int, val colorRes: Int) {
        object MENU : NavigationRegion(R.string.menu, R.color.navigation_menu)
        object PREV : NavigationRegion(R.string.previous, R.color.navigation_prev)
        object NEXT : NavigationRegion(R.string.next, R.color.navigation_next)
        object LEFT : NavigationRegion(R.string.left, R.color.navigation_next)
        object RIGHT : NavigationRegion(R.string.right, R.color.navigation_prev)

        fun directionalRegion(LTR: Boolean): NavigationRegion {
            return if (this === LEFT || this === RIGHT) {
                if (if (LTR) this === LEFT else this === RIGHT) NEXT else PREV
            } else this
        }
    }

    data class Region(
        val rectF: RectF,
        val type: NavigationRegion
    ) {
        fun invert(invertMode: TappingInvertMode): Region {
            if (invertMode == TappingInvertMode.NONE) return this
            return this.copy(
                rectF = this.rectF.invert(invertMode)
            )
        }
    }

    private var constantMenuRegion: RectF = RectF(0f, 0f, 1f, 0.05f)

    abstract var regions: List<Region>

    var invertMode: TappingInvertMode = TappingInvertMode.NONE

    fun getAction(pos: PointF): NavigationRegion {
        val x = pos.x
        val y = pos.y
        val region = regions.map { it.invert(invertMode) }
            .find { it.rectF.contains(x, y) }
        return when {
            region != null -> region.type
            constantMenuRegion.contains(x, y) -> NavigationRegion.MENU
            else -> NavigationRegion.MENU
        }
    }

    enum class TappingInvertMode(val shouldInvertHorizontal: Boolean = false, val shouldInvertVertical: Boolean = false) {
        NONE,
        HORIZONTAL(shouldInvertHorizontal = true),
        VERTICAL(shouldInvertVertical = true),
        BOTH(shouldInvertHorizontal = true, shouldInvertVertical = true)
    }
}

fun RectF.invert(invertMode: ViewerNavigation.TappingInvertMode): RectF {
    val horizontal = invertMode.shouldInvertHorizontal
    val vertical = invertMode.shouldInvertVertical
    return when {
        horizontal && vertical -> RectF(1f - this.right, 1f - this.bottom, 1f - this.left, 1f - this.top)
        vertical -> RectF(this.left, 1f - this.bottom, this.right, 1f - this.top)
        horizontal -> RectF(1f - this.right, this.top, 1f - this.left, this.bottom)
        else -> this
    }
}
