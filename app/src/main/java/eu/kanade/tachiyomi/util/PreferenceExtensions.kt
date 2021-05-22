package eu.kanade.tachiyomi.util

import android.content.SharedPreferences
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.annotation.ArrayRes
import com.f2prateek.rx.preferences.Preference
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.widget.IgnoreFirstSpinnerListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
inline fun <reified T> SharedPreferences.getKey(key: String, default: T, dispatcher: CoroutineContext = Dispatchers.Default): Flow<T> {
    val flow: Flow<T> = channelFlow {
        offer(getItem(key, default))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, k ->
            if (key == k) {
                offer(getItem(key, default)!!)
            }
        }

        registerOnSharedPreferenceChangeListener(listener)
        awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return flow
        .flowOn(dispatcher)
}

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
fun CompoundButton.bindToPreference(pref: Preference<Boolean>, block: (() -> Unit)? = null) {
    isChecked = pref.getOrDefault()
    setOnCheckedChangeListener { _, isChecked ->
        pref.set(isChecked)
        block?.invoke()
    }
}

/**
 * Binds a checkbox or switch view with a boolean preference.
 */
fun CompoundButton.bindToPreference(
    pref: com.tfcporciuncula.flow
    .Preference<Boolean>,
    block: ((Boolean) -> Unit)? = null
) {
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
    (getChildAt(pref.getOrDefault()) as RadioButton).isChecked = true
    setOnCheckedChangeListener { _, checkedId ->
        val index = indexOfChild(findViewById(checkedId))
        pref.set(index)
        block?.invoke()
    }
}

/**
 * Binds a radio group with a int preference.
 */
fun RadioGroup.bindToPreference(pref: com.tfcporciuncula.flow.Preference<Int>, block: (() -> Unit)? = null) {
    (getChildAt(pref.get()) as RadioButton).isChecked = true
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
    pref: com.tfcporciuncula.flow.Preference<Int>,
    offset: Int = 0
) {
    onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
        pref.set(position + offset)
    }
    setSelection(pref.get() - offset, false)
}

/**
 * Binds a spinner to an int preference. The position of the spinner item must
 * correlate with the [intValues] resource item (in arrays.xml), which is a <string-array>
 * of int values that will be parsed here and applied to the preference.
 */
fun Spinner.bindToIntPreference(pref: com.tfcporciuncula.flow.Preference<Int>, @ArrayRes intValuesResource: Int) {
    val intValues = resources.getStringArray(intValuesResource).map { it.toIntOrNull() }
    onItemSelectedListener = IgnoreFirstSpinnerListener { position ->
        pref.set(intValues[position] ?: 0)
    }
    setSelection(intValues.indexOf(pref.get()), false)
}
