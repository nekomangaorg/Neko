package eu.kanade.tachiyomi.ui.setting

import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.jobs.library.DelayedLibrarySuggestionsJob
import eu.kanade.tachiyomi.ui.category.CategoryController
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import eu.kanade.tachiyomi.ui.library.display.TabbedLibraryDisplaySheet
import eu.kanade.tachiyomi.util.system.launchIO
import eu.kanade.tachiyomi.util.system.launchUI
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.core.preferences.DEVICE_BATTERY_NOT_LOW
import org.nekomanga.core.preferences.DEVICE_CHARGING
import org.nekomanga.core.preferences.DEVICE_ONLY_ON_WIFI
import org.nekomanga.domain.library.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsLibraryController : SettingsController() {

    private val db: DatabaseHelper by injectLazy()
    private val libraryPreferences: LibraryPreferences by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.library
        preferenceCategory {
            titleRes = R.string.general
            switchPreference {
                key = libraryPreferences.removeArticles().key()
                titleRes = R.string.sort_by_ignoring_articles
                summaryRes = R.string.when_sorting_ignore_articles
                defaultValue = false
            }

            switchPreference {
                key = libraryPreferences.showSearchSuggestions().key()
                titleRes = R.string.search_suggestions
                summaryRes = R.string.search_tips_show_periodically

                onChange {
                    it as Boolean
                    if (it) {
                        launchIO {
                            LibraryPresenter.setSearchSuggestion(libraryPreferences, db, Injekt.get())
                        }
                    } else {
                        DelayedLibrarySuggestionsJob.setupTask(context, false)
                        libraryPreferences.searchSuggestions().set("")
                    }
                    true
                }
            }

            preference {
                key = "library_display_options"
                isPersistent = false
                titleRes = R.string.display_options
                summaryRes = R.string.can_be_found_in_library_filters

                onClick {
                    TabbedLibraryDisplaySheet(this@SettingsLibraryController).show()
                }
            }
        }

        val dbCategories = db.getCategories().executeAsBlocking()

        preferenceCategory {
            titleRes = R.string.categories
            preference {
                key = "edit_categories"
                isPersistent = false
                val catCount = db.getCategories().executeAsBlocking().size
                titleRes = if (catCount > 0) R.string.edit_categories else R.string.add_categories
                if (catCount > 0) {
                    summary =
                        context.resources.getQuantityString(
                            R.plurals.category_plural,
                            catCount,
                            catCount,
                        )
                }
                onClick { router.pushController(CategoryController().withFadeTransaction()) }
            }
            intListPreference(activity) {
                key = Keys.defaultCategory
                titleRes = R.string.default_category

                val categories = listOf(Category.createDefault(context)) + dbCategories
                entries =
                    listOf(context.getString(R.string.always_ask)) + categories.map { it.name }
                        .toTypedArray()
                entryValues = listOf(-1) + categories.mapNotNull { it.id }.toList()
                defaultValue = "-1"

                val selectedCategory = categories.find { it.id == preferences.defaultCategory().get() }
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
            titleRes = R.string.global_updates
            intListPreference(activity) {
                key = libraryPreferences.updateInterval().key()
                titleRes = R.string.library_update_frequency
                entriesRes = arrayOf(
                    R.string.manual,
                    R.string.every_6_hours,
                    R.string.every_12_hours,
                    R.string.daily,
                    R.string.every_2_days,
                    R.string.weekly,
                )
                entryValues = listOf(0, 6, 12, 24, 48, 168)
                defaultValue = 24

                onChange { newValue ->
                    // Always cancel the previous task, it seems that sometimes they are not updated.
                    LibraryUpdateJob.setupTask(context, 0)

                    val interval = newValue as Int
                    if (interval > 0) {
                        LibraryUpdateJob.setupTask(context, interval)
                    }
                    true
                }
            }
            multiSelectListPreferenceMat(activity) {
                key = libraryPreferences.updateRestrictions().key()
                titleRes = R.string.library_update_restriction
                entriesRes = arrayOf(R.string.wifi, R.string.charging, R.string.battery_not_low)
                entryValues = listOf(DEVICE_ONLY_ON_WIFI, DEVICE_CHARGING, DEVICE_BATTERY_NOT_LOW)
                preSummaryRes = R.string.restrictions_
                noSelectionRes = R.string.none

                libraryPreferences.updateInterval().changes().onEach {
                    isVisible = it > 0
                }.launchIn(viewScope)

                onChange {
                    // Post to event looper to allow the preference to be updated.
                    viewScope.launchUI { LibraryUpdateJob.setupTask(context) }
                    true
                }
            }
            switchPreference {
                key = libraryPreferences.updateOnlyNonCompleted().key()
                titleRes = R.string.only_update_ongoing
                defaultValue = false
            }

            switchPreference {
                key = libraryPreferences.updateOnlyWhenTrackingIsNotFinished().key()
                titleRes = R.string.only_update_based_on_tracking
                defaultValue = false
            }

            switchPreference {
                key = libraryPreferences.updateFaster().key()
                titleRes = R.string.faster_library_update
                defaultValue = false
            }

            intListPreference(activity) {
                key = libraryPreferences.updatePrioritization().key()
                titleRes = R.string.library_update_order

                // The following array lines up with the list rankingScheme in:
                // ../../data/library/LibraryUpdateRanker.kt
                entriesRes = arrayOf(
                    R.string.alphabetically,
                    R.string.last_updated,
                    R.string.next_updated,
                )
                entryRange = 0..2
                defaultValue = 0
            }

            triStateListPreference(activity) {
                libraryPreferences.apply {
                    bindTo(whichCategoriesToUpdate(), whichCategoriesToExclude())
                }
                titleRes = R.string.categories

                val categories = listOf(Category.createDefault(context)) + dbCategories
                entries = categories.map { it.name }
                entryValues = categories.map { it.id.toString() }

                allSelectionRes = R.string.all
            }

            intListPreference(activity) {
                key = libraryPreferences.whatToUpdateOnRefresh().key()
                titleRes = R.string.categories_on_manual

                entriesRes = arrayOf(
                    R.string.first_category,
                    R.string.categories_in_global_update,
                )
                entryRange = 0..1
                defaultValue = -1
            }

            switchPreference {
                key = libraryPreferences.updateCovers().key()
                titleRes = R.string.auto_refresh_covers
                summaryRes = R.string.auto_refresh_covers_summary
                defaultValue = true
            }
        }
    }
}
