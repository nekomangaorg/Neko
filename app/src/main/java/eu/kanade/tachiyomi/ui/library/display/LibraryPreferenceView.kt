package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import com.f2prateek.rx.preferences.Preference
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.library.LibraryController
import uy.kohesive.injekt.injectLazy

abstract class LibraryPreferenceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    internal val preferences by injectLazy<PreferencesHelper>()
    lateinit var controller: LibraryController

    abstract fun initGeneralPreferences()

    override fun onFinishInflate() {
        super.onFinishInflate()
        initGeneralPreferences()
    }

    /**
     * Binds a checkbox or switch view with a boolean preference.
     */
    internal fun CompoundButton.bindToPreference(pref: Preference<Boolean>, block: (() -> Unit)? = null) {
        isChecked = pref.getOrDefault()
        setOnCheckedChangeListener { _, isChecked ->
            pref.set(isChecked)
            block?.invoke()
        }
    }

    /**
     * Binds a checkbox or switch view with a boolean preference.
     */
    internal fun CompoundButton.bindToPreference(
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
    internal fun RadioGroup.bindToPreference(pref: Preference<Int>, block: (() -> Unit)? = null) {
        (getChildAt(pref.getOrDefault()) as RadioButton).isChecked = true
        setOnCheckedChangeListener { _, checkedId ->
            val index = indexOfChild(findViewById(checkedId))
            pref.set(index)
            block?.invoke()
        }
    }
}