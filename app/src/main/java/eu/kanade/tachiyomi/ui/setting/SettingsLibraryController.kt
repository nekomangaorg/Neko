package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.preference.PreferenceScreen
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.category.CategoryController
import kotlinx.android.synthetic.main.pref_library_columns.view.landscape_columns
import kotlinx.android.synthetic.main.pref_library_columns.view.portrait_columns
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class SettingsLibraryController : SettingsController() {

    private val db: DatabaseHelper = Injekt.get()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_library

        preferenceCategory {
            titleRes = R.string.pref_category_library_display

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
                        preferences.landscapeColumns().asObservable(),
                        { portraitCols, landscapeCols -> Pair(portraitCols, landscapeCols) })
                        .subscribeUntilDestroy { (portraitCols, landscapeCols) ->
                            val portrait = getColumnValue(portraitCols)
                            val landscape = getColumnValue(landscapeCols)
                            summary = "${context.getString(R.string.portrait)}: $portrait, " +
                                    "${context.getString(R.string.landscape)}: $landscape"
                        }
            }
        }
        preferenceCategory {
            titleRes = R.string.pref_category_library_update

            intListPreference(activity) {
                key = Keys.libraryUpdateInterval
                titleRes = R.string.pref_library_update_interval
                entriesRes = arrayOf(R.string.update_never, R.string.update_1hour,
                        R.string.update_2hour, R.string.update_3hour, R.string.update_6hour,
                        R.string.update_12hour, R.string.update_24hour, R.string.update_48hour)
                entryValues = listOf(0, 1, 2, 3, 6, 12, 24, 48)
                defaultValue = 0

                onChange { newValue ->
                    // Always cancel the previous task, it seems that sometimes they are not updated.
                    LibraryUpdateJob.cancelTask()

                    val interval = newValue as Int
                    if (interval > 0) {
                        LibraryUpdateJob.setupTask(interval)
                    }
                    true
                }
            }
            multiSelectListPreferenceMat(activity) {
                key = Keys.libraryUpdateRestriction
                titleRes = R.string.pref_library_update_restriction
                entriesRes = arrayOf(R.string.wifi, R.string.charging)
                entryValues = listOf("wifi", "ac")
                customSummaryRes = R.string.pref_library_update_restriction_summary

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

            multiSelectListPreferenceMat(activity) {
                key = Keys.libraryUpdateCategories
                titleRes = R.string.pref_library_update_categories
                entries = dbCategories.map { it.name }
                entryValues = dbCategories.map { it.id.toString() }
                allSelectionRes = R.string.all

                preferences.libraryUpdateCategories().asObservable()
                        .subscribeUntilDestroy {
                            val selectedCategories = it
                                    .mapNotNull { id -> dbCategories.find { it.id == id.toInt() } }
                                    .sortedBy { it.order }

                            customSummary = if (selectedCategories.isEmpty())
                                context.getString(R.string.all)
                            else
                                selectedCategories.joinToString { it.name }
                        }
            }

        preferenceCategory {
            titleRes = R.string.pref_category_library_categories

            preference {
                titleRes = R.string.action_edit_categories
                onClick {
                    router.pushController(CategoryController().withFadeTransaction())
                }
            }

            intListPreference(activity) {
                key = Keys.defaultCategory
                titleRes = R.string.default_category

                    val categories = listOf(Category.createDefault()) + dbCategories
                    entries = listOf(context.getString(R.string.default_category_summary)) +
                            categories.map { it.name }.toTypedArray()
                    entryValues = listOf(-1) + categories.mapNotNull { it.id }.toList()
                    defaultValue = "-1"

                    val selectedCategory = categories.find { it.id == preferences.defaultCategory() }
                    summary = selectedCategory?.name ?: context.getString(R.string.default_category_summary)
                    onChange { newValue ->
                        summary = categories.find {
                            it.id == newValue as Int
                        }?.name ?: context.getString(R.string.default_category_summary)
                        true
                    }
                }
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
                    .positiveButton(android.R.string.ok) {
                        preferences.portraitColumns().set(portrait)
                        preferences.landscapeColumns().set(landscape)
                    }
                    .negativeButton(android.R.string.cancel)

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
