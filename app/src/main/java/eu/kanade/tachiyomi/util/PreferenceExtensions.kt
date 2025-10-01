package eu.kanade.tachiyomi.util

import android.widget.CompoundButton
import tachiyomi.core.preference.Preference

/** Binds a checkbox or switch view with a boolean preference. */
fun CompoundButton.bindToPreference(pref: Preference<Boolean>, block: ((Boolean) -> Unit)? = null) {
    setOnCheckedChangeListener { _, _ -> }
    isChecked = pref.get()
    setOnCheckedChangeListener { _, isChecked ->
        pref.set(isChecked)
        block?.invoke(isChecked)
    }
}
