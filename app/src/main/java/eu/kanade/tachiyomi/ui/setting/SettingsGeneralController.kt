package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.biometric.BiometricManager
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.widget.preference.IntListPreference
import kotlinx.android.synthetic.main.pref_library_columns.view.*
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsGeneralController : SettingsController() {

    private val db: DatabaseHelper = Injekt.get()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_general

        intListPreference {
            key = Keys.theme
            titleRes = R.string.pref_theme
            entriesRes = arrayOf(R.string.light_theme, R.string.dark_theme,
                    R.string.amoled_theme)
            entryValues = arrayOf("1", "2", "3")
            defaultValue = "1"
            summary = "%s"

            onChange {
                activity?.recreate()
                true
            }
        }
        preference {
            titleRes = R.string.pref_library_columns
            onClick {
                LibraryColumnsDialog().showDialog(router)
            }

            fun getColumnValue(value: Int): String {
                return if (value == 0)
                    context.getString(R.string.default_columns)
                else
                    value.toString()
            }

            Observable.combineLatest(
                    preferences.portraitColumns().asObservable(),
                    preferences.landscapeColumns().asObservable()
            ) { portraitCols, landscapeCols -> Pair(portraitCols, landscapeCols) }
                    .subscribeUntilDestroy { (portraitCols, landscapeCols) ->
                        val portrait = getColumnValue(portraitCols)
                        val landscape = getColumnValue(landscapeCols)
                        summary = "${context.getString(R.string.portrait)}: $portrait, " +
                                "${context.getString(R.string.landscape)}: $landscape"
                    }
        }
        intListPreference {
            key = Keys.startScreen
            titleRes = R.string.pref_start_screen
            entriesRes = arrayOf(R.string.label_library, R.string.label_recent_manga,
                    R.string.label_recent_updates)
            entryValues = arrayOf("1", "2", "3")
            defaultValue = "1"
            summary = "%s"
        }
        intListPreference {
            key = Keys.libraryUpdateInterval
            titleRes = R.string.pref_library_update_interval
            entriesRes = arrayOf(R.string.update_never, R.string.update_1hour,
                    R.string.update_2hour, R.string.update_3hour, R.string.update_6hour,
                    R.string.update_12hour, R.string.update_24hour, R.string.update_48hour)
            entryValues = arrayOf("0", "1", "2", "3", "6", "12", "24", "48")
            defaultValue = "0"
            summary = "%s"

            onChange { newValue ->
                // Always cancel the previous task, it seems that sometimes they are not updated.
                LibraryUpdateJob.cancelTask()

                val interval = (newValue as String).toInt()
                if (interval > 0) {
                    LibraryUpdateJob.setupTask(interval)
                }
                true
            }
        }
        multiSelectListPreference {
            key = Keys.libraryUpdateRestriction
            titleRes = R.string.pref_library_update_restriction
            entriesRes = arrayOf(R.string.wifi, R.string.charging)
            entryValues = arrayOf("wifi", "ac")
            summaryRes = R.string.pref_library_update_restriction_summary

            preferences.libraryUpdateInterval().asObservable()
                    .subscribeUntilDestroy { isVisible = it > 0 }

            onChange {
                // Post to event looper to allow the preference to be updated.
                Handler().post { LibraryUpdateJob.setupTask() }
                true
            }
        }
        switchPreference {
            key = Keys.updateOnlyNonCompleted
            titleRes = R.string.pref_update_only_non_completed
            defaultValue = false
        }

        val dbCategories = db.getCategories().executeAsBlocking()

        multiSelectListPreference {
            key = Keys.libraryUpdateCategories
            titleRes = R.string.pref_library_update_categories
            entries = dbCategories.map { it.name }.toTypedArray()
            entryValues = dbCategories.map { it.id.toString() }.toTypedArray()

            preferences.libraryUpdateCategories().asObservable()
                    .subscribeUntilDestroy {
                        val selectedCategories = it
                                .mapNotNull { id -> dbCategories.find { it.id == id.toInt() } }
                                .sortedBy { it.order }

                        summary = if (selectedCategories.isEmpty())
                            context.getString(R.string.all)
                        else
                            selectedCategories.joinToString { it.name }
                    }
        }
        intListPreference {
            key = Keys.defaultCategory
            titleRes = R.string.default_category

            val categories = listOf(Category.createDefault()) + dbCategories

            val selectedCategory = categories.find { it.id == preferences.defaultCategory() }
            entries = arrayOf(context.getString(R.string.default_category_summary)) +
                    categories.map { it.name }.toTypedArray()
            entryValues = arrayOf("-1") + categories.map { it.id.toString() }.toTypedArray()
            defaultValue = "-1"
            summary = selectedCategory?.name ?: context.getString(R.string.default_category_summary)

            onChange { newValue ->
                summary = categories.find {
                    it.id == (newValue as String).toInt()
                }?.name ?: context.getString(R.string.default_category_summary)
                true
            }
        }
        val biometricManager = BiometricManager.from(context)
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            var preference: IntListPreference? = null
            switchPreference {
                key = Keys.useBiometrics
                titleRes = R.string.lock_with_biometrics
                defaultValue = false

                onChange {
                    preference?.isVisible = it as Boolean
                    true
                }
            }
            preference = intListPreference {
                key = Keys.lockAfter
                titleRes = R.string.lock_when_idle
                isVisible = preferences.useBiometrics().getOrDefault()
                val values = arrayOf("0", "2", "5", "10", "20", "30", "60", "90", "120", "-1")
                entries = values.map {
                    when (it) {
                        "0" -> context.getString(R.string.lock_always)
                        "-1" -> context.getString(R.string.lock_never)
                        else -> context.getString(R.string.lock_after_mins, it)
                    }
                }.toTypedArray()
                entryValues = values
                defaultValue = "0"
                summary = "%s"
            }
        }
    }

    class LibraryColumnsDialog : DialogController() {

        private val preferences: PreferencesHelper = Injekt.get()

        private var portrait = preferences.portraitColumns().getOrDefault()
        private var landscape = preferences.landscapeColumns().getOrDefault()

        override fun onCreateDialog(savedViewState: Bundle?): Dialog {
            val dialog = MaterialDialog(activity!!)
                    .title(R.string.pref_library_columns)
                    .customView(viewRes = R.layout.pref_library_columns, scrollable = false)
                    .negativeButton(android.R.string.cancel)
                    .positiveButton(android.R.string.ok){
                        preferences.portraitColumns().set(portrait)
                        preferences.landscapeColumns().set(landscape)
                    }

            onViewCreated(dialog.view)
            return dialog
        }

        fun onViewCreated(view: View) {
            with(view.portrait_columns) {
                displayedValues = arrayOf(context.getString(R.string.default_columns)) +
                        IntRange(1, 10).map(Int::toString)
                value = portrait

                setOnValueChangedListener { _, _, newValue ->
                    portrait = newValue
                }
            }
            with(view.landscape_columns) {
                displayedValues = arrayOf(context.getString(R.string.default_columns)) +
                        IntRange(1, 10).map(Int::toString)
                value = landscape

                setOnValueChangedListener { _, _, newValue ->
                    landscape = newValue
                }
            }
        }

    }

}
