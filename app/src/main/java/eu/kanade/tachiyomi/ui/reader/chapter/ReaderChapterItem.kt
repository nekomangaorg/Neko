package eu.kanade.tachiyomi.ui.reader.chapter

import android.graphics.Typeface
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.ReaderChapterItemBinding
import eu.kanade.tachiyomi.util.chapter.ChapterUtil
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class ReaderChapterItem(val chapter: Chapter, val manga: Manga, val isCurrent: Boolean) :
    AbstractItem<ReaderChapterItem.ViewHolder>(),
    Chapter by chapter {

    val decimalFormat =
        DecimalFormat("#.###", DecimalFormatSymbols().apply { decimalSeparator = '.' })

    val preferences: PreferencesHelper by injectLazy()

    /** defines the type defining this item. must be unique. preferably an id */
    override val type: Int = R.id.reader_chapter_layout

    /** defines the layout which will be used for this item in the list */
    override val layoutRes: Int = R.layout.reader_chapter_item

    override var identifier: Long = chapter.id!!

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ReaderChapterItem>(view) {
        val binding = ReaderChapterItemBinding.bind(view)

        override fun bindView(item: ReaderChapterItem, payloads: List<Any>) {
            val manga = item.manga

            val chapterColor = ChapterUtil.chapterColor(itemView.context, item.chapter)

            val typeface = if (item.isCurrent) ResourcesCompat.getFont(
                itemView.context,
                R.font.montserrat_black
            ) else null

            binding.chapterTitle.text = if (manga.hideChapterTitle(item.preferences)) {
                val number = item.decimalFormat.format(item.chapter_number.toDouble())
                itemView.context.getString(R.string.chapter_, number)
            } else item.name

            val statuses = mutableListOf<String>()
            ChapterUtil.relativeDate(item)?.let { statuses.add(it) }
            item.scanlator?.takeIf { it.isNotBlank() }?.let { statuses.add(item.scanlator ?: "") }

            if (item.isCurrent) {
                binding.chapterTitle.setTypeface(null, Typeface.BOLD_ITALIC)
                binding.chapterSubtitle.setTypeface(null, Typeface.BOLD_ITALIC)
            } else {
                binding.chapterTitle.setTypeface(null, Typeface.NORMAL)
                binding.chapterSubtitle.setTypeface(null, Typeface.NORMAL)
            }

            if (item.chapter.language.isNullOrBlank() || item.chapter.language.equals(
                    "english",
                    true
                )
            ) {
                binding.chapterLanguage.isVisible = false
            } else {
                binding.chapterLanguage.isVisible = true
                binding.chapterLanguage.text = item.chapter.language
            }

            // match color of the chapter title
            binding.chapterTitle.setTextColor(chapterColor)
            binding.chapterSubtitle.setTextColor(chapterColor)
            binding.chapterLanguage.setTextColor(chapterColor)

            binding.bookmarkImage.setImageResource(
                if (item.bookmark) R.drawable.ic_bookmark_24dp
                else R.drawable.ic_bookmark_border_24dp
            )

            val drawableColor = ChapterUtil.bookmarkColor(itemView.context, item)

            DrawableCompat.setTint(binding.bookmarkImage.drawable, drawableColor)
            binding.chapterTitle.setTypeface(typeface)
            binding.chapterSubtitle.setTypeface(typeface)
            binding.chapterLanguage.setTypeface(typeface)
            binding.chapterSubtitle.text = statuses.joinToString(" • ")
        }

        override fun unbindView(item: ReaderChapterItem) {
            binding.chapterTitle.text = null
            binding.chapterSubtitle.text = null
            binding.chapterLanguage.text = null
        }
    }
}
