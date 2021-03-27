package eu.kanade.tachiyomi.ui.main

import android.app.Activity
import android.content.Intent
import androidx.core.net.toUri
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.base.MaterialMenuSheet
import eu.kanade.tachiyomi.util.system.toast

class WhatsNewSheet(activity: Activity) : MaterialMenuSheet(
    activity,
    listOf(
        MenuSheetItem(
            0,
            textRes = R.string.whats_new_this_release,
            drawable = R.drawable.ic_new_releases_24dp
        ),
        MenuSheetItem(
            1,
            textRes = R.string.close,
            drawable = R.drawable.ic_close_24dp
        )
    ),
    title = activity.getString(R.string.updated_to_, BuildConfig.VERSION_NAME),
    showDivider = true,
    selectedId = 0,
    onMenuItemClicked = { _, item ->
        if (item == 0) {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://github.com/jays2kings/tachiyomiJ2K/releases/tag/v${BuildConfig.VERSION_NAME}".toUri()
                )
                activity.startActivity(intent)
            } catch (e: Throwable) {
                activity.toast(e.message)
            }
        }
        true
    }
)
