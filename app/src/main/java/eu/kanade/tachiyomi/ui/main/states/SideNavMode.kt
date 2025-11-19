package eu.kanade.tachiyomi.ui.main.states

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class SideNavMode : Parcelable {
    @Parcelize object Default : SideNavMode()

    @Parcelize object Never : SideNavMode()

    @Parcelize object Always : SideNavMode()

    fun toInt(): Int {
        return when (this) {
            Always -> 2
            Never -> 1
            Default -> 0
        }
    }

    companion object {
        fun fromInt(type: Int): SideNavMode {
            return when (type) {
                2 -> Always
                1 -> Never
                else -> Default
            }
        }
    }
}
