package eu.kanade.tachiyomi.ui.main.states

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class SideNavAlignment : Parcelable {
    @Parcelize object Top : SideNavAlignment()

    @Parcelize object Center : SideNavAlignment()

    @Parcelize object Bottom : SideNavAlignment()

    fun toInt(): Int {
        return when (this) {
            Bottom -> 2
            Center -> 1
            Top -> 0
        }
    }

    companion object {
        fun fromInt(type: Int): SideNavAlignment {
            return when (type) {
                2 -> Bottom
                0 -> Top
                else -> Center
            }
        }
    }
}
