package eu.kanade.tachiyomi.ui.manga

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.transition.ChangeBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.shape.CornerFamily
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.FullCoverDialogBinding
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.powerManager
import eu.kanade.tachiyomi.util.system.rootWindowInsetsCompat
import eu.kanade.tachiyomi.util.view.animateBlur
import uy.kohesive.injekt.injectLazy

class FullCoverDialog(val controller: MangaDetailsController, drawable: Drawable, private val thumbView: View) :
    ComponentDialog(controller.activity!!, R.style.FullCoverDialogTheme) {

    val activity = controller.activity
    val binding = FullCoverDialogBinding.inflate(LayoutInflater.from(context), null, false)
    val preferences: PreferencesHelper by injectLazy()

    private val ratio = 5f.dpToPx
    private val fullRatio = 0f
    val shortAnimationDuration = (
        activity?.resources?.getInteger(
            android.R.integer.config_shortAnimTime,
        ) ?: 0
        ).toLong()

    private val powerSaverChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !this@FullCoverDialog.context.powerManager.isPowerSaveMode
            window?.setDimAmount(if (canBlur) 0.45f else 0.77f)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
            if (canBlur) {
                activity?.window?.decorView?.setRenderEffect(
                    RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP),
                )
            } else {
                activity?.window?.decorView?.setRenderEffect(null)
            }
        }
    }

    init {
        val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !context.powerManager.isPowerSaveMode
        window?.setDimAmount(if (canBlur) 0.45f else 0.77f)
        setContentView(binding.root)

        val filter = IntentFilter()
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.registerReceiver(powerSaverChangeReceiver, filter)
        }

        onBackPressedDispatcher.addCallback {
            if (binding.mangaCoverFull.isClickable) {
                animateBack()
            }
        }

        binding.touchOutside.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.mangaCoverFull.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnSave.setOnClickListener {
            controller.saveCover()
        }
        binding.btnShare.setOnClickListener {
            controller.shareCover()
        }

        val expandedImageView = binding.mangaCoverFull
        expandedImageView.shapeAppearanceModel =
            expandedImageView.shapeAppearanceModel.toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, ratio)
                .build()

        expandedImageView.setImageDrawable(drawable)

        val rect = Rect()
        thumbView.getGlobalVisibleRect(rect)
        val systemInsets = activity?.window?.decorView?.rootWindowInsetsCompat?.getInsets(systemBars())
        val topInset = systemInsets?.top ?: 0
        val leftInset = systemInsets?.left ?: 0
        val rightInset = systemInsets?.right ?: 0
        expandedImageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = thumbView.height
            width = thumbView.width
            topMargin = rect.top - topInset
            leftMargin = rect.left - leftInset
            rightMargin = rect.right - rightInset
            bottomMargin = rect.bottom
            horizontalBias = 0.0f
            verticalBias = 0.0f
        }
        expandedImageView.requestLayout()
        binding.btnShare.alpha = 0f
        binding.btnSave.alpha = 0f

        expandedImageView.post {
            // Hide the thumbnail and show the zoomed-in view. When the animation
            // begins, it will position the zoomed-in view in the place of the
            // thumbnail.
            thumbView.alpha = 0f
            val defMargin = 8.dpToPx
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity?.window?.decorView?.animateBlur(1f, 20f, 50)?.start()
            }
            expandedImageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = 0
                width = 0
                topMargin = defMargin + 48.dpToPx
                marginStart = defMargin
                marginEnd = defMargin
                bottomMargin = defMargin
                horizontalBias = 0.5f
                verticalBias = 0.5f
            }

            // TransitionSet for the full cover because using animation for this SUCKS
            val transitionSet = TransitionSet()
            val bound = ChangeBounds()
            transitionSet.addTransition(bound)
            val changeImageTransform = ChangeImageTransform()
            transitionSet.addTransition(changeImageTransform)
            transitionSet.duration = shortAnimationDuration
            TransitionManager.beginDelayedTransition(binding.root, transitionSet)

            AnimatorSet().apply {
                val radiusAnimator = ValueAnimator.ofFloat(ratio, fullRatio).apply {
                    addUpdateListener {
                        val value = it.animatedValue as Float
                        expandedImageView.shapeAppearanceModel =
                            expandedImageView.shapeAppearanceModel.toBuilder()
                                .setAllCorners(CornerFamily.ROUNDED, value)
                                .build()
                    }
                    duration = shortAnimationDuration
                }
                val saveAnimator = ValueAnimator.ofFloat(binding.btnShare.alpha, 1f).apply {
                    addUpdateListener {
                        binding.btnShare.alpha = it.animatedValue as Float
                        binding.btnSave.alpha = it.animatedValue as Float
                    }
                }
                playTogether(radiusAnimator, saveAnimator)
                duration = shortAnimationDuration
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        window?.let { window ->
            window.navigationBarColor = Color.TRANSPARENT
            window.decorView.fitsSystemWindows = true
            val wic = WindowInsetsControllerCompat(window, window.decorView)
            wic.isAppearanceLightStatusBars = false
            wic.isAppearanceLightNavigationBars = false
        }
    }

    override fun cancel() {
        super.cancel()
        thumbView.alpha = 1f
    }

    override fun dismiss() {
        super.dismiss()
        thumbView.alpha = 1f
    }

    private fun animateBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                context.unregisterReceiver(powerSaverChangeReceiver)
            } catch (_: Exception) { }
        }
        val rect2 = Rect()
        thumbView.getGlobalVisibleRect(rect2)
        binding.mangaCoverFull.isClickable = false
        binding.touchOutside.isClickable = false
        val expandedImageView = binding.mangaCoverFull
        val systemInsets = activity?.window?.decorView?.rootWindowInsetsCompat?.getInsets(systemBars())
        val topInset = systemInsets?.top ?: 0
        val leftInset = systemInsets?.left ?: 0
        val rightInset = systemInsets?.right ?: 0
        expandedImageView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = thumbView.height
            width = thumbView.width
            topMargin = rect2.top - topInset
            leftMargin = rect2.left - leftInset
            rightMargin = rect2.right - rightInset
            bottomMargin = rect2.bottom
            horizontalBias = 0.0f
            verticalBias = 0.0f
        }

        // Zoom out back to tc thumbnail
        val transitionSet2 = TransitionSet()
        val bound2 = ChangeBounds()
        transitionSet2.addTransition(bound2)
        val changeImageTransform2 = ChangeImageTransform()
        transitionSet2.addTransition(changeImageTransform2)
        transitionSet2.duration = shortAnimationDuration
        TransitionManager.beginDelayedTransition(binding.root, transitionSet2)

        if (Build.VERSION.SDK_INT >= 31) {
            activity?.window?.decorView?.animateBlur(20f, 0.1f, 50, true)?.apply {
                startDelay = shortAnimationDuration - 100
            }?.start()
        }
        val attrs = window?.attributes
        val ogDim = attrs?.dimAmount ?: 0.25f

        // AnimationSet for backdrop because idk how to use TransitionSet
        AnimatorSet().apply {
            val radiusAnimator = ValueAnimator.ofFloat(fullRatio, ratio).apply {
                addUpdateListener {
                    val value = it.animatedValue as Float
                    expandedImageView.shapeAppearanceModel =
                        expandedImageView.shapeAppearanceModel.toBuilder()
                            .setAllCorners(CornerFamily.ROUNDED, value)
                            .build()
                }
            }
            val dimAnimator = ValueAnimator.ofFloat(ogDim, 0f).apply {
                addUpdateListener {
                    window?.setDimAmount(it.animatedValue as Float)
                }
            }

            val saveAnimator = ValueAnimator.ofFloat(binding.btnShare.alpha, 0f).apply {
                addUpdateListener {
                    binding.btnShare.alpha = it.animatedValue as Float
                    binding.btnSave.alpha = it.animatedValue as Float
                }
            }

            playTogether(radiusAnimator, dimAnimator, saveAnimator)

            addListener(
                onEnd = {
                    TransitionManager.endTransitions(binding.root)
                    thumbView.alpha = 1f
                    expandedImageView.post {
                        dismiss()
                    }
                },
                onCancel = {
                    TransitionManager.endTransitions(binding.root)
                    thumbView.alpha = 1f
                    expandedImageView.post {
                        dismiss()
                    }
                },
            )
            interpolator = DecelerateInterpolator()
            duration = shortAnimationDuration
        }.start()
    }
}
