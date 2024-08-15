package eu.kanade.tachiyomi.ui.more

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.withSubtitle
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import org.nekomanga.BuildConfig
import org.nekomanga.R
import org.nekomanga.core.preferences.toggle
import org.nekomanga.core.security.SecurityPreferences
import org.nekomanga.databinding.TachiOverflowLayoutBinding
import uy.kohesive.injekt.injectLazy

class OverflowDialog(activity: MainActivity) : Dialog(activity, R.style.OverflowDialogTheme) {

    val binding = TachiOverflowLayoutBinding.inflate(activity.layoutInflater, null, false)
    val preferences: PreferencesHelper by injectLazy()
    val securityPreferences: SecurityPreferences by injectLazy()

    init {
        setContentView(binding.root)

        binding.overflowCardView.backgroundTintList =
            ColorStateList.valueOf(
                ColorUtils.blendARGB(
                    activity.getResourceColor(R.attr.background),
                    activity.getResourceColor(R.attr.colorSecondary),
                    0.075f,
                ),
            )
        binding.touchOutside.setOnClickListener { cancel() }
        val incogText = context.getString(R.string.incognito_mode)
        with(binding.incognitoModeItem) {
            val titleText =
                context.getString(
                    if (securityPreferences.incognitoMode().get()) {
                        R.string.turn_off_
                    } else {
                        R.string.turn_on_
                    },
                    incogText,
                )
            val subtitleText = context.getString(R.string.pauses_reading_history)
            text = titleText.withSubtitle(context, subtitleText)
            setIcon(
                if (securityPreferences.incognitoMode().get()) {
                    R.drawable.ic_incognito_24dp
                } else {
                    R.drawable.ic_glasses_24dp
                },
            )
            setOnClickListener {
                securityPreferences.incognitoMode().toggle()
                val incog = securityPreferences.incognitoMode().get()
                val newTitle =
                    context.getString(
                        if (incog) {
                            R.string.turn_off_
                        } else {
                            R.string.turn_on_
                        },
                        incogText,
                    )
                text = newTitle.withSubtitle(context, subtitleText)
                val drawable =
                    AnimatedVectorDrawableCompat.create(
                        context,
                        if (incog) {
                            R.drawable.anim_read_to_incog
                        } else {
                            R.drawable.anim_incog_to_read
                        },
                    )
                setIcon(drawable)
                (getIcon() as? AnimatedVectorDrawableCompat)?.start()
            }
        }
        binding.settingsItem.setOnClickListener {
            activity.showSettings()
            dismiss()
        }

        binding.helpItem.setOnClickListener {
            activity.openInBrowser(URL_HELP)
            dismiss()
        }

        binding.aboutItem.text =
            context
                .getString(R.string.about)
                .withSubtitle(binding.aboutItem.context, "v${BuildConfig.VERSION_NAME}")

        binding.aboutItem.setOnClickListener {
            activity.showAbout()
            dismiss()
        }

        binding.statsItem.setOnClickListener {
            activity.showStats()
            dismiss()
        }

        binding.overflowCardView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = activity.toolbarHeight - 2.dpToPx
        }
        window?.let { window ->
            window.navigationBarColor = Color.TRANSPARENT
            window.decorView.fitsSystemWindows = true
            val wic = WindowInsetsControllerCompat(window, window.decorView)
            wic.isAppearanceLightStatusBars = false
            wic.isAppearanceLightNavigationBars = false
        }
    }

    private companion object {
        private const val URL_HELP = "https://tachiyomi.org/docs/guides/troubleshooting/"
    }
}
