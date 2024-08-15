package eu.kanade.tachiyomi.ui.recents

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.image.coil.loadManga
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterHolder
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.timeSpanFromNow
import eu.kanade.tachiyomi.util.view.setCards
import org.nekomanga.R
import org.nekomanga.databinding.RecentMangaItemBinding

class RecentMangaHolder(
    view: View,
    val adapter: RecentMangaAdapter,
) : BaseChapterHolder(view, adapter) {

    private val binding = RecentMangaItemBinding.bind(view)

    init {
        binding.cardLayout.setOnClickListener {
            adapter.delegate.onCoverClick(flexibleAdapterPosition)
        }
        binding.removeHistory.setOnClickListener {
            adapter.delegate.onRemoveHistoryClicked(flexibleAdapterPosition)
        }
        updateCards()
    }

    fun updateCards() {
        setCards(adapter.showOutline, binding.card, null)
    }

    fun bind(item: RecentMangaItem) {
        val showDLs = adapter.showDownloads
        itemView.transitionName = "recents chapter $bindingAdapterPosition transition"
        val showRemoveHistory = adapter.showRemoveHistory
        val showTitleFirst = adapter.showTitleFirst
        binding.downloadButton.downloadButton.isVisible =
            when (showDLs) {
                RecentMangaAdapter.ShowRecentsDLs.None -> false
                RecentMangaAdapter.ShowRecentsDLs.OnlyUnread,
                RecentMangaAdapter.ShowRecentsDLs.UnreadOrDownloaded -> !item.chapter.read
                RecentMangaAdapter.ShowRecentsDLs.OnlyDownloaded -> true
                RecentMangaAdapter.ShowRecentsDLs.All -> true
            }

        val isSmallUpdates =
            adapter.viewType == RecentsPresenter.VIEW_TYPE_ONLY_UPDATES && !adapter.showUpdatedTime
        binding.cardLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = (if (isSmallUpdates) 40 else 80).dpToPx
            width = (if (isSmallUpdates) 40 else 60).dpToPx
        }
        listOf(binding.title, binding.subtitle).forEach {
            it.updateLayoutParams<ConstraintLayout.LayoutParams> {
                if (isSmallUpdates) {
                    if (it == binding.title) topMargin = 5.dpToPx
                    endToStart = R.id.button_layout
                    endToEnd = -1
                } else {
                    if (it == binding.title) topMargin = 2.dpToPx
                    endToStart = -1
                    endToEnd = R.id.front_view
                }
            }
        }
        binding.buttonLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
            if (isSmallUpdates) {
                topToBottom = -1
                topToTop = R.id.front_view
            } else {
                topToTop = -1
                topToBottom = R.id.subtitle
            }
        }
        val freeformCovers = !isSmallUpdates && !adapter.uniformCovers
        with(binding.coverThumbnail) {
            adjustViewBounds = freeformCovers
            scaleType =
                if (!freeformCovers) ImageView.ScaleType.CENTER_CROP
                else ImageView.ScaleType.FIT_CENTER
        }
        listOf(binding.coverThumbnail, binding.card).forEach {
            it.updateLayoutParams<ViewGroup.LayoutParams> {
                width =
                    if (!freeformCovers) {
                        ViewGroup.LayoutParams.MATCH_PARENT
                    } else {
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    }
            }
        }

        binding.removeHistory.isVisible = item.mch.history.id != null && showRemoveHistory
        val chapterName =
            if (item.mch.manga.hideChapterTitle(adapter.mangaDetailsPreferences)) {
                val number = adapter.decimalFormat.format(item.chapter.chapter_number.toDouble())
                itemView.context.getString(R.string.chapter_, number)
            } else {
                item.chapter.name
            }
        binding.title.apply {
            text =
                if (!showTitleFirst) {
                    chapterName
                } else {
                    item.mch.manga.title
                }
            ChapterUtil.setTextViewForChapter(this, item)
        }
        binding.subtitle.apply {
            text =
                if (!showTitleFirst) {
                    item.mch.manga.title
                } else {
                    chapterName
                }
            setTextColor(ChapterUtil.readColor(context, item))
        }
        if (binding.frontView.translationX == 0f) {
            binding.read.setImageResource(
                if (item.read) R.drawable.ic_eye_off_24dp else R.drawable.ic_eye_24dp,
            )
        }
        val notValidNum = item.mch.chapter.chapter_number <= 0
        binding.body.isVisible = !isSmallUpdates
        binding.body.text =
            when {
                item.mch.chapter.id == null ->
                    binding.body.context.getString(
                        R.string.added_,
                        item.mch.manga.date_added.timeSpanFromNow(itemView.context),
                    )
                isSmallUpdates -> ""
                item.mch.history.id == null -> {
                    if (adapter.viewType == RecentsPresenter.VIEW_TYPE_ONLY_UPDATES) {
                        if (adapter.sortByFetched) {
                            binding.body.context.getString(
                                R.string.fetched_,
                                item.chapter.date_fetch.timeSpanFromNow(itemView.context),
                            )
                        } else {
                            binding.body.context.getString(
                                R.string.updated_,
                                item.chapter.date_upload.timeSpanFromNow(itemView.context),
                            )
                        }
                    } else {
                        binding.body.context.getString(
                            R.string.fetched_,
                            item.chapter.date_fetch.timeSpanFromNow(itemView.context),
                        ) +
                            "\n" +
                            binding.body.context.getString(
                                R.string.updated_,
                                item.chapter.date_upload.timeSpanFromNow(itemView.context),
                            )
                    }
                }
                item.chapter.id != item.mch.chapter.id ->
                    binding.body.context.getString(
                        R.string.read_,
                        item.mch.history.last_read.timeSpanFromNow,
                    ) +
                        "\n" +
                        binding.body.context.getString(
                            if (notValidNum) R.string.last_read_ else R.string.last_read_chapter_,
                            if (notValidNum) item.mch.chapter.name
                            else adapter.decimalFormat.format(item.mch.chapter.chapter_number),
                        )
                item.chapter.pages_left > 0 && !item.chapter.read ->
                    binding.body.context.getString(
                        R.string.read_,
                        item.mch.history.last_read.timeSpanFromNow(itemView.context),
                    ) +
                        "\n" +
                        itemView.resources.getQuantityString(
                            R.plurals.pages_left,
                            item.chapter.pages_left,
                            item.chapter.pages_left,
                        )
                else ->
                    binding.body.context.getString(
                        R.string.read_,
                        item.mch.history.last_read.timeSpanFromNow(itemView.context),
                    )
            }
        if ((itemView.context as? Activity)?.isDestroyed != true) {
            binding.coverThumbnail.loadManga(item.mch.manga)
        }
        notifyStatus(
            item.status,
            item.progress,
            item.chapter.read,
        )

        resetFrontView()
    }

    private fun resetFrontView() {
        if (binding.frontView.translationX != 0f) {
            itemView.post {
                androidx.transition.TransitionManager.endTransitions(adapter.recyclerView)
                adapter.notifyItemChanged(flexibleAdapterPosition)
            }
        }
    }

    override fun onLongClick(view: View?): Boolean {
        super.onLongClick(view)
        val item = adapter.getItem(flexibleAdapterPosition) as? RecentMangaItem ?: return false
        return item.mch.history.id != null
    }

    fun notifyStatus(
        status: Download.State,
        progress: Int,
        isRead: Boolean,
        animated: Boolean = false,
    ) {
        binding.downloadButton.downloadButton.setDownloadStatus(status, progress, animated)
        val isChapterRead =
            if (adapter.showDownloads == RecentMangaAdapter.ShowRecentsDLs.UnreadOrDownloaded)
                isRead
            else true
        binding.downloadButton.downloadButton.isVisible =
            when (adapter.showDownloads) {
                RecentMangaAdapter.ShowRecentsDLs.UnreadOrDownloaded,
                RecentMangaAdapter.ShowRecentsDLs.OnlyDownloaded, ->
                    status != Download.State.NOT_DOWNLOADED || !isChapterRead
                else -> binding.downloadButton.downloadButton.isVisible
            }
    }

    override fun getFrontView(): View {
        return binding.frontView
    }

    override fun getRearRightView(): View {
        return binding.endView
    }
}
