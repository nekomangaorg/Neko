package eu.kanade.tachiyomi.util

import android.content.SharedPreferences
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import com.fredporciuncula.flow.preferences.Preference
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener

inline fun <reified T> SharedPreferences.getItem(key: String, default: T): T {
    @Suppress("UNCHECKED_CAST")
    return when (default) {
        is String -> getString(key, default) as T
        is Int -> getInt(key, default) as T
        is Long -> getLong(key, default) as T
        is Boolean -> getBoolean(key, default) as T
        is Float -> getFloat(key, default) as T
        is Set<*> -> getStringSet(key, default as Set<String>) as T
        is MutableSet<*> -> getStringSet(key, default as MutableSet<String>) as T
        else -> throw IllegalArgumentException("Generic type not handled: ${T::class.java.name}")
    }
}

/**
 * Binds a checkbox or switch view with a boolean preference.
 */
fun CompoundButton.bindToPreference(pref: Preference<Boolean>, block: ((Boolean) -> Unit)? = null) {
    setOnCheckedChangeListener { _, _ -> }
    isChecked = pref.get()
    setOnCheckedChangeListener { _, isChecked ->
        pref.set(isChecked)
        block?.invoke(isChecked)
    }
}

/**
 * Binds a radio group with a int preference.
 */
fun RadioGroup.bindToPreference(pref: Preference<Int>, block: (() -> Unit)? = null) {
    (getChildAt(pref.get()) as? RadioButton)?.isChecked = true
    setOnCheckedChangeListener { _, checkedId ->
        val index = indexOfChild(findViewById(checkedId))
        pref.set(index)
        block?.invoke()
    }
}

/**
 * Binds a spinner to an int preference with an optional offset for the value.
 */
fun Spinner.bindToPreference(
    pref: Preference<Int>,
    offset: Int = 0,
) {
    onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
        pref.set(position + offset)
    }
    setSelection(pref.get() - offset, false)
}
