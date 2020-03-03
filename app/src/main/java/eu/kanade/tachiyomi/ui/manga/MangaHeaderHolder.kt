package eu.kanade.tachiyomi.ui.manga

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.manga.chapter.ChapterItem
import eu.kanade.tachiyomi.ui.manga.chapter.ChaptersAdapter
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.manga_header_item.*
import java.util.Locale

class MangaHeaderHolder(
    private val view: View,
    private val adapter: ChaptersAdapter
) : MangaChapterHolder(view, adapter) {

    init {
        start_reading_button.setOnClickListener { adapter.coverListener?.readNextChapter() }
        top_view.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = adapter.coverListener?.topCoverHeight() ?: 0
        }
        more_button.setOnClickListener { expandDesc() }
        manga_summary.setOnClickListener { expandDesc() }
        less_button.setOnClickListener {
            manga_summary.maxLines = 3
            manga_genres_tags.gone()
            less_button.gone()
            more_button_group.visible()
        }
        manga_genres_tags.setOnTagClickListener {
            adapter.coverListener?.tagClicked(it)
        }
        filter_button.setOnClickListener {
            adapter.coverListener?.showChapterFilter()
        }
        favorite_button.setOnClickListener {
            adapter.coverListener?.favoriteManga(false)
        }
        favorite_button.setOnLongClickListener {
            adapter.coverListener?.favoriteManga(true)
            true
        }
    }

    private fun expandDesc() {
        if (more_button.visibility == View.VISIBLE) {
            manga_summary.maxLines = Integer.MAX_VALUE
            manga_genres_tags.visible()
            less_button.visible()
            more_button_group.gone()
        }
    }

    override fun bind(item: ChapterItem, manga: Manga) {
        manga_full_title.text = manga.currentTitle()

        if (manga.currentGenres().isNullOrBlank().not())
            manga_genres_tags.setTags(manga.currentGenres()?.split(", ")?.map(String::trim))
        else
            manga_genres_tags.setTags(emptyList())

        if (manga.currentAuthor() == manga.currentArtist() ||
            manga.currentArtist().isNullOrBlank())
            manga_author.text = manga.currentAuthor()
        else {
            manga_author.text = "${manga.currentAuthor()?.trim()}, ${manga.currentArtist()}"
        }
        manga_summary.text = manga.currentDesc() ?: itemView.context.getString(R.string
            .no_description)

        manga_summary.post {
            if (manga_summary.lineCount < 3 && manga.currentGenres().isNullOrBlank()) {
                more_button_group.gone()
            }
        }
        manga_summary_label.text = itemView.context.getString(R.string.about_this,
            itemView.context.getString(
                when {
                    manga.mangaType() == Manga.TYPE_WEBTOON -> R.string.webtoon_viewer
                    manga.mangaType() == Manga.TYPE_MANHUA -> R.string.manhua
                    manga.mangaType() == Manga.TYPE_COMIC -> R.string.comic
                    else -> R.string.manga
                }
            ).toLowerCase(Locale.getDefault()))
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
            else strokeColor = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(
                itemView.context.getResourceColor(R.attr
                .colorOnSurface), 31))
        }
        true_backdrop.setBackgroundColor(adapter.coverListener?.coverColor() ?:
        itemView.context.getResourceColor(android.R.attr.colorBackground))

        with(start_reading_button) {
            val nextChapter = adapter.coverListener?.nextChapter()
            visibleIf(nextChapter != null && !item.isLocked)
            if (nextChapter != null) {
                val number = adapter.decimalFormat.format(nextChapter.chapter_number.toDouble())
                text = resources.getString(
                    when {
                        nextChapter.last_page_read > 0 && nextChapter.chapter_number <= 0 ->
                            R.string.continue_reading
                        nextChapter.chapter_number <= 0 -> R.string.start_reading
                        nextChapter.last_page_read > 0 -> R.string.continue_reading_chapter
                        else -> R.string.start_reader_chapter
                    }, number
                )
            }
        }

        val count = adapter.coverListener?.chapterCount() ?: 0
        chapters_title.text = itemView.resources.getQuantityString(R.plurals.chapters, count, count)

        top_view.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = adapter.coverListener?.topCoverHeight() ?: 0
        }

        manga_status.text = (itemView.context.getString( when (manga.status) {
            SManga.ONGOING -> R.string.ongoing
            SManga.COMPLETED -> R.string.completed
            SManga.LICENSED -> R.string.licensed
            else -> R.string.unknown_status
        }))
        manga_source.text = adapter.coverListener?.mangaSource()?.toString()

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

    fun setTopHeight(newHeight: Int) {
        top_view.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = newHeight
        }
    }

    fun setBackDrop(color: Int) {
        true_backdrop.setBackgroundColor(color)
    }
}