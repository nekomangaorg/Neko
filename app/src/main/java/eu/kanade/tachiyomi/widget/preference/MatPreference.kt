package eu.kanade.tachiyomi.widget.preference

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.preference.Preference
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class MatPreference @JvmOverloads constructor(
    val activity: Activity?,
    context: Context,
    attrs:
        AttributeSet? =
            null
) :
    Preference(context, attrs) {

    val prefs: PreferencesHelper = Injekt.get()
    private var isShowing = false

    @StringRes var dialogTitleRes: Int? = null

    override fun onClick() {
        if (!isShowing) {
            dialog().apply {
                onDismiss { this@MatPreference.isShowing = false }
            }.show()
        }
        isShowing = true
    }

    protected open var customSummaryProvider: SummaryProvider<MatPreference>? = null
        set(value) {
            field = value
            summaryProvider = customSummaryProvider
        }

    override fun getSummary(): CharSequence? {
        customSummaryProvider?.let { return it.provideSummary(this) }
        return super.getSummary()
    }

    override fun setSummary(summaryResId: Int) {
        if (summaryResId == 0) {
            summaryProvider = customSummaryProvider
            return
        } else {
            customSummaryProvider = null
            summaryProvider = null
        }
        super.setSummary(summaryResId)
    }

    override fun setSummary(summary: CharSequence?) {
        if (summary == null) {
            summaryProvider = customSummaryProvider
            return
        } else {
            customSummaryProvider = null
            summaryProvider = null
        }
        super.setSummary(summary)
    }

    open fun dialog(): MaterialDialog {
        return MaterialDialog(activity ?: context).apply {
            if (dialogTitleRes != null) {
                title(res = dialogTitleRes)
            } else if (title != null) {
                title(text = title.toString())
            }
            negativeButton(android.R.string.cancel)
        }
    }
}
