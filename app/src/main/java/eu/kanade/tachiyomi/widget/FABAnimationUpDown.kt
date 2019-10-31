package eu.kanade.tachiyomi.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import eu.kanade.tachiyomi.R
import kotlin.math.min

@Suppress("unused", "UNUSED_PARAMETER")
class FABAnimationUpDown @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) :
    FABAnimationBase() {

    private val INTERPOLATOR = FastOutSlowInInterpolator()

    private val outAnimation by lazy {
        AnimationUtils.loadAnimation(ctx, R.anim.fab_hide_to_bottom).apply {
            duration = 200
            interpolator = INTERPOLATOR
        }
    }
    private val inAnimation by lazy {
        AnimationUtils.loadAnimation(ctx, R.anim.fab_show_from_bottom).apply {
            duration = 200
            interpolator = INTERPOLATOR
        }
    }

    override fun animateOut(button: FloatingActionButton) {
        outAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                isAnimatingOut = true
            }

            override fun onAnimationEnd(animation: Animation) {
                isAnimatingOut = false
                button.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(animation: Animation) {
            }
        })
        button.startAnimation(outAnimation)
    }

    override fun animateIn(button: FloatingActionButton) {
        button.visibility = View.VISIBLE
        button.startAnimation(inAnimation)
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        val translationY = getFabTranslationYForSnackbar(parent, child)
        child.translationY = translationY
        return true
    }

    private fun getFabTranslationYForSnackbar(parent: CoordinatorLayout, fab:
    FloatingActionButton): Float {
        var minOffset = 0f
        val dependencies = parent.getDependencies(fab)
        for (i in 0 until dependencies.size) {
            val view = dependencies[i]
            if (view is Snackbar.SnackbarLayout) {
                minOffset = min(minOffset, view.translationY - view.height)
            }
        }
        return minOffset
    }

    override fun getInsetDodgeRect(parent: CoordinatorLayout, child: FloatingActionButton, rect: Rect): Boolean {
        rect.set(child.left, child.top + 100, child.right, child.bottom - 1000)
        return true
    }
}