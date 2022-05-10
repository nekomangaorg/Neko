package eu.kanade.tachiyomi.ui.base.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mikepenz.iconics.compose.Image
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.kanade.tachiyomi.R

@Composable
fun EmptyView(
    iconicImage: IIcon,
    iconSize: Dp = 24.dp,
    @StringRes message: Int? = null,
    actions: List<Action> = emptyList(),
) {
    val iconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val top = maxHeight / 2
        Column(modifier = Modifier
            .fillMaxSize()
            .paddingFromBaseline(top = top),
            Arrangement.Top,
            Alignment.CenterHorizontally) {
            Image(asset = iconicImage,
                modifier = Modifier
                    .size(iconSize),
                colorFilter = ColorFilter.tint(iconColor))
            message?.let {
                Text(text = stringResource(id = message),
                    color = iconColor,
                    style = MaterialTheme.typography.bodyLarge)
            }
            CompositionLocalProvider(LocalRippleTheme provides CoverRippleTheme) {
                actions.forEach { action ->
                    Spacer(modifier = Modifier.size(16.dp))
                    TextButton(onClick = action.onClick) {
                        Text(text = stringResource(id = action.resId),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EmptyViewPreview() {
    EmptyView(
        iconicImage = CommunityMaterial.Icon.cmd_compass_off,
        iconSize = 72.dp,
        message = R.string.no_results_found,
        actions = listOf(Action(R.string.retry)))
}

data class Action(
    @StringRes val resId: Int,
    val onClick: () -> Unit = {},
)