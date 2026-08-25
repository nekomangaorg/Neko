package org.nekomanga.presentation.screens.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.sheets.BaseSheet
import org.nekomanga.presentation.components.theme.defaultThemeColorState
import org.nekomanga.presentation.theme.Size

@Immutable
sealed interface ReaderPageAction {
    data object Share : ReaderPageAction

    data object Save : ReaderPageAction

    data object SetAsCover : ReaderPageAction

    data object ShareFirstPage : ReaderPageAction

    data object SaveFirstPage : ReaderPageAction

    data object SetFirstPageAsCover : ReaderPageAction

    data object ShareSecondPage : ReaderPageAction

    data object SaveSecondPage : ReaderPageAction

    data object SetSecondPageAsCover : ReaderPageAction

    data object ShareCombinedPages : ReaderPageAction

    data object SaveCombinedPages : ReaderPageAction
}

@Composable
fun ReaderPageActionsSheet(
    hasExtraPage: Boolean,
    onActionClicked: (ReaderPageAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeColorState = defaultThemeColorState()

    BaseSheet(
        themeColor = themeColorState,
        maxSheetHeightPercentage = 0.9f,
        bottomPaddingAroundContent = Size.small,
    ) {
        Column(modifier = modifier.fillMaxWidth().padding(vertical = Size.small)) {
            if (hasExtraPage) {
                ActionRow(
                    title = stringResource(R.string.share_second_page),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        onActionClicked(ReaderPageAction.ShareSecondPage)
                        onDismiss()
                    },
                )
                ActionRow(
                    title = stringResource(R.string.save_second_page),
                    icon = Icons.Outlined.Save,
                    onClick = {
                        onActionClicked(ReaderPageAction.SaveSecondPage)
                        onDismiss()
                    },
                )
                ActionRow(
                    title = stringResource(R.string.set_second_page_as_cover),
                    icon = Icons.Outlined.Image,
                    onClick = {
                        onActionClicked(ReaderPageAction.SetSecondPageAsCover)
                        onDismiss()
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Size.medium, vertical = Size.tiny)
                )

                ActionRow(
                    title = stringResource(R.string.share_first_page),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        onActionClicked(ReaderPageAction.ShareFirstPage)
                        onDismiss()
                    },
                )
                ActionRow(
                    title = stringResource(R.string.save_first_page),
                    icon = Icons.Outlined.Save,
                    onClick = {
                        onActionClicked(ReaderPageAction.SaveFirstPage)
                        onDismiss()
                    },
                )
                ActionRow(
                    title = stringResource(R.string.set_first_page_as_cover),
                    icon = Icons.Outlined.Image,
                    onClick = {
                        onActionClicked(ReaderPageAction.SetFirstPageAsCover)
                        onDismiss()
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Size.medium, vertical = Size.tiny)
                )

                ActionRow(
                    title = stringResource(R.string.share_combined_pages),
                    icon = Icons.Outlined.Collections,
                    onClick = {
                        onActionClicked(ReaderPageAction.ShareCombinedPages)
                        onDismiss()
                    },
                )
                ActionRow(
                    title = stringResource(R.string.save_combined_pages),
                    icon = Icons.Outlined.Collections,
                    onClick = {
                        onActionClicked(ReaderPageAction.SaveCombinedPages)
                        onDismiss()
                    },
                )
            } else {
                ActionRow(
                    title = stringResource(R.string.share),
                    icon = Icons.Outlined.Share,
                    onClick = {
                        onActionClicked(ReaderPageAction.Share)
                        onDismiss()
                    },
                )
                ActionRow(
                    title = stringResource(R.string.save),
                    icon = Icons.Outlined.Save,
                    onClick = {
                        onActionClicked(ReaderPageAction.Save)
                        onDismiss()
                    },
                )
                ActionRow(
                    title = stringResource(R.string.set_as_cover),
                    icon = Icons.Outlined.Image,
                    onClick = {
                        onActionClicked(ReaderPageAction.SetAsCover)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Size.medium, vertical = Size.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Size.large),
        )
        Gap(Size.medium)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
