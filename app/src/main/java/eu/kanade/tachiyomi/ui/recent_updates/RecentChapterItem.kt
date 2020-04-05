package eu.kanade.tachiyomi.ui.recent_updates

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.manga.chapter.BaseChapterItem

class RecentChapterItem(chapter: Chapter, val manga: Manga, header: DateItem) :
    BaseChapterItem<RecentChapterHolder, DateItem>(chapter, header) {

    override fun getLayoutRes(): Int {
        return R.layout.recent_chapters_item
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): RecentChapterHolder {
        return RecentChapterHolder(view, adapter as RecentChaptersAdapter)
    }

    override fun bindViewHolder(
        adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
        holder: RecentChapterHolder,
        position: Int,
        payloads: MutableList<Any?>?
    ) {
        holder.bind(this)
    }

    fun filter(text: String): Boolean {
        return chapter.name.contains(text, false) ||
            manga.title.contains(text, false)
    }
}
