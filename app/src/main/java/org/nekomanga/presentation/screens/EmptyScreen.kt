package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.components.UiText
import org.nekomanga.presentation.theme.Size

private val ErrorFaces =
    listOf(
        "(･o･;)",
        "Σ(ಠ_ಠ)",
        "ಥ_ಥ",
        "(˘･_･˘)",
        "(；￣Д￣)",
        "(･Д･。",
        "(╬ಠ益ಠ)",
        "(╥﹏╥)",
        "(⋟﹏⋞)",
        "Ò︵Ó",
        " ˙ᯅ˙)",
        "(¬_¬)",
        "(ノಠ益ಠ)ノ彡┻━┻",
        "(╯°□°）╯︵ ┻━┻",
        "(｡>﹏<｡)",
        "m(｡_｡)m",
        "(•́﹏•̀｡)",
        "ヾ(･`⌓´･)ﾉﾞ",
        "(；一_一)",
        "(-_-;)",
        "´¬`",
        "(ó﹏ò)",
        "(´-﹏-`)",
        "o(>﹏<)o",
        "ヾ(;ﾟДﾟ)ﾉ",
        "ヽ(｀⌒´メ)ノ",
        "(˚Д˚)",
        "(`皿´)",
    )

@Composable
fun EmptyScreen(
    message: UiText,
    actions: ImmutableList<Action> = persistentListOf(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val errorFace = remember { ErrorFaces.random() }
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = errorFace,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = Size.medium),
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        )

        Text(
            text = message.asString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium),
            textAlign = TextAlign.Center,
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        )

        actions.forEach { action ->
            TextButton(onClick = action.onClick) {
                Text(
                    text = action.text.asString(),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Preview
@Composable
private fun EmptyViewPreview() {
    EmptyScreen(
        message = UiText.StringResource(R.string.no_results_found),
        actions = persistentListOf(Action(UiText.StringResource(R.string.retry))),
    )
}

data class Action(val text: UiText, val onClick: () -> Unit = {})
