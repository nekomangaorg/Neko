package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.jobs.library.DelayedLibrarySuggestionsJob
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.domain.library.LibraryPreferences
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class LibrarySettingsScreen(
    val libraryPreferences: LibraryPreferences,
    onNavigationIconClick: () -> Unit,
    val categories: State<List<Category>>,
    val setLibrarySearchSuggestion: () -> Unit,
    val onAddEditCategoryClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.library

    @Composable
    override fun getPreferences(): ImmutableList<Preference> {
        val context = LocalContext.current
        return persistentListOf(
            Preference.PreferenceGroup(
                title = stringResource(R.string.general),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.SwitchPreference(
                            pref = libraryPreferences.removeArticles(),
                            title = stringResource(R.string.sort_by_ignoring_articles),
                            subtitle = stringResource(R.string.when_sorting_ignore_articles),
                        ),
                        Preference.PreferenceItem.SwitchPreference(
                            pref = libraryPreferences.showSearchSuggestions(),
                            title = stringResource(R.string.search_suggestions),
                            subtitle = stringResource(R.string.search_tips_show_periodically),
                            onValueChanged = {
                                if (it) {
                                    setLibrarySearchSuggestion()
                                } else {
                                    DelayedLibrarySuggestionsJob.setupTask(context, false)
                                    libraryPreferences.searchSuggestions().set("")
                                }
                                true
                            },
                        ),
                        Preference.PreferenceItem.InfoPreference(
                            stringResource(R.string.display_options_can_be)
                        ),
                    ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(R.string.categories),
                preferenceItems =
                    persistentListOf(
                        Preference.PreferenceItem.TextPreference(
                            title =
                                if (categories.value.isNotEmpty())
                                    stringResource(R.string.edit_categories)
                                else stringResource(R.string.add_categories),
                            subtitle =
                                pluralStringResource(
                                    R.plurals.category_plural,
                                    categories.value.size,
                                    categories.value.size,
                                ),
                            onClick = onAddEditCategoryClick,
                        )
                    ),
            ),
        )
    }

    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): ImmutableList<SearchTerm> {
            return persistentListOf(
                SearchTerm(
                    stringResource(R.string.sort_by_ignoring_articles),
                    stringResource(R.string.when_sorting_ignore_articles),
                ),
                SearchTerm(
                    stringResource(R.string.search_suggestions),
                    stringResource(R.string.search_tips_show_periodically),
                ),
                SearchTerm(stringResource(R.string.display_options_can_be)),
                SearchTerm(stringResource(R.string.add_edit_categories)),
            )
        }
    }
}
