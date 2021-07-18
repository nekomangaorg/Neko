package eu.kanade.tachiyomi.ui.library

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.UnreadDownloadBadgeBinding
import eu.kanade.tachiyomi.source.online.utils.FollowStatus
import eu.kanade.tachiyomi.util.system.contextCompatColor
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.updatePaddingRelative

class LibraryBadge @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs) {

    private lateinit var binding: UnreadDownloadBadgeBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = UnreadDownloadBadgeBinding.bind(this)
    }

    fun setUnreadDownload(unread: Int, downloads: Int, showTotalChapters: Boolean) {
        // Update the unread count and its visibility.

        val unreadBadgeBackground = if (showTotalChapters) {
            context.contextCompatColor(R.color.total_badge)
        } else context.getResourceColor(R.attr.unreadBadgeColor)

        with(binding.unreadText) {
            isVisible = unread > 0 || unread == -1 || showTotalChapters
            if (!isVisible) {
                return@with
            }
            text = if (unread == -1) "0" else unread.toString()
            setTextColor(
                // hide the badge text when preference is only show badge
                when {
                    unread == -1 && !showTotalChapters -> unreadBadgeBackground
                    showTotalChapters -> context.contextCompatColor(R.color.total_badge_text)
                    else -> context.getResourceColor(R.attr.colorOnUnreadBadge)
                }
            )
            setBackgroundColor(unreadBadgeBackground)
        }

        // Update the download count or local status and its visibility.
        with(binding.downloadText) {
            isVisible = downloads == -2 || downloads > 0
            if (!isVisible) {
                return@with
            }
            text = if (downloads == -2) {
                resources.getString(R.string.local)
            } else {
                downloads.toString()
            }
            setTextColor(context.getResourceColor(R.attr.colorOnDownloadBadge))
            setBackgroundColor(context.getResourceColor(R.attr.downloadBadgeColor))
        }

        // Show the badge card if unread or downloads exists
        isVisible = binding.downloadText.isVisible || binding.unreadText.isVisible

        // Show the angles divider if both unread and downloads exists
        binding.unreadAngle.isVisible = binding.downloadText.isVisible && binding.unreadText.isVisible

        binding.unreadAngle.setColorFilter(unreadBadgeBackground)
        if (binding.unreadAngle.isVisible) {
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
        this.isVisible = inLibrary
        binding.unreadAngle.isVisible = false
        binding.unreadText.updatePaddingRelative(start = 5.dpToPx)
        binding.unreadText.isVisible = inLibrary
        binding.unreadText.text = resources.getText(R.string.in_library)
    }

    fun setStatus(status: FollowStatus, inLibrary: Boolean) {
        this.isVisible = true
        with(binding) {
            unreadAngle.isVisible = inLibrary
            downloadText.isVisible = inLibrary
            downloadText.text = resources.getText(R.string.in_library)

            unreadText.updatePaddingRelative(start = 5.dpToPx)
            unreadText.isVisible = true
            val statusText = when (status) {
                FollowStatus.READING -> R.string.follows_reading
                FollowStatus.UNFOLLOWED -> R.string.follows_unfollowed
                FollowStatus.COMPLETED -> R.string.follows_completed
                FollowStatus.ON_HOLD -> R.string.follows_on_hold
                FollowStatus.PLAN_TO_READ -> R.string.follows_plan_to_read
                FollowStatus.DROPPED -> R.string.follows_dropped
                FollowStatus.RE_READING -> R.string.follows_re_reading
            }
            unreadText.text = resources.getText(statusText)
        }
    }
}
