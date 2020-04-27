package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.isVisible
import eu.kanade.tachiyomi.util.view.setTextColorRes
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.unread_download_badge.view.*

class LibraryBadge @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs) {

    fun setUnreadDownload(unread: Int, downloads: Int, showTotalChapters: Boolean) {
        // Update the unread count and its visibility.

        val unreadBadgeBackground = context.contextCompatColor(
            if (showTotalChapters) R.color.total_badge else R.color.unread_badge
        )

        with(unread_text) {
            visibleIf(unread > 0 || unread == -1 || showTotalChapters)
            if (!isVisible()) {
                return@with
            }
            text = if (unread == -1) "0" else unread.toString()
            setTextColorRes(
                // hide the badge text when preference is only show badge
                when {
                    unread == -1 && !showTotalChapters -> R.color.unread_badge
                    showTotalChapters -> R.color.total_badge_text
                    else -> R.color.unread_badge_text
                }
            )
            setBackgroundColor(unreadBadgeBackground)
        }

        // Update the download count or local status and its visibility.
        with(download_text) {
            visibleIf(downloads == -2 || downloads > 0)
            if (!isVisible()) {
                return@with
            }
            text = if (downloads == -2) {
                resources.getString(R.string.local)
            } else {
                downloads.toString()
            }
        }

        // Show the badge card if unread or downloads exists
        visibleIf(download_text.isVisible() || unread_text.isVisible())

        // Show the angles divider if both unread and downloads exists
        unread_angle.visibleIf(download_text.isVisible() && unread_text.isVisible())

        unread_angle.setColorFilter(unreadBadgeBackground)
        if (unread_angle.isVisible()) {
            download_text.updatePaddingRelative(end = 8.dpToPx)
            unread_text.updatePaddingRelative(start = 2.dpToPx)
        } else {
            download_text.updatePaddingRelative(end = 5.dpToPx)
            unread_text.updatePaddingRelative(start = 5.dpToPx)
        }
    }

    fun setChapters(chapters: Int?) {
        setUnreadDownload(chapters ?: 0, 0, chapters != null)
    }

    fun setInLibrary(inLibrary: Boolean) {
        this.visibleIf(inLibrary)
        unread_angle.gone()
        unread_text.updatePaddingRelative(start = 5.dpToPx)
        unread_text.visibleIf(inLibrary)
        unread_text.text = resources.getText(R.string.in_library)
    }

    fun setStatus(status: FollowStatus, inLibrary: Boolean) {
        this.visible()

        unread_angle.visibleIf(inLibrary)
        download_text.visibleIf(inLibrary)
        download_text.text = resources.getText(R.string.in_library)

        unread_text.updatePaddingRelative(start = 5.dpToPx)
        unread_text.visible()
        val statusText = when (status) {
            FollowStatus.READING -> R.string.follows_reading
            FollowStatus.UNFOLLOWED -> R.string.follows_unfollowed
            FollowStatus.COMPLETED -> R.string.follows_completed
            FollowStatus.ON_HOLD -> R.string.follows_on_hold
            FollowStatus.PLAN_TO_READ -> R.string.follows_plan_to_read
            FollowStatus.DROPPED -> R.string.follows_dropped
            FollowStatus.RE_READING -> R.string.follows_re_reading
        }
        unread_text.text = resources.getText(statusText)
    }
}
