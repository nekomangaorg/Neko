package eu.kanade.tachiyomi.ui.manga

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController

/**
 * Dialog to choose a shape for the icon.
 */
class ChooseShapeDialog(bundle: Bundle? = null) : DialogController(bundle) {

    constructor(target: MangaDetailsController) : this() {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val modes = intArrayOf(
            R.string.circular,
            R.string.rounded,
            R.string.square,
            R.string.star)

        return MaterialDialog(activity!!)
            .title(R.string.icon_shape)
            .negativeButton(android.R.string.cancel)
            .listItemsSingleChoice(
                items = modes.map { activity?.getString(it) as CharSequence },
                waitForPositiveButton = false) { _, i, _ ->
                (targetController as? MangaDetailsController)?.createShortcutForShape(i)
                dismissDialog()
            }
    }
}
