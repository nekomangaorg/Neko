package eu.kanade.tachiyomi.widget.preference

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class MatPreference @JvmOverloads constructor(
    val activity: Activity?,
    context: Context,
    attrs: AttributeSet? =
        null,
) :
    Preference(context, attrs) {

    val prefs: PreferencesHelper = Injekt.get()

    @StringRes
    var preSummaryRes: Int? = null
        set(value) {
            field = value
            notifyChanged()
        }
    private var isShowing = false

    @StringRes
    var dialogTitleRes: Int? = null

    override fun onClick() {
        if (!isShowing) {
            val dialog = dialog().apply {
                setOnDismissListener { this@MatPreference.isShowing = false }
            }.create()
//            dialog.setOnShowListener {
            onShow(dialog)
//            }
            dialog.show()
        }
        isShowing = true
    }

    protected open fun onShow(dialog: AlertDialog) { }

    protected open var customSummaryProvider: SummaryProvider<MatPreference>? = null
        set(value) {
            field = value
            summaryProvider = customSummaryProvider
        }

    override fun getSummary(): CharSequence? {
        customSummaryProvider?.let {
            val preSummaryRes = preSummaryRes
            return if (preSummaryRes != null) {
                context.getString(preSummaryRes, it.provideSummary(this))
            } else {
                it.provideSummary(this)
            }
        }
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

    open fun dialog(): MaterialAlertDialogBuilder {
        return (activity ?: context).materialAlertDialog().apply {
            if (dialogTitleRes != null) {
                setTitle(dialogTitleRes!!)
            } else if (title != null) {
                setTitle(title.toString())
            }
            setNegativeButton(android.R.string.cancel, null)
        }
    }
}
