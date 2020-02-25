package eu.kanade.tachiyomi.ui.recent_updates

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class ConfirmDeleteChaptersDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : ConfirmDeleteChaptersDialog.Listener {

    private var chaptersToDelete = emptyList<RecentChapterItem>()

    constructor(target: T, chaptersToDelete: List<RecentChapterItem>) : this() {
        this.chaptersToDelete = chaptersToDelete
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
                .message(R.string.confirm_delete_chapters)
                .negativeButton(android.R.string.no)
                .positiveButton(android.R.string.yes) {
                    (targetController as? Listener)?.deleteChapters(chaptersToDelete)
                }
    }

    interface Listener {
        fun deleteChapters(chaptersToDelete: List<RecentChapterItem>)
    }
}
