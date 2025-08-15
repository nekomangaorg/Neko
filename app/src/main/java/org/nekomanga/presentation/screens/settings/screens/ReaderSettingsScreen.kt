package org.nekomanga.presentation.screens.settings.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.ui.setting.MergeLoginEvent
import eu.kanade.tachiyomi.ui.setting.MergeScreenState
import eu.kanade.tachiyomi.ui.setting.MergeScreenType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharedFlow
import org.nekomanga.R
import org.nekomanga.presentation.components.dialog.LoginDialog
import org.nekomanga.presentation.components.dialog.LogoutDialog
import org.nekomanga.presentation.screens.settings.Preference
import org.nekomanga.presentation.screens.settings.widgets.SearchTerm

internal class ReaderSettingsScreen(
    onNavigationIconClick: () -> Unit,
) : SearchableSettings(onNavigationIconClick) {

    override fun getTitleRes(): Int = R.string.merge_source_settings

    @Composable
    override fun getPreferences(): ImmutableList<Preference> {
        val context = LocalContext.current

        return persistentListOf(

        )
    }



    companion object : SearchTermProvider {
        @Composable
        override fun getSearchTerms(): ImmutableList<SearchTerm> {
            return persistentListOf()
        }
    }
}
