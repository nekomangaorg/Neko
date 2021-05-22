package eu.kanade.tachiyomi.ui.setting

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.asImmediateFlow
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.extension.ExtensionUpdateJob
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.migration.MigrationController
import eu.kanade.tachiyomi.util.view.snack
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import kotlinx.coroutines.flow.launchIn
import uy.kohesive.injekt.injectLazy

class SettingsBrowseController : SettingsController() {

    val sourceManager: SourceManager by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = R.string.sources

        preferenceCategory {
            titleRes = R.string.extensions
            switchPreference {
                key = PreferenceKeys.automaticExtUpdates
                titleRes = R.string.check_for_extension_updates
                defaultValue = true

                onChange {
                    it as Boolean
                    ExtensionUpdateJob.setupTask(it)
                    true
                }
            }
        }

        preferenceCategory {
            titleRes = R.string.pref_global_search
            switchPreference {
                key = PreferenceKeys.onlySearchPinned
                titleRes = R.string.only_search_pinned_when
            }
        }

        preferenceCategory {
            titleRes = R.string.migration
            // Only show this if someone has mass migrated manga once

            preference {
                titleRes = R.string.source_migration
                onClick { router.pushController(MigrationController().withFadeTransaction()) }
            }
            if (preferences.skipPreMigration().getOrDefault() || preferences.migrationSources()
                .isSet()
            ) {
                switchPreference {
                    key = PreferenceKeys.skipPreMigration
                    titleRes = R.string.skip_pre_migration
                    summaryRes = R.string.use_last_saved_migration_preferences
                    defaultValue = false
                }
            }
            preference {
                key = "match_pinned_sources"
                titleRes = R.string.match_pinned_sources
                summaryRes = R.string.only_enable_pinned_for_migration
                onClick {
                    val ogSources = preferences.migrationSources().get()
                    val pinnedSources =
                        (preferences.pinnedCatalogues().get() ?: emptySet()).joinToString("/")
                    preferences.migrationSources().set(pinnedSources)
                    (activity as? MainActivity)?.setUndoSnackBar(
                        view?.snack(
                            R.string.migration_sources_changed
                        ) {
                            setAction(R.string.undo) {
                                preferences.migrationSources().set(ogSources)
                            }
                        }
                    )
                }
            }

            preference {
                key = "match_enabled_sources"
                titleRes = R.string.match_enabled_sources
                summaryRes = R.string.only_enable_enabled_for_migration
                onClick {
                    val ogSources = preferences.migrationSources().get()
                    val languages = preferences.enabledLanguages().get()
                    val hiddenCatalogues = preferences.hiddenSources().get()
                    val enabledSources =
                        sourceManager.getCatalogueSources().filter { it.lang in languages }
                            .filterNot { it.id.toString() in hiddenCatalogues }
                            .sortedBy { "(${it.lang}) ${it.name}" }
                            .joinToString("/") { it.id.toString() }
                    preferences.migrationSources().set(enabledSources)
                    (activity as? MainActivity)?.setUndoSnackBar(
                        view?.snack(
                            R.string.migration_sources_changed
                        ) {
                            setAction(R.string.undo) {
                                preferences.migrationSources().set(ogSources)
                            }
                        }
                    )
                }
            }

            infoPreference(R.string.you_can_migrate_in_library)
        }

        preferenceCategory {
            titleRes = R.string.nsfw_sources

            switchPreference {
                key = PreferenceKeys.showNsfwSource
                titleRes = R.string.show_in_sources
                summaryRes = R.string.requires_app_restart
                defaultValue = true
            }
            switchPreference {
                key = PreferenceKeys.showNsfwExtension
                titleRes = R.string.show_in_extensions
                defaultValue = true
            }
            switchPreference {
                key = PreferenceKeys.labelNsfwExtension
                titleRes = R.string.label_in_extensions
                defaultValue = true

                preferences.showNsfwExtension().asImmediateFlow { isVisible = it }.launchIn(viewScope)
            }

            infoPreference(R.string.does_not_prevent_unofficial_nsfw)
        }
    }
}
