package eu.kanade.tachiyomi.ui.manga

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersAdapter
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.manga_header_item.*

class MangaHeaderHolder(
    private val view: View,
    private val adapter: ChaptersAdapter
) : MangaChapterHolder(view, adapter) {

    init {
        start_reading_button.setOnClickListener { adapter.coverListener?.readNextChapter() }
    }

    override fun bind(item: ChapterItem, manga: Manga) {
        manga_title.text = manga.currentTitle()
        if (manga.currentAuthor() == manga.currentArtist() ||
            manga.currentArtist().isNullOrBlank())
            manga_author.text = manga.currentAuthor()
        else {
            manga_author.text = "${manga.currentAuthor()?.trim()}, ${manga.currentArtist()}"
        }
        manga_summary.text = manga.currentDesc()
        manga_summary_label.text = "About this ${if (manga.mangaType() == Manga.TYPE_MANGA) "Manga"
        else "Manhwa"}"
        with(favorite_button) {
            icon = ContextCompat.getDrawable(
                itemView.context, when {
                    item.isLocked -> R.drawable.ic_lock_white_24dp
                    manga.favorite -> R.drawable.ic_bookmark_white_24dp
                    else -> R.drawable.ic_add_to_library_24dp
                }
            )
            text = itemView.resources.getString(
                when {
                    item.isLocked -> R.string.unlock
                    manga.favorite -> R.string.in_library
                    else -> R.string.add_to_library
                }
            )
            backgroundTintList =
                ContextCompat.getColorStateList(context, android.R.color.transparent)
            if (!item.isLocked && manga.favorite) {
                backgroundTintList =
                    ColorStateList.valueOf(
                        ColorUtils.setAlphaComponent(
                            context.getResourceColor(R.attr.colorAccent), 75))
                strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
            }
        }
        true_backdrop.setBackgroundColor(adapter.coverListener?.coverColor() ?:
        itemView.context.getResourceColor(android.R.attr.colorBackground))

        with(start_reading_button) {
            val nextChapter = adapter.coverListener?.nextChapter()
            visibleIf(nextChapter != null && !item.isLocked)
            if (nextChapter != null) {
                val number = adapter.decimalFormat.format(nextChapter.chapter_number.toDouble())
                text = resources.getString(if (nextChapter.last_page_read > 0)
                    R.string.continue_reader_chapter
                else R.string.start_reader_chapter, number)
            }
        }

        GlideApp.with(view.context).load(manga)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(manga.id!!).toString()))
            .into(manga_cover)
        GlideApp.with(view.context).load(manga)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(manga.id!!).toString()))
            .centerCrop()
            .into(backdrop)
    }

    fun setBackDrop(color: Int) {
        true_backdrop.setBackgroundColor(color)
    }
}