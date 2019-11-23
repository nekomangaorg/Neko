package eu.kanade.tachiyomi.widget

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlin.math.min

class FABMoveBehaviour(context: Context, attrs: AttributeSet) :
    CoordinatorLayout.Behavior<FloatingActionButton>(context, attrs) {

    override fun layoutDependsOn(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }
    override fun onDependentViewChanged(parent: CoordinatorLayout, child: FloatingActionButton, dependency: View): Boolean {
        if (isTablet(child.context)) return true
        val translationY = getFabTranslationYForSnackbar(parent, child)
        child.translationY = translationY
        return true
    }

    private fun isTablet(context: Context): Boolean {
        return (context.resources.configuration.screenLayout and Configuration
            .SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    private fun getFabTranslationYForSnackbar(parent: CoordinatorLayout, fab: FloatingActionButton): Float {
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
}