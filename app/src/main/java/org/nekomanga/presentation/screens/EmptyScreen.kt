package org.nekomanga.presentation.screens

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.nekomanga.R
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
    message: String,
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
            modifier = Modifier.padding(bottom = Size.medium),
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f),
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .45f),
        )

        actions.forEach { action ->
            TextButton(onClick = action.onClick) {
                Text(
                    text = stringResource(id = action.resId),
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
        message = stringResource(id = R.string.no_results_found),
        actions = persistentListOf(Action(R.string.retry)),
    )
}

data class Action(@StringRes val resId: Int, val onClick: () -> Unit = {})