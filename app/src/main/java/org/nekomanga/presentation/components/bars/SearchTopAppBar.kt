package org.nekomanga.presentation.components.bars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.zIndex
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.FlexibleTopBar
import org.nekomanga.presentation.components.FlexibleTopBarColors
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.theme.Size

@Composable
fun SearchTopAppBar(
    onSearchText: (String?) -> Unit,
    searchPlaceHolder: String,
    color: Color,
    navigationIconLabel: String,
    navigationIcon: ImageVector,
    incognitoMode: Boolean = false,
    onNavigationIconClicked: () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var showTextField by rememberSaveable { mutableStateOf(false) }
    var alreadyRequestedFocus by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    FlexibleTopBar(
        scrollBehavior = scrollBehavior,
        colors =
            FlexibleTopBarColors(containerColor = color, scrolledContainerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Size.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().heightIn(Size.appBarHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToolTipButton(
                        toolTipLabel = navigationIconLabel,
                        icon = navigationIcon,
                        onClick = onNavigationIconClicked,
                    )
                    if (incognitoMode) {
                        Gap(Size.small)
                        Image(
                            CommunityMaterial.Icon2.cmd_incognito_circle,
                            colorFilter = ColorFilter.tint(LocalContentColor.current),
                            modifier = Modifier.size(Size.extraLarge).zIndex(1f),
                        )
                        Gap(Size.small)
                    }
                }
                if (showTextField) {
                    OutlinedTextField(
                        modifier =
                            Modifier.weight(1f)
                                .padding(horizontal = Size.small)
                                .focusRequester(focusRequester),
                        value = searchText,
                        placeholder = {
                            Text(text = stringResource(id = R.string.search_chapters))
                        },
                        onValueChange = {
                            searchText = it
                            onSearchText(it)
                        },
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                cursorColor =
                                    LocalContentColor.current.copy(
                                        alpha = LocalContentAlpha.current
                                    ),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = searchText.isNotBlank(),
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                ToolTipButton(
                                    toolTipLabel = stringResource(id = R.string.clear),
                                    icon = Icons.Filled.Close,
                                    onClick = {
                                        onSearchText("")
                                        searchText = ""
                                    },
                                )
                            }
                        },
                        maxLines = 1,
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchText(searchText) }),
                    )
                    LaunchedEffect(Unit) {
                        if (!alreadyRequestedFocus) {
                            focusRequester.requestFocus()
                            alreadyRequestedFocus = true
                        }
                        if (searchText.isNotBlank()) {
                            onSearchText(searchText)
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon =
                        when (showTextField) {
                            true -> Icons.Filled.SearchOff
                            false -> Icons.Filled.Search
                        }
                    ToolTipButton(
                        toolTipLabel = searchPlaceHolder,
                        icon = icon,
                        onClick = {
                            searchText = ""
                            alreadyRequestedFocus = false
                            onSearchText(null)
                            showTextField = !showTextField
                        },
                    )
                    actions()
                }
            }
        }
    }
}
