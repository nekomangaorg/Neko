package org.nekomanga.presentation.components.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.ThemeColors
import org.nekomanga.presentation.theme.Shapes

@Composable
fun EditCategorySheet(themeColor: ThemeColors, cancelClick: () -> Unit, newCategoryClick: () -> Unit) {
    CompositionLocalProvider(LocalRippleTheme provides themeColor.rippleTheme) {

        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(Shapes.sheetRadius)) {
            val paddingModifier = Modifier.padding(horizontal = 8.dp)
            LazyColumn(
                modifier = Modifier
                    .navigationBarsPadding(),
            ) {

                item {
                    Gap(16.dp)
                    Row(modifier = paddingModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(modifier = paddingModifier, text = stringResource(id = R.string.move_x_to, stringResource(id = R.string.manga)), style = MaterialTheme.typography.titleLarge)
                        TextButton(modifier = paddingModifier, onClick = newCategoryClick) {
                            Text(text = stringResource(id = R.string.plus_new_category), style = MaterialTheme.typography.titleSmall.copy(color = themeColor.buttonColor))
                        }
                    }
                    Gap(16.dp)
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.veryLowContrast))
                }

                item {
                    Row(modifier = paddingModifier, verticalAlignment = Alignment.CenterVertically) {
                        var state = remember { mutableStateOf(true) }
                        Checkbox(
                            checked = state.value,
                            onCheckedChange = { newValue ->
                                state.value = newValue
                            },
                            colors = CheckboxDefaults.colors(checkedColor = themeColor.buttonColor, checkmarkColor = MaterialTheme.colorScheme.surface),
                        )
                        Gap(4.dp)
                        Text("Text")
                    }
                }

                item {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.veryLowContrast))
                    Gap(4.dp)
                    Row(modifier = paddingModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = cancelClick, colors = ButtonDefaults.textButtonColors(contentColor = themeColor.buttonColor)) {
                            Text(text = stringResource(id = R.string.cancel), style = MaterialTheme.typography.titleSmall)
                        }
                        ElevatedButton(onClick = cancelClick, colors = ButtonDefaults.elevatedButtonColors(containerColor = themeColor.buttonColor)) {
                            Text(text = stringResource(id = R.string.add_to_), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.surface)
                        }
                    }
                    Gap(16.dp)
                }
            }
        }
    }
}
