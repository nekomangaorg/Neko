package eu.kanade.tachiyomi.ui.catalogue

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HideCatalogueDialog(bundle: Bundle? = null) : DialogController(bundle) {

    private val source = Injekt.get<SourceManager>().get(args.getLong("key"))!!

    constructor(source: Source) : this(Bundle().apply { putLong("key", source.id) })

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        return MaterialDialog(activity!!)
                .title(text = activity!!.getString(R.string.hide_question, source.name))
                .positiveButton(android.R.string.ok) {
                    (targetController as? Listener)?.hideCatalogueDialogClosed(source)
                }
                .negativeButton(android.R.string.cancel)
    }

    interface Listener {
        fun hideCatalogueDialogClosed(source: Source)
    }
}
