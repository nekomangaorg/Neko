package eu.kanade.tachiyomi.ui.recents

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page

class RecentMangaItem(val mch: MangaChapterHistory, val chapter: Chapter) :
    AbstractFlexibleItem<RecentMangaHolder>
    () {

    private var _status: Int = 0

    val progress: Int
        get() {
            val pages = download?.pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }

    var status: Int
        get() = download?.status ?: _status
        set(value) { _status = value }

    @Transient var download: Download? = null

    override fun getLayoutRes(): Int {
        return R.layout.recent_manga_item
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): RecentMangaHolder {
        return RecentMangaHolder(view, adapter as RecentMangaAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: RecentMangaHolder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {

        holder.bind(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other is RecentMangaItem) {
            return chapter.id == other.chapter.id
        }
        return false
    }

    override fun hashCode(): Int {
        return chapter.id!!.hashCode()
    }
}
