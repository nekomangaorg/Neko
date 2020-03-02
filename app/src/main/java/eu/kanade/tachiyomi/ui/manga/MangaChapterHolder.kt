package eu.kanade.tachiyomi.ui.manga

import android.view.View
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersAdapter

abstract class MangaChapterHolder(
    private val view: View,
    private val adapter: ChaptersAdapter
) : BaseFlexibleViewHolder(view, adapter) {
    /**
     * Method called from [ChaptersAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param item the manga item to bind.
     */
    abstract fun bind(item: ChapterItem, manga: Manga)
}