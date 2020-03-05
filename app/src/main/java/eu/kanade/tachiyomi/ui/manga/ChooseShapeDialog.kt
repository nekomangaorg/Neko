package eu.kanade.tachiyomi.ui.manga

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.manga.info.MangaInfoController

/**
 * Dialog to choose a shape for the icon.
 */
class ChooseShapeDialog(bundle: Bundle? = null) : DialogController(bundle) {

    constructor(target: MangaInfoController) : this() {
        targetController = target
    }

    constructor(target: MangaDetailsController) : this() {
        targetController = target
    }

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val modes = intArrayOf(
            R.string.circular_icon,
            R.string.rounded_icon,
            R.string.square_icon,
            R.string.star_icon)

        return MaterialDialog(activity!!)
            .title(R.string.icon_shape)
            .negativeButton(android.R.string.cancel)
            .listItemsSingleChoice (
                items = modes.map { activity?.getString(it) as CharSequence },
                waitForPositiveButton = false)
            { _, i, _ ->
                (targetController as? MangaInfoController)?.createShortcutForShape(i)
                (targetController as? MangaDetailsController)?.createShortcutForShape(i)
                dismissDialog()
            }
    }
}