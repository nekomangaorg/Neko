package eu.kanade.tachiyomi.ui.more

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import eu.kanade.tachiyomi.data.updater.AppDownloadInstallJob
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import org.nekomanga.R

@Deprecated(
    message =
        "This is only used from the main screen when it auto checks. Manual checks use compose version remove when everything is composed")
class NewUpdateDialogController(bundle: Bundle? = null) : DialogController(bundle) {

    constructor(
        body: String,
        url: String
    ) : this(
        Bundle().apply {
            putString(BODY_KEY, body)
            putString(URL_KEY, url)
        },
    )

    override fun onCreateDialog(savedViewState: Bundle?): Dialog {
        val isOnA12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        return activity!!
            .materialAlertDialog()
            .setTitle(R.string.new_version_available)
            .setMessage(args.getString(BODY_KEY) ?: "")
            .setPositiveButton(if (isOnA12) R.string.update else R.string.download) { _, _ ->
                val appContext = applicationContext
                if (appContext != null) {
                    // Start download
                    val url = args.getString(URL_KEY) ?: ""
                    AppDownloadInstallJob.start(appContext, url, true)
                }
            }
            .setNegativeButton(R.string.ignore, null)
            .create()
    }

    companion object {
        const val BODY_KEY = "NewUpdateDialogController.body"
        const val URL_KEY = "NewUpdateDialogController.key"
    }
}
