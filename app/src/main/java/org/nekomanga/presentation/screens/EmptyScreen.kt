package org.nekomanga.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import jp.wasabeef.gap.Gap
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
        "(；ﾟДﾟ)",
        "ヾ(;ﾟДﾟ)ﾉ",
        "ヽ(｀⌒´メ)ノ",
        "(˚Д˚)",
        "(`皿´)",
        "Σ（ﾟдﾟlll)",
        "(・・)？",
        "(・・。)ゞ",
        "(ﾟдﾟ；)",
        "(´-ω-`)",
        "(╬ ಠ 益 ಠ )",
        "( ˘･з･)",
        "(ノ｀Д´)ノ",
        "(｀⌒´メ)",
        "(ง •̀_•́)ง",
        "(￣^￣)ゞ",
        "(；´д｀)ゞ",
        "(T_T)",
        "(._.)",
        "( ˘•ω•˘ )",
        "(っ´-`c)",
        "(´＿｀。)",
    )

@Composable
fun EmptyScreen(
    message: UiText,
    modifier: Modifier = Modifier.fillMaxSize(),
    actions: List<Action> = listOf(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val errorFace = remember { ErrorFaces.random() }
    Column(
        modifier = modifier.padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = errorFace,
            style = MaterialTheme.typography.displayMedium,
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
            modifier = Modifier.clearAndSetSemantics {},
        )
        Gap(Size.large)

        Text(
            text = message.asString(),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Size.medium),
            textAlign = TextAlign.Center,
            color =
                MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.mediumAlphaLowContrast),
        )
        Gap(Size.large)

        actions.forEach { action ->
            ElevatedButton(onClick = action.onClick) { Text(text = action.text.asString()) }
            Gap(Size.small)
        }
    }
}

data class Action(val text: UiText, val onClick: () -> Unit = {})
