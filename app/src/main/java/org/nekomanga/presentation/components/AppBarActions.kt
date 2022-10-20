package org.nekomanga.presentation.components

import ToolTipIconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import kotlinx.collections.immutable.toPersistentList

fun listGridAppBarAction(isList: Boolean, isEnabled: Boolean = true, onClick: () -> Unit): AppBar.Action {
    return when (isList) {
        true -> AppBar.Action(title = UiText.StringResource(resourceId = R.string.display_as_grid), icon = Icons.Filled.ViewModule, onClick = onClick, isEnabled = isEnabled)
        false -> AppBar.Action(title = UiText.StringResource(resourceId = R.string.display_as_list), icon = Icons.Filled.ViewList, onClick = onClick, isEnabled = isEnabled)
    }
}

fun showLibraryEntriesAction(showEntries: Boolean, onClick: () -> Unit): AppBar.Action {
    return when (showEntries) {
        true -> AppBar.Action(title = UiText.StringResource(R.string.hide_library_manga), icon = Icons.Filled.VisibilityOff, onClick = onClick)
        false -> AppBar.Action(title = UiText.StringResource(R.string.show_library_manga), icon = Icons.Filled.Visibility, onClick = onClick)
    }
}

@Composable
fun AppBarActions(
    actions: List<AppBar.AppBarAction>,
) {
    var showMenu by remember { mutableStateOf(false) }

    actions.filterIsInstance<AppBar.Action>().map {
        ToolTipIconButton(
            toolTipLabel = it.title.asString(),
            icon = it.icon,
            isEnabled = it.isEnabled,
            buttonClicked = it.onClick,
        )
    }

    val overflowActions = actions.filterIsInstance<AppBar.OverflowAction>()
    if (overflowActions.isNotEmpty()) {
        ToolTipIconButton(
            toolTipLabel = stringResource(R.string.more),
            icon = Icons.Filled.MoreVert,
            buttonClicked = { showMenu = !showMenu },
        )

        SimpleDropdownMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            dropDownItems =
            overflowActions.map { appBarAction ->
                appBarAction.toSimpleAction()
            }.toPersistentList(),
        )
    }
}

object AppBar {
    interface AppBarAction

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
