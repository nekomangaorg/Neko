package eu.kanade.tachiyomi.ui.manga.chapter

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class DeleteChaptersDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : DeleteChaptersDialog.Listener {

    constructor(target: T) : this() {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!).show {
            message(R.string.confirm_delete_chapters)
            negativeButton(android.R.string.no)
            positiveButton(android.R.string.yes) {
                (targetController as? Listener)?.deleteChapters()
            }
        }
    }

    interface Listener {
        fun deleteChapters()
    }
}
