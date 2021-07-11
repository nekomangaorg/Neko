package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.ChapterFilterLayoutBinding
import eu.kanade.tachiyomi.widget.TriStateCheckBox

class ChapterFilterLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    lateinit var binding: ChapterFilterLayoutBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = ChapterFilterLayoutBinding.bind(this)
        binding.showAll.setOnCheckedChangeListener(::checkedFilter)
        binding.showUnread.setOnCheckedChangeListener(::checkedFilter)
        binding.showDownload.setOnCheckedChangeListener(::checkedFilter)
        binding.showBookmark.setOnCheckedChangeListener(::checkedFilter)
    }

    private fun checkedFilter(checkBox: TriStateCheckBox, state: TriStateCheckBox.State) {
        if (state != TriStateCheckBox.State.UNCHECKED) {
            if (binding.showAll == checkBox && state == TriStateCheckBox.State.CHECKED) {
                binding.showUnread.animateDrawableToState(TriStateCheckBox.State.UNCHECKED)
                binding.showDownload.animateDrawableToState(TriStateCheckBox.State.UNCHECKED)
                binding.showBookmark.animateDrawableToState(TriStateCheckBox.State.UNCHECKED)
            } else {
                if (binding.showAll == checkBox) {
                    binding.showAll.state = TriStateCheckBox.State.CHECKED
                } else {
                    binding.showAll.animateDrawableToState(TriStateCheckBox.State.UNCHECKED)
                }
            }
        } else if (
            binding.showUnread.isUnchecked &&
            binding.showDownload.isUnchecked &&
            binding.showBookmark.isUnchecked
        ) {
            binding.showAll.animateDrawableToState(TriStateCheckBox.State.CHECKED)
        }
    }

    fun setCheckboxes(manga: Manga) {
        binding.showUnread.state = when (manga.readFilter) {
            Manga.CHAPTER_SHOW_UNREAD -> TriStateCheckBox.State.CHECKED
            Manga.CHAPTER_SHOW_READ -> TriStateCheckBox.State.INVERSED
            else -> TriStateCheckBox.State.UNCHECKED
        }
        binding.showDownload.state = when (manga.downloadedFilter) {
            Manga.CHAPTER_SHOW_DOWNLOADED -> TriStateCheckBox.State.CHECKED
            Manga.CHAPTER_SHOW_NOT_DOWNLOADED -> TriStateCheckBox.State.INVERSED
            else -> TriStateCheckBox.State.UNCHECKED
        }
        binding.showBookmark.state = when (manga.bookmarkedFilter) {
            Manga.CHAPTER_SHOW_BOOKMARKED -> TriStateCheckBox.State.CHECKED
            Manga.CHAPTER_SHOW_NOT_BOOKMARKED -> TriStateCheckBox.State.INVERSED
            else -> TriStateCheckBox.State.UNCHECKED
        }

        binding.showAll.isChecked = binding.showUnread.isUnchecked &&
            binding.showDownload.isUnchecked &&
            binding.showBookmark.isUnchecked
    }
}
