package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.nekomanga.R
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

class SettingsDownloadController : SettingsController() {

    private val db: DatabaseHelper by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) =
        screen.apply {
            titleRes = R.string.downloads

            switchPreference {
                key = Keys.downloadOnlyOverWifi
                titleRes = R.string.only_download_over_wifi
                defaultValue = true
            }
            switchPreference {
                key = Keys.saveChaptersAsCBZ
                titleRes = R.string.save_chapters_as_cbz
                defaultValue = true
            }
            switchPreference {
                bindTo(readerPreferences.splitTallImages())
                titleRes = R.string.split_tall_images
                summaryRes = R.string.split_tall_images_summary
            }

            preferenceCategory {
                titleRes = R.string.remove_after_read

                switchPreference {
                    key = Keys.removeAfterMarkedAsRead
                    titleRes = R.string.remove_when_marked_as_read
                    defaultValue = false
                }
                intListPreference(activity) {
                    key = Keys.removeAfterReadSlots
                    titleRes = R.string.remove_after_read
                    entriesRes =
                        arrayOf(
                            R.string.never,
                            R.string.last_read_chapter,
                            R.string.second_to_last,
                            R.string.third_to_last,
                            R.string.fourth_to_last,
                            R.string.fifth_to_last,
                        )
                    entryRange = -1..4
                    defaultValue = -1
                }
            }

            val dbCategories = db.getCategories().executeAsBlocking()
            val categories = listOf(Category.createDefault(context)) + dbCategories

            preferenceCategory {
                titleRes = R.string.download_new_chapters

                switchPreference {
                    bindTo(preferences.downloadNewChapters())
                    titleRes = R.string.download_new_chapters
                }
                triStateListPreference(activity) {
                    preferences.apply {
                        bindTo(downloadNewChaptersInCategories(), excludeCategoriesInDownloadNew())
                    }
                    titleRes = R.string.categories
                    entries = categories.map { it.name }
                    entryValues = categories.map { it.id.toString() }
                    allSelectionRes = R.string.all

                    preferences
                        .downloadNewChapters()
                        .changes()
                        .onEach { isVisible = it }
                        .launchIn(viewScope)
                }
            }

            preferenceCategory {
                titleRes = R.string.download_ahead

                intListPreference(activity) {
                    bindTo(preferences.autoDownloadWhileReading())
                    titleRes = R.string.auto_download_while_reading
                    entries =
                        listOf(
                            context.getString(R.string.never),
                            context.resources.getQuantityString(
                                R.plurals.next_unread_chapters, 2, 2),
                            context.resources.getQuantityString(
                                R.plurals.next_unread_chapters, 3, 3),
                            context.resources.getQuantityString(
                                R.plurals.next_unread_chapters, 5, 5),
                            context.resources.getQuantityString(
                                R.plurals.next_unread_chapters, 10, 10),
                        )
                    entryValues = listOf(0, 2, 3, 5, 10)
                }
                infoPreference(R.string.download_ahead_info)
            }

            preferenceCategory {
                titleRes = R.string.automatic_removal

                intListPreference(activity) {
                    bindTo(preferences.deleteRemovedChapters())
                    titleRes = R.string.delete_removed_chapters
                    summary = activity?.getString(R.string.delete_downloaded_if_removed_online)
                    entriesRes =
                        arrayOf(
                            R.string.ask_on_chapters_page,
                            R.string.always_keep,
                            R.string.always_delete,
                        )
                    entryRange = 0..2
                }
            }
        }
}
