package eu.kanade.tachiyomi.ui.reader.chapter

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.visible
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class ReaderChapterItem(val chapter: Chapter, val manga: Manga, val isCurrent: Boolean) :
    AbstractItem<ReaderChapterItem.ViewHolder>(),
    Chapter by chapter {

    val decimalFormat =
        DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })

    /** defines the type defining this item. must be unique. preferably an id */
    override val type: Int = R.id.reader_chapter_layout

    /** defines the layout which will be used for this item in the list */
    override val layoutRes: Int = R.layout.reader_chapter_item

    override var identifier: Long = chapter.id!!

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ReaderChapterItem>(view) {
        private var chapterTitle: TextView = view.findViewById(R.id.chapter_title)
        private var chapterSubtitle: TextView = view.findViewById(R.id.chapter_scanlator)
        private var chapterLanguage: TextView = view.findViewById(R.id.chapter_language)
        var bookmarkButton: FrameLayout = view.findViewById(R.id.bookmark_layout)
        private var bookmarkImage: ImageView = view.findViewById(R.id.bookmark_image)

        override fun bindView(item: ReaderChapterItem, payloads: List<Any>) {
            val manga = item.manga

            val chapterColor =
                if (item.isCurrent) itemView.context.getColor(R.color.neko_green_darker)
                else ChapterUtil.chapterColor(itemView.context, item.chapter)

            val typeface = if (item.isCurrent) ResourcesCompat.getFont(itemView.context, R.font.metropolis_bold_italic) else null

            chapterTitle.text = when (manga.displayMode) {
                Manga.DISPLAY_NUMBER -> {
                    val number = item.decimalFormat.format(item.chapter_number.toDouble())
                    itemView.context.getString(R.string.chapter_, number)
                }
                else -> item.name
            }

            val statuses = mutableListOf<String>()
            ChapterUtil.relativeDate(item)?.let { statuses.add(it) }
            item.scanlator?.isNotBlank()?.let { statuses.add(item.scanlator!!) }

            if (item.chapter.language.isNullOrBlank() || item.chapter.language.equals("english", true)) {
                chapterLanguage.gone()
            } else {
                chapterLanguage.visible()
                chapterLanguage.text = item.chapter.language
            }

            // match color of the chapter title
            chapterTitle.setTextColor(chapterColor)
            chapterSubtitle.setTextColor(chapterColor)
            chapterLanguage.setTextColor(chapterColor)

            bookmarkImage.setImageResource(
                if (item.bookmark) R.drawable.ic_bookmark_24dp
                else R.drawable.ic_bookmark_border_24dp
            )

            val drawableColor = ChapterUtil.bookmarkColor(itemView.context, item)

            DrawableCompat.setTint(bookmarkImage.drawable, drawableColor)

            chapterTitle.setTypeface(typeface)
            chapterSubtitle.setTypeface(typeface)
            chapterLanguage.setTypeface(typeface)
            chapterSubtitle.text = statuses.joinToString(" • ")
        }

        override fun unbindView(item: ReaderChapterItem) {
            chapterTitle.text = null
            chapterSubtitle.text = null
            chapterLanguage.text = null
        }
    }
}
