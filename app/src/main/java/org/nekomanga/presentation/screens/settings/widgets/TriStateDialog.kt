package org.nekomanga.presentation.screens.settings.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import org.nekomanga.R
import org.nekomanga.presentation.extensions.isScrolledToEnd
import org.nekomanga.presentation.extensions.isScrolledToStart
import org.nekomanga.presentation.theme.Size

private enum class State {
    CHECKED,
    INVERSED,
    UNCHECKED,
}

@Composable
fun <T> TriStateListDialog(
    title: String,
    message: String? = null,
    negativeOnly: Boolean = false,
    items: List<T>,
    initialChecked: List<T>,
    initialInversed: List<T>,
    itemLabel: @Composable (T) -> String,
    onDismissRequest: () -> Unit,
    onValueChanged: (newIncluded: List<T>, newExcluded: List<T>) -> Unit,
) {
    val selected = remember {
        items
            .map {
                when (it) {
                    in initialChecked -> State.CHECKED
                    in initialInversed -> State.INVERSED
                    else -> State.UNCHECKED
                }
            }
            .toMutableStateList()
    }
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(Size.tiny),
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column {
                if (message != null) {
                    Text(text = message, modifier = Modifier.padding(bottom = Size.small))
                }

                Box {
                    val listState = rememberLazyListState()
                    LazyColumn(state = listState) {
                        itemsIndexed(
                            items = items,
                            key = { index, item -> "$index-${item.hashCode()}" },
                        ) { index, item ->
                            val state = selected[index]
                            Row(
                                modifier =
                                    Modifier.clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            selected[index] =
                                                when (state) {
                                                    State.UNCHECKED ->
                                                        if (negativeOnly) State.INVERSED
                                                        else State.CHECKED
                                                    State.CHECKED -> State.INVERSED
                                                    State.INVERSED -> State.UNCHECKED
                                                }
                                        }
                                        .defaultMinSize(minHeight = Size.huge)
                                        .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    modifier = Modifier.padding(end = Size.medium),
                                    imageVector =
                                        when (state) {
                                            State.UNCHECKED -> Icons.Rounded.CheckBoxOutlineBlank
                                            State.CHECKED -> Icons.Rounded.CheckBox
                                            State.INVERSED -> Icons.Rounded.DisabledByDefault
                                        },
                                    tint =
                                        if (state == State.UNCHECKED) {
                                            LocalContentColor.current
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                    contentDescription =
                                        stringResource(
                                            when (state) {
                                                State.UNCHECKED -> R.string.not_selected
                                                State.CHECKED -> R.string.selected
                                                State.INVERSED -> R.string.disabled
                                            }
                                        ),
                                )
                                Text(text = itemLabel(item))
                            }
                        }
                    }

                    if (!listState.isScrolledToStart()) {
                        HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                    }
                    if (!listState.isScrolledToEnd()) {
                        HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(text = stringResource(R.string.cancel)) }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val included =
                        items.mapIndexedNotNull { index, category ->
                            if (selected[index] == State.CHECKED) category else null
                        }
                    val excluded =
                        items.mapIndexedNotNull { index, category ->
                            if (selected[index] == State.INVERSED) category else null
                        }
                    onValueChanged(included, excluded)
                }
            ) {
                Text(text = stringResource(R.string.ok))
            }
        },
    )
}
