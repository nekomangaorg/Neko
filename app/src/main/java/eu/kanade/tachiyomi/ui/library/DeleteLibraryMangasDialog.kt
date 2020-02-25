package eu.kanade.tachiyomi.ui.library

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class DeleteLibraryMangasDialog<T>(bundle: Bundle? = null) :
        DialogController(bundle) where T : Controller, T : DeleteLibraryMangasDialog.Listener {

    private var mangas = emptyList<Manga>()

    constructor(target: T, mangas: List<Manga>) : this() {
        this.mangas = mangas
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
                .title(R.string.action_remove)
                .message(R.string.confirm_delete_manga)
                .negativeButton(android.R.string.no)
                .checkBoxPrompt(res = R.string.also_delete_chapters, isCheckedDefault = true) {}
                .positiveButton(android.R.string.yes) {
                    (targetController as? Listener)?.deleteMangasFromLibrary(mangas, it.isCheckPromptChecked())
                }
    }

    interface Listener {
        fun deleteMangasFromLibrary(mangas: List<Manga>, deleteChapters: Boolean)
    }
}
