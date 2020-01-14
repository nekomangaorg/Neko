package eu.kanade.tachiyomi.ui.recently_read

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.History
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class RemoveHistoryDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : RemoveHistoryDialog.Listener {

    private var manga: Manga? = null

    private var history: History? = null

    constructor(target: T, manga: Manga, history: History) : this() {
        this.manga = manga
        this.history = history
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val activity = activity!!

        return MaterialDialog(activity)
                .title(R.string.action_remove)
                .message(R.string.dialog_with_checkbox_remove_description)
                .checkBoxPrompt(res = R.string.dialog_with_checkbox_reset, isCheckedDefault = true) {}
                .negativeButton(android.R.string.cancel)
                .positiveButton(R.string.action_remove) {
                    onPositive(it.isCheckPromptChecked())
                }

    }

    private fun onPositive(checked: Boolean) {
        val target = targetController as? Listener ?: return
        val manga = manga ?: return
        val history = history ?: return

        target.removeHistory(manga, history, checked)
    }

    interface Listener {
        fun removeHistory(manga: Manga, history: History, all: Boolean)
    }

}