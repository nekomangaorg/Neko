package eu.kanade.tachiyomi.ui.recents

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.MangaChapterHistory
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterItem

class RecentMangaItem(
    val mch: MangaChapterHistory = MangaChapterHistory.createBlank(),
    chapter: Chapter = ChapterImpl(),
    header:
    RecentMangaHeaderItem?
) :
    BaseChapterItem<RecentMangaHolder, RecentMangaHeaderItem>(chapter, header) {

    override fun getLayoutRes(): Int {
        return if (mch.manga.id == null) R.layout.recents_footer_item
        else R.layout.recent_manga_item
    }

    override fun createViewHolder(
        view: View,
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>
    ): RecentMangaHolder {
        return RecentMangaHolder(view, adapter as RecentMangaAdapter)
    }

    override fun isSwipeable(): Boolean {
        return mch.manga.id != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is RecentMangaItem) {
            return if (mch.manga.id == null) header?.recentsType == other.header?.recentsType
            else chapter.id == other.chapter.id
        }
        return false
    }

    override fun hashCode(): Int {
        return if (mch.manga.id == null) -(header?.recentsType ?: 0).hashCode()
        else (chapter.id ?: 0L).hashCode()
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: RecentMangaHolder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {
        if (mch.manga.id == null) holder.bind(header?.recentsType ?: 0)
        else holder.bind(this)
    }
}
