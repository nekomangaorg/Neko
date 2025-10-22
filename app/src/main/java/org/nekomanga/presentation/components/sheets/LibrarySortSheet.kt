package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibrarySort
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.theme.ThemeColorState
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Composable
fun LibrarySortSheet(
    currentLibrarySort: LibrarySort,
    librarySortClicked: (LibrarySort) -> Unit,
    themeColorState: ThemeColorState = defaultThemeColorState(),
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides themeColorState.rippleConfiguration
    ) {
        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .6

        BaseSheet(themeColor = themeColorState) {
            val paddingModifier = Modifier.padding(horizontal = Size.small)

            Gap(Size.medium)
            Text(
                modifier = paddingModifier.fillMaxWidth(),
                text = stringResource(R.string.sort_by),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Gap(Size.medium)
            LazyColumn(
                modifier = Modifier.fillMaxWidth().requiredHeightIn(Size.none, maxLazyHeight.dp)
            ) {
                items(
                    items = LibrarySort.filteredEntries(),
                    key = { librarySort -> librarySort.stringRes() },
                ) { librarySort ->
                    val textColor =
                        if (currentLibrarySort == librarySort)
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = NekoColors.disabledAlphaHighContrast
                            )
                        else MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable(
                                    enabled = librarySort != currentLibrarySort,
                                    onClick = { librarySortClicked(librarySort) },
                                )
                                .padding(horizontal = Size.small, vertical = Size.smedium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = librarySort.composeIcon(),
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(Size.large),
                        )
                        Gap(Size.medium)
                        Text(
                            text = stringResource(librarySort.stringRes()),
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}
