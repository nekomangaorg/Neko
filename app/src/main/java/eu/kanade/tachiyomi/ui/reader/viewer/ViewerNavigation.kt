package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import androidx.annotation.StringRes
import org.nekomanga.R

abstract class ViewerNavigation {

    sealed class NavigationRegion(@param:StringRes val nameRes: Int, val colorRes: Int) {
        val name: String
            get() = javaClass.simpleName

        object MENU : NavigationRegion(R.string.menu, R.color.navigation_menu)

        object PREV : NavigationRegion(R.string.previous, R.color.navigation_prev)

        object NEXT : NavigationRegion(R.string.next, R.color.navigation_next)

        object LEFT : NavigationRegion(R.string.left, R.color.navigation_next)

        object RIGHT : NavigationRegion(R.string.right, R.color.navigation_prev)

        fun directionalRegion(LTR: Boolean): NavigationRegion {
            return if (this === LEFT || this === RIGHT) {
                if (if (LTR) this === RIGHT else this === LEFT) NEXT else PREV
            } else {
                this
            }
        }
    }

    data class NormalizedRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        fun contains(x: Float, y: Float): Boolean {
            return x >= left && x < right && y >= top && y < bottom
        }

        fun invert(invertMode: TappingInvertMode): NormalizedRect {
            val horizontal = invertMode.shouldInvertHorizontal
            val vertical = invertMode.shouldInvertVertical
            return when {
                horizontal && vertical ->
                    NormalizedRect(1f - right, 1f - bottom, 1f - left, 1f - top)
                vertical -> NormalizedRect(left, 1f - bottom, right, 1f - top)
                horizontal -> NormalizedRect(1f - right, top, 1f - left, bottom)
                else -> this
            }
        }
    }

    data class Region(val rect: NormalizedRect, val type: NavigationRegion) {
        fun invert(invertMode: TappingInvertMode): Region {
            if (invertMode == TappingInvertMode.NONE) return this
            return this.copy(rect = this.rect.invert(invertMode))
        }
    }

    private var constantMenuRegion: NormalizedRect = NormalizedRect(0f, 0f, 1f, 0.05f)

    abstract var regions: List<Region>

    var invertMode: TappingInvertMode = TappingInvertMode.NONE

    fun getAction(pos: PointF): NavigationRegion {
        val x = pos.x
        val y = pos.y
        val region = regions.map { it.invert(invertMode) }.find { it.rect.contains(x, y) }
        return when {
            region != null -> region.type
            constantMenuRegion.contains(x, y) -> NavigationRegion.MENU
            else -> NavigationRegion.MENU
        }
    }

    enum class TappingInvertMode(
        val shouldInvertHorizontal: Boolean = false,
        val shouldInvertVertical: Boolean = false,
    ) {
        NONE,
        HORIZONTAL(shouldInvertHorizontal = true),
        VERTICAL(shouldInvertVertical = true),
        BOTH(shouldInvertHorizontal = true, shouldInvertVertical = true),
    }
}
