package eu.kanade.tachiyomi.widget

import android.animation.Animator
import android.view.animation.Animation

open class SimpleAnimationListener : Animation.AnimationListener {
    override fun onAnimationRepeat(animation: Animation) {}

    override fun onAnimationEnd(animation: Animation) {}

    override fun onAnimationStart(animation: Animation) {}
}

open class SimpleAnimatorListener : Animator.AnimatorListener {

    override fun onAnimationCancel(animation: Animator?) {}
    override fun onAnimationRepeat(animator: Animator) {}

    override fun onAnimationEnd(animator: Animator) {}

    override fun onAnimationStart(animator: Animator) {}
}

class StartAnimatorListener(private val startAnimationListener: (animator: Animator) -> Unit) :
    SimpleAnimatorListener() {
    override fun onAnimationStart(animator: Animator) {
        startAnimationListener(animator)
    }
}

class EndAnimatorListener(private val endAnimationListener: (animator: Animator) -> Unit) :
    SimpleAnimatorListener() {
    override fun onAnimationEnd(animator: Animator) {
        endAnimationListener(animator)
    }
}
