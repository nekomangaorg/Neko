package eu.kanade.tachiyomi.ui.setting

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import eu.kanade.tachiyomi.ui.base.controller.BasicComposeController
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoScaffold
import org.nekomanga.presentation.components.NekoScaffoldType
import org.nekomanga.presentation.theme.Size

class LicensesController : BasicComposeController() {
    @Composable
    override fun ScreenContent() {
        val libraries by produceLibraries(R.raw.aboutlibraries)
        NekoScaffold(
            type = NekoScaffoldType.Title,
            onNavigationIconClicked = router::handleBack,
            title = stringResource(id = R.string.open_source_licenses),
            content = { contentPadding ->
                LibrariesContainer(
                    contentPadding = contentPadding,
                    libraries = libraries,
                    padding =
                        LibraryDefaults.libraryPadding(contentPadding = PaddingValues(Size.tiny)),
                )
            },
        )
    }
}
