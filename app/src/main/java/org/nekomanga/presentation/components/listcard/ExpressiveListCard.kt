package org.nekomanga.presentation.components.listcard

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.nekomanga.presentation.theme.Size

enum class ListCardType {
    Top,
    Center,
    Bottom,
    Single,
}

@Composable
fun ExpressiveListCard(
    modifier: Modifier = Modifier,
    listCardType: ListCardType,
    content: @Composable () -> Unit,
) {
    val shape =
        remember(listCardType) {
            when (listCardType) {
                ListCardType.Top ->
                    RoundedCornerShape(
                        topStart = Size.medium,
                        topEnd = Size.medium,
                        bottomEnd = Size.tiny,
                        bottomStart = Size.tiny,
                    )
                ListCardType.Center ->
                    RoundedCornerShape(
                        topStart = Size.tiny,
                        topEnd = Size.tiny,
                        bottomEnd = Size.tiny,
                        bottomStart = Size.tiny,
                    )
                ListCardType.Single -> {
                    RoundedCornerShape(Size.medium)
                }
                ListCardType.Bottom ->
                    RoundedCornerShape(
                        topStart = Size.tiny,
                        topEnd = Size.tiny,
                        bottomEnd = Size.medium,
                        bottomStart = Size.medium,
                    )
            }
        }

    ElevatedCard(modifier = modifier, shape = shape) { content() }
}
