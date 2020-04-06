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
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.category.CategoryController
import kotlinx.android.synthetic.main.pref_library_columns.view.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsLibraryController : SettingsController() {

    private val db: DatabaseHelper = Injekt.get()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.pref_category_library
        preferenceCategory {
            titleRes = R.string.pref_category_library_display
            /*
            switchPreference {
                key = Keys.libraryAsSingleList
                titleRes = R.string.pref_library_single_list
                summaryRes = R.string.pref_library_single_list_summary
                defaultValue = false
            }*/

            switchPreference {
                key = Keys.removeArticles
                titleRes = R.string.pref_remove_articles
                summaryRes = R.string.pref_remove_articles_summary
                defaultValue = false
            }
        }

        val dbCategories = db.getCategories().executeAsBlocking()

        preferenceCategory {
            titleRes = R.string.pref_category_library_categories
            preference {
                titleRes = R.string.action_edit_categories
                val catCount = db.getCategories().executeAsBlocking().size
                summary =
                    context.resources.getQuantityString(R.plurals.category, catCount, catCount)
                onClick { router.pushController(CategoryController().withFadeTransaction()) }
            }
            intListPreference(activity) {
                key = Keys.defaultCategory
                titleRes = R.string.default_category

                val categories = listOf(Category.createDefault(context)) + dbCategories
                entries =
                    listOf(context.getString(R.string.default_category_summary)) + categories.map { it.name }
                        .toTypedArray()
                entryValues = listOf(-1) + categories.mapNotNull { it.id }.toList()
                defaultValue = "-1"

                val selectedCategory = categories.find { it.id == preferences.defaultCategory() }
                summary =
                    selectedCategory?.name ?: context.getString(R.string.default_category_summary)
                onChange { newValue ->
                    summary = categories.find {
                        it.id == newValue as Int
                    }?.name ?: context.getString(R.string.default_category_summary)
                    true
                }
            }
            intListPreference(activity) {
                titleRes = R.string.pref_keep_category_sorting
                key = Keys.keepCatSort

                customSummary = context.getString(R.string.pref_keep_category_sorting_summary)
                entries = listOf(
                    context.getString(R.string.always_ask),
                    context.getString(R.string.option_keep_category_sort),
                    context.getString(R.string.option_switch_to_dnd)
                )
                entryRange = 0..2
                defaultValue = 0
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_category_library_update
            intListPreference(activity) {
                key = Keys.updateOnRefresh
                titleRes = R.string.categories_on_manual

                entriesRes = arrayOf(
                    R.string.first_category, R.string.categories_in_global_update
                )
                entryRange = 0..1
                defaultValue = -1
            }
            intListPreference(activity) {
                key = Keys.libraryUpdateInterval
                titleRes = R.string.pref_library_update_interval
                entriesRes = arrayOf(
                    R.string.update_never,
                    R.string.update_1hour,
                    R.string.update_2hour,
                    R.string.update_3hour,
                    R.string.update_6hour,
                    R.string.update_12hour,
                    R.string.update_24hour,
                    R.string.update_48hour
                )
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

            intListPreference(activity) {
                key = Keys.libraryUpdatePrioritization
                titleRes = R.string.pref_library_update_prioritization

                // The following array lines up with the list rankingScheme in:
                // ../../data/library/LibraryUpdateRanker.kt
                entriesRes = arrayOf(
                    R.string.action_sort_alpha, R.string.action_sort_last_updated
                )
                entryRange = 0..1
                defaultValue = 0
                summaryRes = R.string.pref_library_update_prioritization_summary
            }

            multiSelectListPreferenceMat(activity) {
                key = Keys.libraryUpdateCategories
                titleRes = R.string.pref_library_update_categories
                entries = dbCategories.map { it.name }
                entryValues = dbCategories.map { it.id.toString() }
                allSelectionRes = R.string.all

                preferences.libraryUpdateCategories().asObservable().subscribeUntilDestroy {
                    val selectedCategories =
                        it.mapNotNull { id -> dbCategories.find { it.id == id.toInt() } }
                            .sortedBy { it.order }

                    customSummary =
                        if (selectedCategories.isEmpty()) context.getString(R.string.all)
                        else selectedCategories.joinToString { it.name }
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
