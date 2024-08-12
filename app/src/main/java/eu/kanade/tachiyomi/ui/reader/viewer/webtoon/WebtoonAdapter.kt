package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.hasMissingChapters

/** RecyclerView Adapter used by this [viewer] to where [ViewerChapters] updates are posted. */
class WebtoonAdapter(val viewer: WebtoonViewer) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /** List of currently set items. */
    var items: List<Any> = emptyList()
        private set

    var currentChapter: ReaderChapter? = null

    /**
     * Updates this adapter with the given [chapters]. It handles setting a few pages of the
     * next/previous chapter to allow seamless transitions.
     */
    fun setChapters(chapters: ViewerChapters, forceTransition: Boolean) {
        val newItems = mutableListOf<Any>()

        // Force chapter transition page if there are missing chapters
        val prevHasMissingChapters = hasMissingChapters(chapters.currChapter, chapters.prevChapter)
        val nextHasMissingChapters = hasMissingChapters(chapters.nextChapter, chapters.currChapter)

        // Add previous chapter pages and transition.
        if (chapters.prevChapter != null) {
            // We only need to add the last few pages of the previous chapter, because it'll be
            // selected as the current chapter when one of those pages is selected.
            val prevPages = chapters.prevChapter.pages
            if (prevPages != null) {
                newItems.addAll(prevPages.takeLast(2))
            }
        }

        // Skip transition page if the chapter is loaded & current page is not a transition page
        if (prevHasMissingChapters ||
            forceTransition ||
            chapters.prevChapter?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter))
        }

        // Add current chapter.
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            newItems.addAll(currPages)
        }

        currentChapter = chapters.currChapter

        // Add next chapter transition and pages.
        if (nextHasMissingChapters ||
            forceTransition ||
            chapters.nextChapter?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ChapterTransition.Next(chapters.currChapter, chapters.nextChapter))
        }

        if (chapters.nextChapter != null) {
            // Add at most two pages, because this chapter will be selected before the user can
            // swap more pages.
            val nextPages = chapters.nextChapter.pages
            if (nextPages != null) {
                newItems.addAll(nextPages.take(2))
            }
        }

        val result = DiffUtil.calculateDiff(Callback(items, newItems))
        items = newItems
        result.dispatchUpdatesTo(this)
    }

    /** Returns the amount of items of the adapter. */
    override fun getItemCount(): Int {
        return items.size
    }

    /** Returns the view type for the item at the given [position]. */
    override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is ReaderPage -> PAGE_VIEW
            is ChapterTransition -> TRANSITION_VIEW
            else -> error("Unknown view type for ${item.javaClass}")
        }
    }

    /** Creates a new view holder for an item with the given [viewType]. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            PAGE_VIEW -> {
                val view = ReaderPageImageView(parent.context, isWebtoon = true)
                WebtoonPageHolder(view, viewer)
            }
            TRANSITION_VIEW -> {
                val view = LinearLayout(parent.context)
                WebtoonTransitionHolder(view, viewer)
            }
            else -> error("Unknown view type")
        }
    }

    /** Binds an existing view [holder] with the item at the given [position]. */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is WebtoonPageHolder -> holder.bind(item as ReaderPage)
            is WebtoonTransitionHolder -> holder.bind(item as ChapterTransition)
        }
    }

    /** Recycles an existing view [holder] before adding it to the view pool. */
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is WebtoonPageHolder -> holder.recycle()
            is WebtoonTransitionHolder -> holder.recycle()
        }
    }

    /** Diff util callback used to dispatch delta updates instead of full dataset changes. */
    private class Callback(
        private val oldItems: List<Any>,
        private val newItems: List<Any>,
    ) : DiffUtil.Callback() {

        /** Returns true if these two items are the same. */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]

            return oldItem == newItem
        }

        /** Returns true if the contents of the items are the same. */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return true
        }

        /** Returns the size of the old list. */
        override fun getOldListSize(): Int {
            return oldItems.size
        }

        /** Returns the size of the new list. */
        override fun getNewListSize(): Int {
            return newItems.size
        }
    }

    private companion object {
        /** View holder type of a chapter page view. */
        const val PAGE_VIEW = 0

        /** View holder type of a chapter transition view. */
        const val TRANSITION_VIEW = 1
    }
}
