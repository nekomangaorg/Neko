package eu.kanade.tachiyomi.ui.recents

import android.widget.ImageView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga

class RecentsAdapter(val delegate: RecentsInterface) :
    FlexibleAdapter<IFlexible<RecentsHolder>>(null, delegate, true) {

    interface RecentsInterface {
        fun resumeManga(manga: Manga, chapter: Chapter)
        fun showManga(manga: Manga)
        fun markAsRead(manga: Manga, chapter: Chapter)
        fun downloadChapter(item: RecentMangaItem)
        fun downloadChapterNow(chapter: Chapter)
        fun setCover(manga: Manga, view: ImageView)
        fun viewAll(position: Int)
    }
}
