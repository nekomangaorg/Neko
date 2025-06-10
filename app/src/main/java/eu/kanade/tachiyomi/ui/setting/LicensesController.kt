package eu.kanade.tachiyomi.ui.setting

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.libraryColors
import eu.kanade.tachiyomi.ui.base.controller.BasicComposeController
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.theme.Size

class LicensesController : BasicComposeController() {
    @Composable
    override fun ScreenContent() {
        NekoScaffold(
            title = stringResource(id = R.string.open_source_licenses),
            type = NekoScaffoldType.Title,
            onNavigationIconClicked = router::handleBack,
        ) { contentPadding ->
            LibrariesContainer(
                contentPadding = contentPadding,
                colors =
                    LibraryDefaults.libraryColors(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        badgeBackgroundColor = MaterialTheme.colorScheme.primary,
                        badgeContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                padding = LibraryDefaults.libraryPadding(contentPadding = PaddingValues(Size.tiny)),
            )
        }
    }
}
