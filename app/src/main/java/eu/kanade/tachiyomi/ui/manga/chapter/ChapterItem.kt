package eu.kanade.tachiyomi.ui.manga.chapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.manga.MangaChapterHolder
import eu.kanade.tachiyomi.ui.manga.MangaHeaderHolder

class ChapterItem(val chapter: Chapter, val manga: Manga) :
    AbstractFlexibleItem<MangaChapterHolder>(),
    Chapter by chapter {

    private var _status: Int = 0

    val progress: Int
        get() {
            val pages = download?.pages ?: return 0
            return pages.map(Page::progress).average().toInt()
        }
    var isLocked = false

    var status: Int
        get() = download?.status ?: _status
        set(value) { _status = value }

    @Transient var download: Download? = null

    val isDownloaded: Boolean
        get() = status == Download.DOWNLOADED

    override fun getLayoutRes(): Int {
        return if (chapter.isHeader) R.layout.manga_header_item
        else R.layout.chapters_mat_item
    }

    override fun isSelectable(): Boolean {
        return chapter.isHeader
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): MangaChapterHolder {
        return if (chapter.isHeader) MangaHeaderHolder(view, adapter as ChaptersAdapter,
            startExpanded = chapter.read)
        else ChapterMatHolder(view, adapter as ChaptersAdapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: MangaChapterHolder,
                                position: Int,
                                payloads: MutableList<Any?>?) {
        holder.bind(this, manga)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is ChapterItem) {
            return chapter.id!! == other.chapter.id!!
        }
        return false
    }

    override fun hashCode(): Int {
        return chapter.id!!.hashCode()
    }

}
