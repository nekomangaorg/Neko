package org.nekomanga.presentation.components

import ToolTipIconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import eu.kanade.tachiyomi.R
import kotlinx.collections.immutable.toPersistentList
import org.nekomanga.presentation.theme.NekoTheme

@Composable
fun ListGridActionButton(isList: Boolean, buttonClicked: () -> Unit) {
    when (isList.not()) {
        true -> ToolTipIconButton(
            toolTipLabel = stringResource(id = R.string.display_as_, "list"),
            icon = Icons.Filled.ViewList,
            buttonClicked = buttonClicked,
        )

        false -> ToolTipIconButton(
            toolTipLabel = stringResource(id = R.string.display_as_, "grid"),
            icon = Icons.Filled.ViewModule,
            buttonClicked = buttonClicked,
        )
    }
}

@Preview
@Composable
private fun ListGridActionButton() {
    Row {
        NekoTheme {
            ListGridActionButton(isList = false) {}
        }
        NekoTheme {
            ListGridActionButton(isList = true) {}
        }
    }
}

@Composable
fun AppBarActions(
    actions: List<AppBar.AppBarAction>,
) {
    var showMenu by remember { mutableStateOf(false) }

    actions.filterIsInstance<AppBar.Action>().map {
        ToolTipIconButton(
            toolTipLabel = it.title,
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
        val title: String,
        val icon: ImageVector,
        val onClick: () -> Unit,
        val isEnabled: Boolean = true,
    ) : AppBarAction

    data class OverflowAction(
        val title: String,
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
