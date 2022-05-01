package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.databinding.ReaderTransitionViewBinding
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.util.system.contextCompatDrawable
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlin.math.roundToInt

class ReaderTransitionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val binding: ReaderTransitionViewBinding =
        ReaderTransitionViewBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(transition: ChapterTransition, downloadManager: DownloadManager, manga: Manga?) {
        manga ?: return
        when (transition) {
            is ChapterTransition.Prev -> bindPrevChapterTransition(transition, downloadManager, manga)
            is ChapterTransition.Next -> bindNextChapterTransition(transition, downloadManager, manga)
        }

        missingChapterWarning(transition)
    }

    /**
     * Binds a previous chapter transition on this view and subscribes to the page load status.
     */
    private fun bindPrevChapterTransition(
        transition: ChapterTransition,
        downloadManager: DownloadManager,
        manga: Manga,
    ) {
        val prevChapter = transition.to

        binding.lowerText.isVisible = prevChapter != null
        if (prevChapter != null) {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_TEXT_START
            val isPrevDownloaded = downloadManager.isChapterDownloaded(prevChapter.chapter, manga)
            val isCurrentDownloaded = downloadManager.isChapterDownloaded(transition.from.chapter, manga)
            binding.upperText.text = buildSpannedString {
                bold { append(context.getString(R.string.previous_title)) }
                append("\n${prevChapter.chapter.name}")
                if (isPrevDownloaded != isCurrentDownloaded) addDLImageSpan(isPrevDownloaded)
            }
            binding.lowerText.text = buildSpannedString {
                bold { append(context.getString(R.string.current_chapter)) }
                append("\n${transition.from.chapter.name}")
            }
        } else {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_CENTER
            binding.upperText.text = context.getString(R.string.theres_no_previous_chapter)
        }
    }

    /**
     * Binds a next chapter transition on this view and subscribes to the load status.
     */
    private fun bindNextChapterTransition(
        transition: ChapterTransition,
        downloadManager: DownloadManager,
        manga: Manga,
    ) {
        val nextChapter = transition.to

        binding.lowerText.isVisible = nextChapter != null
        if (nextChapter != null) {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_TEXT_START
            val isCurrentDownloaded = downloadManager.isChapterDownloaded(transition.from.chapter, manga)
            val isNextDownloaded = downloadManager.isChapterDownloaded(nextChapter.chapter, manga)
            binding.upperText.text = buildSpannedString {
                bold { append(context.getString(R.string.finished_chapter)) }
                append("\n${transition.from.chapter.name}")
            }
            binding.lowerText.text = buildSpannedString {
                bold { append(context.getString(R.string.next_title)) }
                append("\n${nextChapter.chapter.name}")
                if (isNextDownloaded != isCurrentDownloaded) addDLImageSpan(isNextDownloaded)
            }
        } else {
            binding.upperText.textAlignment = TEXT_ALIGNMENT_CENTER
            binding.upperText.text = context.getString(R.string.theres_no_next_chapter)
        }
    }

    private fun SpannableStringBuilder.addDLImageSpan(isDownloaded: Boolean) {
        val icon = context.contextCompatDrawable(
            if (isDownloaded) R.drawable.ic_file_download_24dp else R.drawable.ic_cloud_24dp
        )
            ?.mutate()
            ?.apply {
                val size = binding.lowerText.textSize + 4f.dpToPx
                setTint(binding.lowerText.currentTextColor)
                setBounds(0, 0, size.roundToInt(), size.roundToInt())
            } ?: return
        append(" ")
        inSpans(ImageSpan(icon)) { append("image") }
    }

    fun setTextColors(@ColorInt color: Int) {
        binding.upperText.setTextColor(color)
        binding.warningText.setTextColor(color)
        binding.lowerText.setTextColor(color)
    }

    private fun missingChapterWarning(transition: ChapterTransition) {
        if (transition.to == null) {
            binding.warning.isVisible = false
            return
        }

        val hasMissingChapters = when (transition) {
            is ChapterTransition.Prev -> hasMissingChapters(transition.from, transition.to)
            is ChapterTransition.Next -> hasMissingChapters(transition.to, transition.from)
        }

        if (!hasMissingChapters) {
            binding.warning.isVisible = false
            return
        }

        val chapterDifference = when (transition) {
            is ChapterTransition.Prev -> calculateChapterDifference(transition.from, transition.to)
            is ChapterTransition.Next -> calculateChapterDifference(transition.to, transition.from)
        }

        binding.warningText.text = resources.getQuantityString(R.plurals.missing_chapters_warning, chapterDifference.toInt(), chapterDifference.toInt())
        binding.warning.isVisible = true
    }
}
