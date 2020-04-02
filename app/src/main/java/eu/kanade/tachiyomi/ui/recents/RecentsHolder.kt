package eu.kanade.tachiyomi.ui.recents

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.recents_item.*

class RecentsHolder(
    view: View,
    val adapter: RecentsAdapter
) : BaseFlexibleViewHolder(view, adapter),
    FlexibleAdapter.OnItemClickListener,
    FlexibleAdapter.OnUpdateListener,
    RecentMangaAdapter.RecentsInterface {

    private val subAdapter = RecentMangaAdapter(this)

    init {
        recycler.adapter = subAdapter
        subAdapter.isSwipeEnabled = true
        val manager = LinearLayoutManager(view.context)
        recycler.layoutManager = manager
        recycler.addItemDecoration(
            DividerItemDecoration(
                recycler.context, DividerItemDecoration.VERTICAL
            )
        )
        recycler.setHasFixedSize(true)
        view_all.setOnClickListener { adapter.delegate.viewAll(adapterPosition) }
        subAdapter.itemTouchHelperCallback.setSwipeFlags(
            ItemTouchHelper.LEFT
        )
    }

    fun bind(item: RecentsItem) {
        when (item.recentType) {
            RecentsItem.CONTINUE_READING -> {
                title.setText(R.string.continue_reading)
                view_all.setText(R.string.view_history)
            }
            RecentsItem.NEW_CHAPTERS -> {
                title.setText(R.string.new_chapters)
                view_all.setText(R.string.view_all_updates)
            }
            RecentsItem.NEWLY_ADDED -> {
                title.setText(R.string.new_additions)
            }
        }
        title.visibleIf(item.recentType != RecentsItem.SEARCH)
        view_all.visibleIf(item.recentType == RecentsItem.CONTINUE_READING || item.recentType == RecentsItem.NEW_CHAPTERS)
        recycler.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = if (adapterPosition == adapter.itemCount - 1) 0 else 12.dpToPx
        }
        subAdapter.updateDataSet(item.mangaList)
    }

    override fun downloadChapter(position: Int) {
        val item = (subAdapter.getItem(position) as? RecentMangaItem) ?: return
        adapter.delegate.downloadChapter(item)
    }

    override fun startDownloadNow(position: Int) {
        val chapter = (subAdapter.getItem(position) as? RecentMangaItem)?.chapter ?: return
        adapter.delegate.downloadChapterNow(chapter)
    }

    override fun onCoverClick(position: Int) {
        val manga = (subAdapter.getItem(position) as? RecentMangaItem)?.mch?.manga ?: return
        adapter.delegate.showManga(manga)
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        val item = (subAdapter.getItem(position) as? RecentMangaItem) ?: return false
        adapter.delegate.resumeManga(item.mch.manga, item.chapter)
        return true
    }

    fun updateChapterDownload(download: Download): Boolean {
        val holder = getHolder(download.chapter) ?: return false
        holder.notifyStatus(download.status, download.progress)
        return true
    }

    fun refreshChapter(chapterId: Long) {
        val item = (adapter.getItem(adapterPosition) as? RecentsItem) ?: return
        val recentItemPos = item.mangaList.indexOfFirst { it.mch.chapter.id == chapterId }
        if (recentItemPos > -1) subAdapter.notifyItemChanged(recentItemPos)
    }

    override fun onUpdateEmptyView(size: Int) {
        if (size > 0) {
            empty_view?.hide()
        } else {
            val recentsType = (adapter.getItem(adapterPosition) as? RecentsItem)?.recentType ?: return
            when (recentsType) {
                RecentsItem.CONTINUE_READING ->
                    empty_view?.show(R.drawable.ic_history_white_128dp, R.string.information_no_recent_manga)
                RecentsItem.NEW_CHAPTERS ->
                    empty_view?.show(R.drawable.ic_update_black_128dp, R.string.information_no_recent)
                RecentsItem.NEWLY_ADDED ->
                    empty_view?.show(R.drawable.recent_read_outline_128dp, R.string.information_no_recent)
                RecentsItem.SEARCH ->
                    empty_view?.show(R.drawable.search_128dp, R.string.no_search_result)
            }
        }
    }

    private fun getHolder(chapter: Chapter): RecentMangaHolder? {
        return recycler?.findViewHolderForItemId(chapter.id!!) as? RecentMangaHolder
    }

    override fun setCover(manga: Manga, view: ImageView) {
        adapter.delegate.setCover(manga, view)
    }

    override fun markAsRead(position: Int) {
        val item = (subAdapter.getItem(position) as RecentMangaItem)
        adapter.delegate.markAsRead(item.mch.manga, item.chapter)
    }

    override fun onHeaderClick(position: Int) {
    }
    override fun isSearching() = false
}
