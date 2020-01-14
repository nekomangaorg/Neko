package eu.kanade.tachiyomi.widget.preference

import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.ui.setting.defaultValue
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class ListMatPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? =
    null) :
    Preference(context, attrs) {
    protected val prefs: PreferencesHelper = Injekt.get()

    var sharedPref:String? = null
    var otherPref:Preference? = null
    var entryValues:List<String> = emptyList()
    var entriesRes:Array<Int>
        get() = emptyArray()
        set(value) { entries = value.map { context.getString(it) } }
    protected var defValue:String = ""
    var entries:List<String> = emptyList()

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)
        defValue = defaultValue as? String ?: defValue
    }
    override fun getSummary(): CharSequence {
        val index = entryValues.indexOf(prefs.getStringPref(key, defValue).getOrDefault())
        return if (entries.isEmpty() || index == -1) ""
        else entries[index]
    }

    override fun onClick() {
        dialog().show()
    }

    open fun dialog(): MaterialDialog {
        return MaterialDialog(context).apply {
            if (this@ListMatPreference.title != null)
                title(text = this@ListMatPreference.title.toString())
            negativeButton(android.R.string.cancel)
            val default = entryValues.indexOf(if (sharedPref != null) {
                val settings = context.getSharedPreferences(sharedPref, Context.MODE_PRIVATE)
                settings.getString(key, "")
            }
            else prefs.getStringPref(key, defValue).getOrDefault())
            listItemsSingleChoice(items = entries,
                waitForPositiveButton = false,
                initialSelection = default) { _, pos, _ ->
                val value = entryValues[pos]
                if (sharedPref != null) {
                    val oldDef = if (default > -1) entries[default] else ""
                    val settings = context.getSharedPreferences(sharedPref, Context.MODE_PRIVATE)
                    val edit = settings.edit()
                    edit.putString(key, value)
                    edit.apply()
                    otherPref?.callChangeListener(value)
                    if (oldDef == otherPref?.summary || otherPref?.summary.isNullOrEmpty()) otherPref?.summary =
                        entries[pos]
                    else otherPref?.summary = otherPref?.summary?.toString()?.replace(oldDef,
                        entries[pos]
                    ) ?: entries[pos]
                }
                else {
                    prefs.getStringPref(key, defValue).set(value)
                    this@ListMatPreference.summary = this@ListMatPreference.summary
                }
                dismiss()
            }
        }
    }
}