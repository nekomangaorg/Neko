package org.nekomanga.presentation.components.bars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import jp.wasabeef.gap.Gap
import org.nekomanga.R
import org.nekomanga.presentation.components.FlexibleTopBar
import org.nekomanga.presentation.components.FlexibleTopBarColors
import org.nekomanga.presentation.components.ToolTipButton
import org.nekomanga.presentation.theme.Size

@Composable
fun SearchOutlineTopAppBar(
    onSearch: (String?) -> Unit = {},
    searchPlaceHolder: String = "",
    searchPlaceHolderAlt: String = "",
    initialSearch: String = "",
    color: Color,
    navigationEnabled: Boolean = false,
    navigationIconLabel: String? = null,
    navigationIcon: ImageVector? = null,
    incognitoMode: Boolean,
    onNavigationIconClicked: () -> Unit = {},
    onSearchDisabled: () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    underHeaderActions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val focusRequester = remember { FocusRequester() }

    var searchText by rememberSaveable { mutableStateOf("") }
    var searchEnabled by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialSearch) {
        if (initialSearch.isNotEmpty() && searchText != initialSearch) {
            searchText = initialSearch
            searchEnabled = true
        }
    }

    val focusManager = LocalFocusManager.current
    FlexibleTopBar(
        scrollBehavior = scrollBehavior,
        colors = FlexibleTopBarColors(containerColor = color, scrolledContainerColor = color),
    ) {
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            SearchBar(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = Size.small)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                searchEnabled = true
                            }
                        }
                        .focusRequester(focusRequester),
                expanded = false,
                onExpandedChange = {},
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchText,
                        expanded = false,
                        onExpandedChange = {},
                        onQueryChange = {
                            searchText = it
                            onSearch(it)
                        },
                        onSearch = { onSearch(it) },
                        placeholder = {
                            Text(
                                text =
                                    if (searchEnabled && searchPlaceHolderAlt.isNotEmpty())
                                        searchPlaceHolderAlt
                                    else searchPlaceHolder,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (navigationEnabled) {
                                    ToolTipButton(
                                        toolTipLabel = navigationIconLabel!!,
                                        icon = navigationIcon,
                                        onClick = onNavigationIconClicked,
                                    )
                                }
                                if (searchEnabled) {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.cancel_search),
                                        icon = Icons.Filled.SearchOff,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        onClick = {
                                            onSearch("")
                                            searchText = ""
                                            searchEnabled = false
                                            focusManager.clearFocus()
                                            onSearchDisabled()
                                        },
                                    )
                                } else {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.search),
                                        icon = Icons.Filled.Search,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        onClick = { searchEnabled = true },
                                    )
                                }
                                if (incognitoMode) {
                                    Gap(Size.small)
                                    Image(
                                        CommunityMaterial.Icon2.cmd_incognito_circle,
                                        colorFilter = ColorFilter.tint(LocalContentColor.current),
                                        modifier = Modifier.size(Size.extraLarge),
                                    )
                                    Gap(Size.small)
                                }
                            }
                        },
                        trailingIcon = {
                            Row {
                                AnimatedVisibility(
                                    visible = searchText.isNotBlank(),
                                    enter = fadeIn(),
                                    exit = fadeOut(),
                                ) {
                                    ToolTipButton(
                                        toolTipLabel = stringResource(id = R.string.clear),
                                        icon = Icons.Filled.Close,
                                        enabledTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        onClick = {
                                            onSearch("")
                                            searchText = ""
                                        },
                                    )
                                }
                                if (!searchEnabled) {
                                    actions()
                                }
                            }
                        },
                    )
                },
                content = {},
            )
            if (underHeaderActions != {}) {
                Gap(Size.tiny)
                underHeaderActions()
            }
        }
    }
}
