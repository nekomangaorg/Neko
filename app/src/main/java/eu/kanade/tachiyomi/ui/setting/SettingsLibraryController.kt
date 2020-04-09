package eu.kanade.tachiyomi.ui.setting

import android.os.Handler
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.base.controller.withFadeTransaction
import eu.kanade.tachiyomi.ui.category.CategoryController
import kotlinx.android.synthetic.main.pref_library_columns.view.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys

class SettingsLibraryController : SettingsController() {

    private val db: DatabaseHelper = Injekt.get()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = with(screen) {
        titleRes = R.string.library
        preferenceCategory {
            titleRes = R.string.display
            switchPreference {
                key = Keys.removeArticles
                titleRes = R.string.pref_remove_articles
                summaryRes = R.string.pref_remove_articles_summary
                defaultValue = false
            }
        }

        val dbCategories = db.getCategories().executeAsBlocking()

        preferenceCategory {
            titleRes = R.string.categories
            preference {
                titleRes = R.string.edit_categories
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
                    listOf(context.getString(R.string.always_ask)) + categories.map { it.name }.toTypedArray()
                entryValues = listOf(-1) + categories.mapNotNull { it.id }.toList()
                defaultValue = "-1"

                val selectedCategory = categories.find { it.id == preferences.defaultCategory() }
                summary =
                    selectedCategory?.name ?: context.getString(R.string.always_ask)
                onChange { newValue ->
                    summary = categories.find {
                        it.id == newValue as Int
                    }?.name ?: context.getString(R.string.always_ask)
                    true
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.updates
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
                    R.string.manual,
                    R.string.hourly,
                    R.string.every_2_hours,
                    R.string.every_3_hours,
                    R.string.every_6_hours,
                    R.string.every_12_hours,
                    R.string.daily,
                    R.string.every_2_days
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
                titleRes = R.string.library_update_restriction
                entriesRes = arrayOf(R.string.wifi, R.string.charging)
                entryValues = listOf("wifi", "ac")
                customSummaryRes = R.string.library_update_restriction_summary

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
                titleRes = R.string.library_update_order

                // The following array lines up with the list rankingScheme in:
                // ../../data/library/LibraryUpdateRanker.kt
                entriesRes = arrayOf(
                    R.string.alphabetically, R.string.last_updated
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
}
