package eu.kanade.tachiyomi.ui.migration.manga.design

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import com.f2prateek.rx.preferences.Preference
import com.google.android.material.bottomsheet.BottomSheetDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.databinding.MigrationBottomSheetBinding
import eu.kanade.tachiyomi.ui.migration.MigrationFlags
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.toInt
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.setBottomEdge
import eu.kanade.tachiyomi.util.view.setEdgeToEdge
import eu.kanade.tachiyomi.util.view.visible
import uy.kohesive.injekt.injectLazy

class MigrationBottomSheetDialog(
    activity: Activity,
    private val listener: StartMigrationListener
) : BottomSheetDialog(activity, R.style.BottomSheetDialogTheme) {

    /**
     * Preferences helper.
     */
    private val preferences by injectLazy<PreferencesHelper>()

    private val binding = MigrationBottomSheetBinding.inflate(activity.layoutInflater)
    init {
        // Use activity theme for this layout
        setContentView(binding.root)
        if (activity.resources.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.sourceGroup.orientation = LinearLayout.HORIZONTAL
            val params = binding.skipStep.layoutParams as ConstraintLayout.LayoutParams
            params.apply {
                topToBottom = -1
                startToStart = -1
                bottomToBottom = binding.extraSearchParam.id
                startToEnd = binding.extraSearchParam.id
                endToEnd = binding.sourceGroup.id
                topToTop = binding.extraSearchParam.id
                marginStart = 16.dpToPx
            }
            binding.skipStep.layoutParams = params

            val params2 = binding.extraSearchParamText.layoutParams as ConstraintLayout.LayoutParams
            params2.bottomToBottom = binding.optionsLayout.id
            binding.extraSearchParamText.layoutParams = params2

            val params3 = binding.extraSearchParam.layoutParams as ConstraintLayout.LayoutParams
            params3.endToEnd = -1
            binding.extraSearchParam.layoutParams = params3
        }
        setEdgeToEdge(activity, binding.root)
        setBottomEdge(
            if (activity.resources.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) binding.extraSearchParamText
            else binding.skipStep,
            activity
        )
    }

    /**
     * Called when the sheet is created. It initializes the listeners and values of the preferences.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initPreferences()

        // window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding.fab.setOnClickListener {
            preferences.skipPreMigration().set(binding.skipStep.isChecked)
            listener.startMigration(
                if (binding.extraSearchParam.isChecked && binding.extraSearchParamText.text.isNotBlank()) binding.extraSearchParamText.text.toString() else null
            )
            dismiss()
        }
    }

    /**
     * Init general reader preferences.
     */
    private fun initPreferences() {
        val flags = preferences.migrateFlags().getOrDefault()

        binding.migChapters.isChecked = MigrationFlags.hasChapters(flags)
        binding.migCategories.isChecked = MigrationFlags.hasCategories(flags)
        binding.migTracking.isChecked = MigrationFlags.hasTracks(flags)

        binding.migChapters.setOnCheckedChangeListener { _, _ -> setFlags() }
        binding.migCategories.setOnCheckedChangeListener { _, _ -> setFlags() }
        binding.migTracking.setOnCheckedChangeListener { _, _ -> setFlags() }

        binding.extraSearchParamText.isVisible = false
        binding.extraSearchParam.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.extraSearchParamText.visible()
            } else {
                binding.extraSearchParamText.isVisible = false
            }
        }
        binding.sourceGroup.bindToPreference(preferences.useSourceWithMost())

        binding.skipStep.isChecked = preferences.skipPreMigration().getOrDefault()
        binding.skipStep.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) (listener as? Controller)?.activity?.toast(
                R.string.to_show_again_setting_sources,
                Toast.LENGTH_LONG
            )
        }
    }

    private fun setFlags() {
        var flags = 0
        if (binding.migChapters.isChecked) flags = flags or MigrationFlags.CHAPTERS
        if (binding.migCategories.isChecked) flags = flags or MigrationFlags.CATEGORIES
        if (binding.migTracking.isChecked) flags = flags or MigrationFlags.TRACK
        preferences.migrateFlags().set(flags)
    }

    /**
     * Binds a checkbox or switch view with a boolean preference.
     */
    private fun CompoundButton.bindToPreference(pref: Preference<Boolean>) {
        isChecked = pref.getOrDefault()
        setOnCheckedChangeListener { _, isChecked -> pref.set(isChecked) }
    }

    /**
     * Binds a radio group with a boolean preference.
     */
    private fun RadioGroup.bindToPreference(pref: Preference<Boolean>) {
        (getChildAt(pref.getOrDefault().toInt()) as RadioButton).isChecked = true
        setOnCheckedChangeListener { _, value ->
            val index = indexOfChild(findViewById(value))
            pref.set(index == 1)
        }
    }
}

interface StartMigrationListener {
    fun startMigration(extraParam: String?)
}
