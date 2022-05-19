package org.nekomanga.presentation.components

import TooltipBox
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import eu.kanade.tachiyomi.R
import org.nekomanga.presentation.theme.NekoTheme

@Composable
fun ListGridActionButton(isList: Boolean, buttonClicked: () -> Unit) {
    when (isList.not()) {
        true -> TooltipBox(
            toolTipLabel = stringResource(id = R.string.display_as_, "list"),
            icon = Icons.Filled.ViewList,
            buttonClicked = buttonClicked
        )

        false -> TooltipBox(
            toolTipLabel = stringResource(id = R.string.display_as_, "grid"),
            icon = Icons.Filled.ViewModule,
            buttonClicked = buttonClicked
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
        TooltipBox(
            toolTipLabel = it.title,
            icon = it.icon,
            isEnabled = it.isEnabled,
            buttonClicked = it.onClick
        )
    }

    val overflowActions = actions.filterIsInstance<AppBar.OverflowAction>()
    if (overflowActions.isNotEmpty()) {
        TooltipBox(toolTipLabel = stringResource(R.string.more),
            icon = Icons.Filled.MoreVert,
            buttonClicked = { showMenu = !showMenu })

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            overflowActions.map {
                DropdownMenuItem(
                    onClick = {
                        it.onClick()
                        showMenu = false
                    },
                    text = { Text(it.title) },
                )
            }
        }
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
        val onClick: () -> Unit,
    ) : AppBarAction

    object Empty : AppBarAction
}
