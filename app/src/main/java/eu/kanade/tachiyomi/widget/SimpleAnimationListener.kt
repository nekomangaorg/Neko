package eu.kanade.tachiyomi.widget

import android.view.animation.Animation

/** Add an action which will be invoked when the animation has ended. */
inline fun Animation.doOnEnd(
    crossinline action: (animation: Animation) -> Unit,
): Animation.AnimationListener = setListener(onEnd = action)

/** Add an action which will be invoked when the animation has started. */
inline fun Animation.doOnStart(
    crossinline action: (animation: Animation) -> Unit,
): Animation.AnimationListener = setListener(onStart = action)

/**
 * Add a listener to this Animation using the provided actions.
 *
 * @return the [Animation.AnimationListener] added to the Animator
 */
inline fun Animation.setListener(
    crossinline onEnd: (animation: Animation) -> Unit = {},
    crossinline onStart: (animation: Animation) -> Unit = {},
    crossinline onRepeat: (animation: Animation) -> Unit = {},
): Animation.AnimationListener {
    val listener = object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation) = onRepeat(animation)
        override fun onAnimationEnd(animation: Animation) = onEnd(animation)
        override fun onAnimationStart(animation: Animation) = onStart(animation)
    }
    setAnimationListener(listener)
    return listener
}
