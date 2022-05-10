package eu.kanade.tachiyomi.ui.base.components

import TooltipBox
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.components.theme.NekoTheme
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
                        TooltipBox(toolTipLabel = stringResource(id = R.string.back),
                            icon = Icons.Filled.ArrowBack,
                            buttonClicked = onBack)
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
    when (isList.not()) {
        true -> TooltipBox(
            toolTipLabel = stringResource(id = R.string.display_as_, "list"),
            icon = Icons.Filled.List,
            buttonClicked = buttonClicked)

        false -> TooltipBox(
            toolTipLabel = stringResource(id = R.string.display_as_, "grid"),
            icon = Icons.Filled.ViewModule,
            buttonClicked = buttonClicked)
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
fun getTopAppBarColor(): Color {
    return MaterialTheme.colorScheme.surface.copy(alpha = .7f)
}
