package org.nekomanga.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.nekomanga.presentation.extensions.collectAsState
import org.nekomanga.presentation.screens.settings.widgets.InfoWidget
import org.nekomanga.presentation.screens.settings.widgets.ListPreferenceWidget
import org.nekomanga.presentation.screens.settings.widgets.MultiSelectListPreferenceWidget
import org.nekomanga.presentation.screens.settings.widgets.SitePreferenceWidget
import org.nekomanga.presentation.screens.settings.widgets.SwitchPreferenceWidget
import org.nekomanga.presentation.screens.settings.widgets.TextPreferenceWidget
import org.nekomanga.presentation.theme.Size

val LocalPreferenceHighlighted = compositionLocalOf(structuralEqualityPolicy()) { false }
val LocalPreferenceMinHeight = compositionLocalOf(structuralEqualityPolicy()) { 56.dp }

@Composable
fun StatusWrapper(
    item: Preference.PreferenceItem<*>,
    highlightKey: String?,
    content: @Composable () -> Unit,
) {
    val enabled = item.enabled
    val highlighted = item.title == highlightKey
    AnimatedVisibility(
        visible = enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        content = {
            CompositionLocalProvider(
                LocalPreferenceHighlighted provides highlighted,
                content = content,
            )
        },
    )
}

@Composable
internal fun PreferenceItem(item: Preference.PreferenceItem<*>, highlightKey: String?) {
    val scope = rememberCoroutineScope()
    StatusWrapper(item = item, highlightKey = highlightKey) {
        when (item) {
            is Preference.PreferenceItem.SwitchPreference -> {
                val value by item.pref.collectAsState()
                SwitchPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    checked = value,
                    onCheckedChanged = { newValue ->
                        scope.launch {
                            if (item.onValueChanged(newValue)) {
                                item.pref.set(newValue)
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.SliderPreference -> {
                /*SliderItem(
                    label = item.title,
                    min = item.min,
                    max = item.max,
                    steps = item.steps,
                    value = item.value,
                    valueText =
                        item.subtitle.takeUnless { it.isNullOrEmpty() } ?: item.value.toString(),
                    onChange = { scope.launch { item.onValueChanged(it) } },
                    labelStyle = MaterialTheme.typography.titleLarge,
                )*/
            }
            is Preference.PreferenceItem.ListPreference<*> -> {
                val value by item.pref.collectAsState()
                ListPreferenceWidget(
                    value = value,
                    title = item.title,
                    subtitle = item.internalSubtitleProvider(value, item.entries),
                    icon = item.icon,
                    entries = item.entries,
                    onValueChange = { newValue ->
                        scope.launch {
                            if (item.internalOnValueChanged(newValue!!)) {
                                item.internalSet(newValue)
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.BasicListPreference -> {
                /*ListPreferenceWidget(
                    value = item.value,
                    title = item.title,
                    subtitle = item.subtitleProvider(item.value, item.entries),
                    icon = item.icon,
                    entries = item.entries,
                    onValueChange = { scope.launch { item.onValueChanged(it) } },
                )*/
            }
            is Preference.PreferenceItem.MultiSelectListPreference -> {
                val values by item.pref.collectAsState()
                MultiSelectListPreferenceWidget(
                    preference = item,
                    values = values,
                    onValuesChange = { newValues ->
                        scope.launch {
                            if (item.onValueChanged(newValues)) {
                                item.pref.set(newValues.toMutableSet())
                            }
                        }
                    },
                )
            }
            is Preference.PreferenceItem.TextPreference -> {
                TextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    onPreferenceClick = item.onClick,
                )
            }
            is Preference.PreferenceItem.EditTextPreference -> {
                /*val values by item.pref.collectAsState()
                EditTextPreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    value = values,
                    onConfirm = {
                        val accepted = item.onValueChanged(it)
                        if (accepted) item.pref.set(it)
                        accepted
                    },
                )*/
            }
            is Preference.PreferenceItem.TrackerPreference -> {
                /*  val isLoggedIn by
                    item.tracker.let { tracker ->
                        tracker.isLoggedInFlow.collectAsState(tracker.isLoggedIn)
                    }
                TrackingPreferenceWidget(
                    tracker = item.tracker,
                    checked = isLoggedIn,
                    onClick = { if (isLoggedIn) item.logout() else item.login() },
                )*/
            }
            is Preference.PreferenceItem.SitePreference -> {
                SitePreferenceWidget(
                    title = item.title,
                    subtitle = item.subtitle,
                    loggedIn = item.isLoggedIn,
                    onClick = { if (item.isLoggedIn) item.logout() else item.login() },
                )
            }
            is Preference.PreferenceItem.InfoPreference -> {
                InfoWidget(text = item.title)
            }
            is Preference.PreferenceItem.CustomPreference -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (item.title.isNotEmpty()) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier =
                                Modifier.fillMaxWidth()
                                    .padding(
                                        start = Size.medium,
                                        bottom = Size.small,
                                        top = Size.medium,
                                    ),
                        ) {
                            Text(
                                text = item.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Normal
                                    ),
                            )
                        }
                    }
                    item.content()
                }
            }
        }
    }
}
