package org.nekomanga.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import org.nekomanga.R
import org.nekomanga.presentation.components.UiText
import tachiyomi.core.preference.Preference as PreferenceData

sealed class Preference {
    abstract val title: UiText
    abstract val enabled: Boolean

    sealed class PreferenceItem<T> : Preference() {
        abstract val subtitle: UiText?
        abstract val icon: ImageVector?
        abstract val onValueChanged: suspend (newValue: T) -> Boolean

        /** A basic [PreferenceItem] that only displays texts. */
        data class TextPreference(
            override val title: UiText,
            override val subtitle: UiText? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },
            val onClick: (() -> Unit)? = null,
        ) : PreferenceItem<String>()

        /** A [PreferenceItem] that provides a two-state toggleable option. */
        data class SwitchPreference(
            val pref: PreferenceData<Boolean>,
            override val title: UiText,
            override val subtitle: UiText? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Boolean) -> Boolean = { true },
        ) : PreferenceItem<Boolean>()

        /** A [PreferenceItem] that provides a slider to select an integer number. */
        data class SliderPreference(
            val value: Int,
            val max: Int,
            val min: Int = 0,
            val steps: Int = 0,
            override val title: UiText = UiText.String(""),
            override val subtitle: UiText? = null,
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Int) -> Boolean = { true },
        ) : PreferenceItem<Int>()

        /** A [PreferenceItem] that displays a list of entries as a dialog. */
        @Suppress("UNCHECKED_CAST")
        data class ListPreference<T>(
            val pref: PreferenceData<T>,
            override val title: UiText,
            override val subtitle: UiText? = UiText.String("%s"),
            val subtitleProvider:
                @Composable
                (value: T, entries: ImmutableMap<T, UiText>) -> String? =
                { v, e ->
                    subtitle?.asString()?.format(e[v]?.asString())
                },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: T) -> Boolean = { true },
            val entries: ImmutableMap<T, UiText>,
        ) : PreferenceItem<T>() {
            internal fun internalSet(newValue: Any) = pref.set(newValue as T)

            internal suspend fun internalOnValueChanged(newValue: Any) =
                onValueChanged(newValue as T)

            @Composable
            internal fun internalSubtitleProvider(
                value: Any?,
                entries: ImmutableMap<out Any?, UiText>,
            ) = subtitleProvider(value as T, entries as ImmutableMap<T, UiText>)
        }

        /** [ListPreference] but with no connection to a [PreferenceData] */
        data class BasicListPreference(
            val value: String,
            override val title: UiText,
            override val subtitle: UiText? = UiText.String("%s"),
            val subtitleProvider:
                @Composable
                (value: String, entries: ImmutableMap<String, UiText>) -> String? =
                { v, e ->
                    subtitle?.asString()?.format(e[v]?.asString())
                },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },
            val entries: ImmutableMap<String, String>,
        ) : PreferenceItem<String>()

        /**
         * A [PreferenceItem] that displays a list of entries as a dialog. Multiple entries can be
         * selected at the same time.
         */
        data class MultiSelectListPreference(
            val pref: PreferenceData<Set<String>>,
            override val title: UiText,
            override val subtitle: UiText? = UiText.String("%s"),
            val subtitleProvider:
                @Composable
                (value: Set<String>, entries: ImmutableMap<String, String>) -> String? =
                { v, e ->
                    val combined =
                        remember(v) { v.map { e[it] }.takeIf { it.isNotEmpty() }?.joinToString() }
                            ?: stringResource(R.string.none)
                    subtitle?.asString()?.format(combined)
                },
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: Set<String>) -> Boolean = { true },
            val entries: ImmutableMap<String, String>,
        ) : PreferenceItem<Set<String>>()

        /** A [PreferenceItem] that shows a EditText in the dialog. */
        data class EditTextPreference(
            val pref: PreferenceData<String>,
            override val title: UiText,
            override val subtitle: UiText? = UiText.String("%s"),
            override val icon: ImageVector? = null,
            override val enabled: Boolean = true,
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true },
        ) : PreferenceItem<String>()

        /** A [PreferenceItem] for individual tracker. */
        data class TrackerPreference(
            // val tracker: Tracker,
            override val title: UiText,
            val login: () -> Unit,
            val logout: () -> Unit,
        ) : PreferenceItem<String>() {
            override val enabled: Boolean = true
            override val subtitle: UiText? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true }
        }

        data class InfoPreference(override val title: UiText) : PreferenceItem<String>() {
            override val enabled: Boolean = true
            override val subtitle: UiText? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (newValue: String) -> Boolean = { true }
        }

        data class CustomPreference(
            override val title: UiText,
            val content: @Composable () -> Unit,
        ) : PreferenceItem<Unit>() {
            override val enabled: Boolean = true
            override val subtitle: UiText? = null
            override val icon: ImageVector? = null
            override val onValueChanged: suspend (newValue: Unit) -> Boolean = { true }
        }
    }

    data class PreferenceGroup(
        override val title: UiText,
        override val enabled: Boolean = true,
        val preferenceItems: ImmutableList<PreferenceItem<out Any>>,
    ) : Preference()
}
