package eu.kanade.tachiyomi.ui.library.display

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.util.bindToPreference
import kotlinx.android.synthetic.main.library_badges_layout.view.*

class LibraryBadgesView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LibraryPreferenceView(context, attrs)  {

    override fun initGeneralPreferences() {
        unread_badge_group.bindToPreference(preferences.unreadBadgeType()) {
            controller.presenter.requestUnreadBadgesUpdate()
        }
        hide_reading.bindToPreference(preferences.hideStartReadingButton()) {
            controller.reattachAdapter()
        }
        download_badge.bindToPreference(preferences.downloadBadge()) {
            controller.presenter.requestDownloadBadgesUpdate()
        }
    }
}