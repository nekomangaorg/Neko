package org.nekomanga.presentation.components.sheets

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Category
import jp.wasabeef.gap.Gap
import org.nekomanga.presentation.components.NekoColors
import org.nekomanga.presentation.screens.ThemeColors
import java.util.Locale

@Composable
fun EditCategorySheet(
    addingToLibrary: Boolean,
    categories: List<Category>,
    mangaCategories: List<Category>,
    themeColor: ThemeColors,
    cancelClick: () -> Unit,
    newCategoryClick: () -> Unit,
    confirmClicked: (List<Category>) -> Unit,
    addToLibraryClick: () -> Unit,
) {
    CompositionLocalProvider(LocalRippleTheme provides themeColor.rippleTheme) {

        val context = LocalContext.current

        val enabledCategories = remember { mangaCategories.associateBy { it.id!! }.toMutableMap() }
        val acceptText = remember { mutableStateOf(calculateText(context, mangaCategories, enabledCategories, addingToLibrary)) }

        val maxLazyHeight = LocalConfiguration.current.screenHeightDp * .5

        BaseSheet(themeColor = themeColor) {

            val paddingModifier = Modifier.padding(horizontal = 8.dp)
            Column(
                modifier = Modifier
                    .navigationBarsPadding(),
            ) {

                Gap(16.dp)
                Row(modifier = paddingModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val prefix = if (addingToLibrary) R.string.add_x_to else R.string.move_x_to
                    Text(modifier = paddingModifier, text = stringResource(id = prefix, stringResource(id = R.string.manga)), style = MaterialTheme.typography.titleLarge)
                    TextButton(modifier = paddingModifier, onClick = newCategoryClick) {
                        Text(text = stringResource(id = R.string.plus_new_category), style = MaterialTheme.typography.titleSmall.copy(color = themeColor.buttonColor))
                    }
                }
                Gap(16.dp)
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.veryLowContrast))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeightIn(0.dp, maxLazyHeight.dp),
                ) {
                    items(categories) { category: Category ->
                        Row(modifier = paddingModifier, verticalAlignment = Alignment.CenterVertically) {
                            var state by remember { mutableStateOf(enabledCategories.contains(category.id)) }
                            Checkbox(
                                checked = state,
                                onCheckedChange = {
                                    if (it) {
                                        enabledCategories[category.id!!] = category
                                    } else {
                                        enabledCategories.remove(category.id)
                                    }
                                    state = it
                                    acceptText.value = calculateText(context, mangaCategories, enabledCategories, addingToLibrary)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = themeColor.buttonColor, checkmarkColor = MaterialTheme.colorScheme.surface),
                            )
                            Gap(4.dp)
                            Text(text = category.name)
                        }

                    }
                }


                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = NekoColors.veryLowContrast))
                Gap(4.dp)
                Row(modifier = paddingModifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = cancelClick, colors = ButtonDefaults.textButtonColors(contentColor = themeColor.buttonColor)) {
                        Text(text = stringResource(id = R.string.cancel), style = MaterialTheme.typography.titleSmall)
                    }
                    ElevatedButton(
                        onClick = {
                            confirmClicked(enabledCategories.values.toList())
                            addToLibraryClick()
                            cancelClick()
                        },
                        colors = ButtonDefaults.elevatedButtonColors(containerColor = themeColor.buttonColor),
                    ) {

                        Text(text = acceptText.value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.surface)
                    }
                }
                Gap(16.dp)
            }
        }
    }
}

private fun calculateText(context: Context, initialMangaCategories: List<Category>, currentlySelectedCategories: Map<Int, Category>, addingToLibrary: Boolean): String {
    val initialIds = initialMangaCategories.map { it.id!! }.toSet()
    val same = currentlySelectedCategories.filter { initialIds.contains(it.key) }.values.toList()
    val difference = currentlySelectedCategories.filterNot { initialIds.contains(it.key) }.values.toList()

    val addingMore = initialMangaCategories.isNotEmpty() && difference.isNotEmpty() && same.size == initialMangaCategories.size
    val nothingChanged = difference.isEmpty() && same.size == initialIds.size
    val removing = same.size < initialIds.size && difference.isEmpty()

    fun quantity(size: Int) = context.resources.getQuantityString(R.plurals.category_plural, size, size)

    return context.getString(
        when {
            addingToLibrary || (addingMore && !nothingChanged) -> R.string.add_to_
            removing -> R.string.remove_from_
            nothingChanged -> R.string.keep_in_
            else -> R.string.move_to_
        },
        when {
            same.size == 1 && nothingChanged -> same.first().name
            difference.isEmpty() && initialIds.isNotEmpty() && initialIds.size == same.size -> quantity(same.size)
            difference.isEmpty() && same.isEmpty() && initialIds.isNotEmpty() -> quantity(initialIds.size)
            same.isEmpty() && difference.isEmpty() -> context.getString(R.string.default_category).lowercase(Locale.ROOT)
            difference.size == 1 && nothingChanged.not() -> difference.first().name
            same.size == 1 && nothingChanged.not() && same.size == initialIds.size -> same.first().name
            difference.size > 1 -> context.resources.getQuantityString(
                R.plurals.category_plural,
                difference.size,
                difference.size,
            )
            else -> context.resources.getQuantityString(
                R.plurals.category_plural,
                initialIds.size - same.size,
                initialIds.size - same.size,
            )
        },
    )
}
