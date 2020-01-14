package eu.kanade.tachiyomi.ui.manga.chapter

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.controller.DialogController

class SetDisplayModeDialog<T>(bundle: Bundle? = null) : DialogController(bundle)
        where T : Controller, T : SetDisplayModeDialog.Listener {

    private val selectedIndex = args.getInt("selected", -1)

    constructor(target: T, selectedIndex: Int = -1) : this(Bundle().apply {
        putInt("selected", selectedIndex)
    }) {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val activity = activity!!
        val ids = intArrayOf(Manga.DISPLAY_NAME, Manga.DISPLAY_NUMBER)
        val choices = intArrayOf(R.string.show_title, R.string.show_chapter_number)
                .map { activity.getString(it) }

        return MaterialDialog(activity)
                .title(R.string.action_display_mode)
                .listItemsSingleChoice(items = choices, initialSelection = selectedIndex)
                { _, position, text ->
                    (targetController as? Listener)?.setDisplayMode(ids[position])
                }
                .positiveButton(android.R.string.ok)
    }

    interface Listener {
        fun setDisplayMode(id: Int)
    }

}