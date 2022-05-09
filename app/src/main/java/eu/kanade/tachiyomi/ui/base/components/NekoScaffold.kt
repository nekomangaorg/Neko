package eu.kanade.tachiyomi.ui.base.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.components.theme.Typefaces

@Composable
fun NekoScaffold(
    @StringRes title: Int,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit = {},
) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colorScheme.surface.luminance() > .5
    val color = getTopAppBarColor()
    SideEffect {
        systemUiController.setStatusBarColor(color, darkIcons = useDarkIcons)
    }
    val scrollBehavior = remember { TopAppBarDefaults.enterAlwaysScrollBehavior() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar =
        {
            CompositionLocalProvider(LocalRippleTheme provides CoverRippleTheme) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = getTopAppBarColor(),
                        scrolledContainerColor = getTopAppBarColor()),
                    modifier = Modifier
                        .statusBarsPadding(),
                    title = {
                        Text(text = stringResource(id = title),
                            style = TextStyle(
                                fontFamily = Typefaces.montserrat,
                                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                                fontWeight = FontWeight.Normal))
                    },
                    navigationIcon = {
                        IconButton(onClick = { onBack() }) {
                            Icon(imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back))
                        }

                    },
                    actions = actions,
                    scrollBehavior = scrollBehavior
                )
            }
        })
    { paddingValues ->
        content(paddingValues)
    }
}

@Composable
fun ListGridActionButton(isList: Boolean, buttonClicked: () -> Unit) {
    IconButton(onClick = { buttonClicked() }) {
        Icon(
            imageVector = if (isList.not()) {
                Icons.Filled.ViewList
            } else {
                Icons.Filled.ViewModule
            },
            tint = MaterialTheme.colorScheme.onSurface,
            contentDescription = stringResource(id = R.string.display_as)
        )
    }
}

@Composable
fun getTopAppBarColor(): Color {
    return MaterialTheme.colorScheme.surface.copy(alpha = .7f)
}
