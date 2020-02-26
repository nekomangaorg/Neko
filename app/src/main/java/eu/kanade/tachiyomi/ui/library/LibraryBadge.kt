package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import kotlinx.android.synthetic.main.unread_download_badge.view.*

class LibraryBadge @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null):
    MaterialCardView(context, attrs) {

    fun setUnreadDownload(unread: Int, downloads: Int, showTotalChapters: Boolean) {
        // Update the unread count and its visibility.
        with(unread_text) {
            text = if (unread == -1) "0" else unread.toString()
            setTextColor(if (unread == -1 && !showTotalChapters)
                context.getResourceColor(android.R.attr.colorAccent)
            else Color.WHITE)
            setBackgroundColor(
                if (showTotalChapters) ContextCompat.getColor(context, R.color.total_green)
                else context.getResourceColor(android.R.attr.colorAccent))
            visibility = when {
                unread > 0 || unread == -1 || showTotalChapters -> View.VISIBLE
                else -> View.GONE
            }
        }

        // Update the download count or local status and its visibility.
        with(download_text) {
            visibility = if (downloads == -2 || downloads > 0)  View.VISIBLE else View.GONE
            text = if (downloads == -2)
                resources.getString(R.string.local_source_badge)
            else downloads.toString()
        }

        // Show the bade card if unread or downloads exists
        badge_view.visibility = if (download_text.visibility == View.VISIBLE || unread_text
                .visibility != View.GONE) View.VISIBLE else View.GONE

        // Show the angles divider if both unread and downloads exists
        unread_angle.visibility = if (download_text.visibility == View.VISIBLE && unread_text
                .visibility != View.GONE) View.VISIBLE else View.GONE
        unread_angle.setColorFilter(
            if (showTotalChapters) ContextCompat.getColor(context, R.color.total_green)
            else context.getResourceColor(android.R.attr.colorAccent))
        if (unread_angle.visibility == View.VISIBLE) {
            download_text.updatePaddingRelative(end = 8.dpToPx)
            unread_text.updatePaddingRelative(start = 2.dpToPx)
        }
        else {
            download_text.updatePaddingRelative(end = 5.dpToPx)
            unread_text.updatePaddingRelative(start = 5.dpToPx)
        }
    }

    fun setChapters(chapters: Int?) {
        setUnreadDownload(chapters ?: 0, 0, chapters != null)
    }

    fun setInLibrary(inLibrary: Boolean) {
        badge_view.visibility = if (inLibrary) View.VISIBLE else View.GONE
        unread_angle.visibility = View.GONE
        unread_text.updatePaddingRelative(start = 5.dpToPx)
        unread_text.visibility = if (inLibrary) View.VISIBLE else View.GONE
        unread_text.text = resources.getText(R.string.in_library)
    }

}