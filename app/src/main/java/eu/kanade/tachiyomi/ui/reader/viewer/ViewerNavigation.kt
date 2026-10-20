package eu.kanade.tachiyomi.ui.reader.viewer

import android.graphics.PointF
import android.graphics.RectF
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

    data class Region(val rectF: RectF, val type: NavigationRegion) {
        fun invert(invertMode: TappingInvertMode): Region {
            if (invertMode == TappingInvertMode.NONE) return this
            return this.copy(rectF = this.rectF.invert(invertMode))
        }
    }

    private var constantMenuRegion: RectF = RectF(0f, 0f, 1f, 0.05f)

    abstract var regions: List<Region>

    var invertMode: TappingInvertMode = TappingInvertMode.NONE

    fun getAction(pos: PointF): NavigationRegion {
        val x = pos.x
        val y = pos.y
        val region = regions.map { it.invert(invertMode) }.find { it.rectF.contains(x, y) }
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

fun RectF.invert(invertMode: ViewerNavigation.TappingInvertMode): RectF {
    val horizontal = invertMode.shouldInvertHorizontal
    val vertical = invertMode.shouldInvertVertical
    return when {
        horizontal && vertical -> {
            val l = 1f - this.right
            val t = 1f - this.bottom
            val r = 1f - this.left
            val b = 1f - this.top
            RectF(l, t, r, b).apply {
                left = l
                top = t
                right = r
                bottom = b
            }
        }
        vertical -> {
            val l = this.left
            val t = 1f - this.bottom
            val r = this.right
            val b = 1f - this.top
            RectF(l, t, r, b).apply {
                left = l
                top = t
                right = r
                bottom = b
            }
        }
        horizontal -> {
            val l = 1f - this.right
            val t = this.top
            val r = 1f - this.left
            val b = this.bottom
            RectF(l, t, r, b).apply {
                left = l
                top = t
                right = r
                bottom = b
            }
        }
        else -> this
    }
}
