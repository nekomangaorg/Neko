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
import java.util.Locale

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

        return MaterialDialog(activity).title(R.string.remove)
            .message(R.string.this_will_reomve_the_read_date_question).checkBoxPrompt(
                text = activity.getString(
                    R.string.reset_all_chapters_for_this_,
                    (manga?.mangaType(activity) ?: activity.getString(R.string.manga)).toLowerCase(
                            Locale.ROOT
                        )
                )
            ) {}.negativeButton(android.R.string.cancel).positiveButton(R.string.remove) {
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
