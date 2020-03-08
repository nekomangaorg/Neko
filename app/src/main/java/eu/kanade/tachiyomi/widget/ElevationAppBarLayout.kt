package eu.kanade.tachiyomi.widget

import android.animation.StateListAnimator
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.AppBarLayout

class ElevationAppBarLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : AppBarLayout(context, attrs) {

    private var origStateAnimator: StateListAnimator? = null
    private var origElevation: Float

    init {
        origStateAnimator = stateListAnimator
        origElevation = elevation
    }

    fun enableElevation() {
       /* if (stateListAnimator == null) {
            stateListAnimator = origStateAnimator
            elevation = origElevation
        }*/
    }

    fun disableElevation() {
       // stateListAnimator = null
        //elevation = 0f
        //translationZ = 0.1f.dpToPx
    }

}
