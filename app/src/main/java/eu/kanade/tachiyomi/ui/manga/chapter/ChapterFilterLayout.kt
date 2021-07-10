package eu.kanade.tachiyomi.ui.manga.chapter

import android.content.Context
import android.util.AttributeSet
import android.widget.CompoundButton
import android.widget.LinearLayout
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.ChapterFilterLayoutBinding

class ChapterFilterLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    lateinit var binding: ChapterFilterLayoutBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = ChapterFilterLayoutBinding.bind(this)
        binding.showAll.setOnCheckedChangeListener(::checkedFilter)
        binding.showRead.setOnCheckedChangeListener(::checkedFilter)
        binding.showUnread.setOnCheckedChangeListener(::checkedFilter)
        binding.showDownload.setOnCheckedChangeListener(::checkedFilter)
        binding.showBookmark.setOnCheckedChangeListener(::checkedFilter)
    }

    private fun checkedFilter(checkBox: CompoundButton, isChecked: Boolean) {
        if (isChecked) {
            if (binding.showAll == checkBox) {
                binding.showRead.isChecked = false
                binding.showUnread.isChecked = false
                binding.showDownload.isChecked = false
                binding.showBookmark.isChecked = false
            } else {
                binding.showAll.isChecked = false
                if (binding.showRead == checkBox) binding.showUnread.isChecked = false
                else if (binding.showUnread == checkBox) binding.showRead.isChecked = false
            }
        } else if (!binding.showRead.isChecked && !binding.showUnread.isChecked && !binding.showDownload.isChecked && !binding.showBookmark.isChecked) {
            binding.showAll.isChecked = true
        }
    }

    fun setCheckboxes(manga: Manga) {
        binding.showRead.isChecked = manga.readFilter == Manga.CHAPTER_SHOW_READ
        binding.showUnread.isChecked = manga.readFilter == Manga.CHAPTER_SHOW_UNREAD
        binding.showDownload.isChecked = manga.downloadedFilter == Manga.CHAPTER_SHOW_DOWNLOADED
        binding.showBookmark.isChecked = manga.bookmarkedFilter == Manga.CHAPTER_SHOW_BOOKMARKED

        binding.showAll.isChecked = !(
            binding.showRead.isChecked || binding.showUnread.isChecked ||
                binding.showDownload.isChecked || binding.showBookmark.isChecked
            )
    }
}
