package eu.kanade.tachiyomi.ui.setting

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults.libraryColors
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults.libraryPadding
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.BasicComposeController
import org.nekomanga.presentation.components.NekoScaffold

class LicensesController : BasicComposeController() {
    @Composable
    override fun ScreenContent() {
        NekoScaffold(
            title = stringResource(id = R.string.open_source_licenses),
            onNavigationIconClicked = { activity?.onBackPressed() }) { contentPadding ->
            LibrariesContainer(
                contentPadding = contentPadding,
                colors = libraryColors(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    badgeBackgroundColor = MaterialTheme.colorScheme.primary,
                    badgeContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                padding = libraryPadding(badgeContentPadding = PaddingValues(4.dp))
            )
        }
    }
}
 
 