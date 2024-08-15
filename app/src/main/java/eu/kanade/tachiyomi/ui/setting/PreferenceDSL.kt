package eu.kanade.tachiyomi.ui.setting

import android.app.Activity
import android.graphics.drawable.Drawable
import androidx.annotation.StringRes
import androidx.biometric.BiometricPrompt
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.DialogPreference
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.mikepenz.iconics.IconicsDrawable
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.isAuthenticationSupported
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.startAuthentication
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.widget.preference.IntListMatPreference
import eu.kanade.tachiyomi.widget.preference.ListMatPreference
import eu.kanade.tachiyomi.widget.preference.MultiListMatPreference
import eu.kanade.tachiyomi.widget.preference.TriStateListPreference
import org.nekomanga.R

@DslMarker @Target(AnnotationTarget.TYPE) annotation class DSL

inline fun PreferenceManager.newScreen(
    block: (@DSL PreferenceScreen).() -> Unit
): PreferenceScreen {
    return createPreferenceScreen(context).also { it.block() }
}

inline fun PreferenceGroup.preference(block: (@DSL Preference).() -> Unit): Preference {
    return initThenAdd(Preference(context), block)
}

inline fun PreferenceGroup.themePreference(
    block: (@DSL ThemePreference).() -> Unit
): ThemePreference {
    return initThenAdd(ThemePreference(context), block)
}

inline fun PreferenceGroup.switchPreference(
    block: (@DSL SwitchPreferenceCompat).() -> Unit
): SwitchPreferenceCompat {
    return initThenAdd(SwitchPreferenceCompat(context), block)
}

inline fun PreferenceGroup.checkBoxPreference(
    block: (@DSL CheckBoxPreference).() -> Unit
): CheckBoxPreference {
    return initThenAdd(CheckBoxPreference(context), block)
}

inline fun PreferenceGroup.editTextPreference(
    block: (@DSL EditTextPreference).() -> Unit
): EditTextPreference {
    return initThenAdd(EditTextPreference(context), block).also(::initDialog)
}

inline fun PreferenceGroup.dropDownPreference(
    block: (@DSL DropDownPreference).() -> Unit
): DropDownPreference {
    return initThenAdd(DropDownPreference(context), block).also(::initDialog)
}

inline fun PreferenceGroup.listPreference(
    activity: Activity?,
    block: (@DSL ListMatPreference).() -> Unit,
): ListMatPreference {
    return initThenAdd(ListMatPreference(activity, context), block)
}

inline fun PreferenceGroup.intListPreference(
    activity: Activity?,
    block: (@DSL IntListMatPreference).() -> Unit,
): IntListMatPreference {
    return initThenAdd(IntListMatPreference(activity, context), block)
}

inline fun PreferenceGroup.multiSelectListPreferenceMat(
    activity: Activity?,
    block: (@DSL MultiListMatPreference).() -> Unit,
): MultiListMatPreference {
    return initThenAdd(MultiListMatPreference(activity, context), block)
}

inline fun PreferenceGroup.triStateListPreference(
    activity: Activity?,
    block: (@DSL TriStateListPreference).() -> Unit,
): TriStateListPreference {
    return initThenAdd(TriStateListPreference(activity, context), block)
}

inline fun PreferenceScreen.preferenceCategory(
    block: (@DSL PreferenceCategory).() -> Unit
): PreferenceCategory {
    return addThenInit(
        PreferenceCategory(context).apply { isIconSpaceReserved = false },
        block,
    )
}

inline fun PreferenceScreen.switchPreference(
    block: (@DSL SwitchPreferenceCompat).() -> Unit
): SwitchPreferenceCompat {
    return initThenAdd(SwitchPreferenceCompat(context), block)
}

fun PreferenceGroup.infoPreference(@StringRes infoRes: Int): Preference {
    return initThenAdd(
        Preference(context),
        {
            iconRes = R.drawable.ic_info_outline_24dp
            iconTint = context.getResourceColor(android.R.attr.textColorSecondary)
            summaryRes = infoRes
            isSelectable = false
        },
    )
}

inline fun PreferenceScreen.preferenceScreen(
    block: (@DSL PreferenceScreen).() -> Unit
): PreferenceScreen {
    return addThenInit(preferenceManager.createPreferenceScreen(context), block)
}

fun initDialog(dialogPreference: DialogPreference) {
    with(dialogPreference) {
        if (dialogTitle == null) {
            dialogTitle = title
        }
    }
}

fun <P : Preference> PreferenceGroup.add(p: P): P {
    return p.apply {
        this.isIconSpaceReserved = false
        this.isSingleLineTitle = false
        addPreference(this)
    }
}

inline fun <P : Preference> PreferenceGroup.initThenAdd(p: P, block: P.() -> Unit): P {
    return p.apply {
        this.isIconSpaceReserved = false
        this.isSingleLineTitle = false
        block()
        addPreference(this)
    }
}

inline fun <P : Preference> PreferenceGroup.addThenInit(p: P, block: P.() -> Unit): P {
    return p.apply {
        this.isIconSpaceReserved = false
        this.isSingleLineTitle = false
        addPreference(this)
        block()
    }
}

inline fun Preference.onClick(crossinline block: () -> Unit) {
    setOnPreferenceClickListener {
        block()
        true
    }
}

inline fun Preference.onChange(crossinline block: (Any?) -> Boolean) {
    setOnPreferenceChangeListener { _, newValue -> block(newValue) }
}

fun <T> Preference.bindTo(preference: tachiyomi.core.preference.Preference<T>) {
    key = preference.key()
    defaultValue = preference.defaultValue()
}

fun <T> ListPreference.bindTo(preference: tachiyomi.core.preference.Preference<T>) {
    key = preference.key()
    defaultValue = preference.defaultValue()
}

fun <T> ListMatPreference.bindTo(preference: tachiyomi.core.preference.Preference<T>) {
    key = preference.key()
    val defValue = preference.defaultValue()
    defaultValue = if (defValue is Set<*>) defValue else defValue.toString()
}

@Deprecated(
    "Do not bind tri-states prefs with a single preference",
    ReplaceWith("bindTo(preference, excludePreference = )"),
    DeprecationLevel.ERROR,
)
fun <T> TriStateListPreference.bindTo(preference: tachiyomi.core.preference.Preference<T>) {
    key = preference.key()
}

fun TriStateListPreference.bindTo(
    includePreference: tachiyomi.core.preference.Preference<Set<String>>,
    excludePreference: tachiyomi.core.preference.Preference<Set<String>>,
) {
    key = includePreference.key()
    excludeKey = excludePreference.key()
    defaultValue = includePreference.defaultValue() to excludePreference.defaultValue()
}

fun <T> IntListMatPreference.bindTo(preference: tachiyomi.core.preference.Preference<T>) {
    key = preference.key()
    defaultValue = preference.defaultValue()
}

fun SwitchPreferenceCompat.requireAuthentication(
    activity: FragmentActivity?,
    title: String,
    subtitle: String? = null,
    confirmationRequired: Boolean = true,
) {
    onPreferenceChangeListener =
        Preference.OnPreferenceChangeListener { _, newValue ->
            newValue as Boolean
            if (activity != null && context.isAuthenticationSupported()) {
                activity.startAuthentication(
                    title,
                    subtitle,
                    confirmationRequired,
                    callback =
                        object : AuthenticatorUtil.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(
                                result: BiometricPrompt.AuthenticationResult
                            ) {
                                super.onAuthenticationSucceeded(result)
                                isChecked = newValue
                            }

                            override fun onAuthenticationError(
                                errorCode: Int,
                                errString: CharSequence
                            ) {
                                super.onAuthenticationError(errorCode, errString)
                                activity.toast(errString.toString())
                            }
                        },
                )
                false
            } else {
                true
            }
        }
}

var Preference.defaultValue: Any?
    get() = null // set only
    set(value) {
        setDefaultValue(value)
    }

var Preference.titleRes: Int
    get() = 0 // set only
    set(value) {
        setTitle(value)
    }

var Preference.iconRes: Int
    get() = 0 // set only
    set(value) {
        icon = VectorDrawableCompat.create(context.resources, value, context.theme)
    }

var Preference.summaryRes: Int
    get() = 0 // set only
    set(value) {
        setSummary(value)
    }

var Preference.iconTint: Int
    get() = 0 // set only
    set(value) {
        DrawableCompat.setTint(icon!!, value)
    }

var Preference.iconDrawable: Drawable
    get() = IconicsDrawable(context) // set only
    set(value) {
        icon = value
    }
