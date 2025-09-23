package org.nekomanga.presentation.components

import ToolTipButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.R
import org.nekomanga.presentation.components.dropdown.MainDropdownMenu
import org.nekomanga.presentation.components.dropdown.SimpleDropDownItem
import org.nekomanga.presentation.components.dropdown.SimpleDropdownMenu

fun listGridAppBarAction(
    isList: Boolean,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
): AppBar.Action {
    return when (isList) {
        true ->
            AppBar.Action(
                title = UiText.StringResource(resourceId = R.string.display_as_grid),
                icon = Icons.Filled.ViewModule,
                onClick = onClick,
                isEnabled = isEnabled,
            )
        false ->
            AppBar.Action(
                title = UiText.StringResource(resourceId = R.string.display_as_list),
                icon = Icons.AutoMirrored.Filled.ViewList,
                onClick = onClick,
                isEnabled = isEnabled,
            )
    }
}

fun showLibraryEntriesAction(showEntries: Int, onClick: () -> Unit): AppBar.Action {
    return when (showEntries % 3) {
        2 ->
            AppBar.Action(
                title = UiText.StringResource(R.string.show_all_manga),
                icon = Icons.Filled.Visibility,
                onClick = onClick,
            )
        1 ->
            AppBar.Action(
                title = UiText.StringResource(R.string.show_library_manga),
                icon = Icons.Filled.Favorite,
                onClick = onClick,
            )
        else ->
            AppBar.Action(
                title = UiText.StringResource(R.string.hide_library_manga),
                icon = Icons.Filled.VisibilityOff,
                onClick = onClick,
            )
    }
}

@Composable
fun AppBarActions(
    actions: List<AppBar.AppBarAction>,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    var showMenu by remember { mutableStateOf(false) }

    actions.filterIsInstance<AppBar.Action>().map {
        ToolTipButton(
            toolTipLabel = it.title.asString(),
            icon = it.icon,
            isEnabled = it.isEnabled,
            buttonClicked = it.onClick,
            enabledTint = tint,
        )
    }

    val overflowActions = actions.filterIsInstance<AppBar.OverflowAction>()
    if (overflowActions.isNotEmpty()) {
        ToolTipButton(
            toolTipLabel = stringResource(R.string.more),
            icon = Icons.Filled.MoreVert,
            buttonClicked = { showMenu = !showMenu },
            enabledTint = tint,
        )

        SimpleDropdownMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            dropDownItems =
                overflowActions
                    .map { appBarAction -> appBarAction.toSimpleAction() }
                    .toPersistentList(),
        )
    }

    val mainDropDown = actions.filterIsInstance<AppBar.MainDropdown>().firstOrNull()
    if (mainDropDown != null) {
        ToolTipButton(
            toolTipLabel = stringResource(R.string.more),
            icon = Icons.Filled.MoreVert,
            buttonClicked = { showMenu = !showMenu },
            enabledTint = tint,
        )

        mainDropDown.menuShowing(showMenu)

        MainDropdownMenu(
            expanded = showMenu,
            incognitoModeEnabled = mainDropDown.incognitoMode,
            incognitoModeClick = mainDropDown.incognitoModeClick,
            settingsClick = mainDropDown.settingsClick,
            statsClick = mainDropDown.statsClick,
            aboutClick = mainDropDown.aboutClick,
            helpClick = mainDropDown.helpClick,
            onDismiss = { showMenu = false },
        )
    }
}

object AppBar {
    interface AppBarAction

    data class MainDropdown(
        val incognitoMode: Boolean,
        val incognitoModeClick: () -> Unit,
        val settingsClick: () -> Unit,
        val statsClick: () -> Unit,
        val aboutClick: () -> Unit,
        val helpClick: () -> Unit,
        val menuShowing: (Boolean) -> Unit,
    ) : AppBarAction

    data class Action(
        val title: UiText,
        val icon: ImageVector,
        val onClick: () -> Unit,
        val isEnabled: Boolean = true,
    ) : AppBarAction

    data class OverflowAction(
        val title: UiText,
        val onClick: (() -> Unit?)? = null,
        val children: List<OverflowAction>? = null,
    ) : AppBarAction {

        fun toSimpleAction(): SimpleDropDownItem {
            return if (children == null) {
                SimpleDropDownItem.Action(title, onClick = { onClick?.invoke() })
            } else {
                SimpleDropDownItem.Parent(title, children = children.map { it.toSimpleAction() })
            }
        }
    }

    object Empty : AppBarAction
}
