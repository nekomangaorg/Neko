package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.UnreadDownloadBadgeBinding
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.isVisible
import eu.kanade.tachiyomi.util.view.setTextColorRes
import eu.kanade.tachiyomi.util.view.updatePaddingRelative
import eu.kanade.tachiyomi.util.view.visibleIf

class LibraryBadge @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs) {

    private lateinit var binding: UnreadDownloadBadgeBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = UnreadDownloadBadgeBinding.bind(this)
    }

    fun setUnreadDownload(unread: Int, downloads: Int, showTotalChapters: Boolean) {
        // Update the unread count and its visibility.

        val unreadBadgeBackground = context.contextCompatColor(
            if (showTotalChapters) R.color.total_badge else R.color.unread_badge
        )

        with(binding.unreadText) {
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
        with(binding.downloadText) {
            visibleIf(downloads == -2 || downloads > 0)
            if (!isVisible()) { return@with }
            text = if (downloads == -2) {
                resources.getString(R.string.local)
            } else {
                downloads.toString()
            }
        }

        // Show the badge card if unread or downloads exists
        visibleIf(binding.downloadText.isVisible() || binding.unreadText.isVisible())

        // Show the angles divider if both unread and downloads exists
        binding.unreadAngle.visibleIf(binding.downloadText.isVisible() && binding.unreadText.isVisible())

        binding.unreadAngle.setColorFilter(unreadBadgeBackground)
        if (binding.unreadAngle.isVisible()) {
            binding.downloadText.updatePaddingRelative(end = 8.dpToPx)
            binding.unreadText.updatePaddingRelative(start = 2.dpToPx)
        } else {
            binding.downloadText.updatePaddingRelative(end = 5.dpToPx)
            binding.unreadText.updatePaddingRelative(start = 5.dpToPx)
        }
    }

    fun setChapters(chapters: Int?) {
        setUnreadDownload(chapters ?: 0, 0, chapters != null)
    }

    fun setInLibrary(inLibrary: Boolean) {
        this.visibleIf(inLibrary)
        binding.unreadAngle.gone()
        binding.unreadText.updatePaddingRelative(start = 5.dpToPx)
        binding.unreadText.visibleIf(inLibrary)
        binding.unreadText.text = resources.getText(R.string.in_library)
    }
}
