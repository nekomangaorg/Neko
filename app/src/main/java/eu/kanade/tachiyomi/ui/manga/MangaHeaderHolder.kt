package eu.kanade.tachiyomi.ui.manga

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil.loadAny
import coil.request.CachePolicy
import com.google.android.material.button.MaterialButton
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.MangaHeaderItemBinding
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.isLTR
import eu.kanade.tachiyomi.util.view.resetStrokeColor
import eu.kanade.tachiyomi.util.view.updateLayoutParams

@SuppressLint("ClickableViewAccessibility")
class MangaHeaderHolder(
    private val view: View,
    private val adapter: MangaDetailsAdapter,
    startExpanded: Boolean
) : BaseFlexibleViewHolder(view, adapter) {

    val binding = MangaHeaderItemBinding.bind(view)

    private var showReadingButton = true
    private var showMoreButton = true
    var hadSelection = false

    init {
        binding.chapterLayout.setOnClickListener { adapter.delegate.showChapterFilter() }
        binding.startReadingButton.setOnClickListener { adapter.delegate.readNextChapter() }
        binding.topView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = adapter.delegate.topCoverHeight()
        }
        binding.moreButton.setOnClickListener { expandDesc() }
        binding.mangaSummary.setOnClickListener {
            if (binding.moreButton.isVisible) {
                expandDesc()
            } else if (!hadSelection) {
                collapseDesc()
            } else {
                hadSelection = false
            }
        }
        binding.mangaSummary.setOnLongClickListener {
            if (binding.mangaSummary.isTextSelectable && !adapter.recyclerView.canScrollVertically(-1)) {
                (adapter.delegate as MangaDetailsController).binding.swipeRefresh.isEnabled = false
            }
            false
        }
        binding.mangaSummary.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                view.requestFocus()
            }
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                hadSelection = binding.mangaSummary.hasSelection()
                (adapter.delegate as MangaDetailsController).binding.swipeRefresh.isEnabled =
                    true
            }
            false
        }
        if (!itemView.resources.isLTR) {
            binding.moreBgGradient.rotation = 180f
        }
        binding.lessButton.setOnClickListener { collapseDesc() }
        binding.mangaGenresTags.setOnTagClickListener {
            adapter.delegate.tagClicked(it)
        }
        binding.webviewButton.setOnClickListener { adapter.delegate.openInWebView() }
        binding.shareButton.setOnClickListener { adapter.delegate.prepareToShareManga() }
        binding.favoriteButton.setOnClickListener {
            adapter.delegate.favoriteManga(false)
        }
        binding.title.setOnClickListener {
            binding.title.text?.let { adapter.delegate.globalSearch(it.toString()) }
        }
        binding.title.setOnLongClickListener {
            adapter.delegate.copyToClipboard(binding.title.text.toString(), R.string.title)
            true
        }
        binding.mangaAuthor.setOnClickListener {
            binding.mangaAuthor.text?.let { adapter.delegate.globalSearch(it.toString()) }
        }
        binding.mangaAuthor.setOnLongClickListener {
            adapter.delegate.copyToClipboard(binding.mangaAuthor.text.toString(), R.string.author)
            true
        }
        binding.mangaCover.setOnClickListener { adapter.delegate.zoomImageFromThumb(binding.coverCard) }
        binding.trackButton.setOnClickListener { adapter.delegate.showTrackingSheet() }
        if (startExpanded) expandDesc()
        else collapseDesc()
    }

    private fun expandDesc() {
        if (binding.moreButton.visibility == View.VISIBLE) {
            binding.mangaSummary.maxLines = Integer.MAX_VALUE
            binding.mangaSummary.setTextIsSelectable(true)
            binding.mangaGenresTags.isVisible = true
            binding.lessButton.isVisible = true
            binding.moreButtonGroup.isVisible = false
            binding.title.maxLines = Integer.MAX_VALUE
        }
    }

    private fun collapseDesc() {
        binding.mangaSummary.setTextIsSelectable(false)
        binding.mangaSummary.isClickable = true
        binding.mangaSummary.maxLines = 3
        binding.mangaGenresTags.isVisible = false
        binding.lessButton.isVisible = false
        binding.moreButtonGroup.isVisible = true
        binding.title.maxLines = 4
        adapter.recyclerView.post {
            adapter.delegate.updateScroll()
        }
    }

    fun bindChapters() {
        val presenter = adapter.delegate.mangaPresenter()
        val count = presenter.chapters.size
        binding.chaptersTitle.text = itemView.resources.getQuantityString(R.plurals.chapters_plural, count, count)
        binding.filtersText.text = presenter.currentFilters()
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: MangaHeaderItem, manga: Manga) {
        val presenter = adapter.delegate.mangaPresenter()
        binding.title.text = manga.title

        if (manga.genre.isNullOrBlank().not()) binding.mangaGenresTags.setTags(
            manga.genre?.split(",")?.map(String::trim)
        )
        else binding.mangaGenresTags.setTags(emptyList())

        if (manga.author == manga.artist || manga.artist.isNullOrBlank()) {
            binding.mangaAuthor.text = manga.author?.trim()
        } else {
            binding.mangaAuthor.text = listOfNotNull(manga.author?.trim(), manga.artist?.trim()).joinToString(", ")
        }
        binding.mangaSummary.text =
            if (manga.description.isNullOrBlank()) itemView.context.getString(R.string.no_description)
            else manga.description?.trim()

        binding.mangaSummary.post {
            binding.mangaSummary
            if (binding.subItemGroup.visibility != View.GONE) {
                if ((binding.mangaSummary.lineCount < 3 && manga.genre.isNullOrBlank()) || binding.lessButton.isVisible) {
                    binding.mangaSummary.setTextIsSelectable(true)
                    binding.moreButtonGroup.isVisible = false
                    showMoreButton = binding.lessButton.isVisible
                } else {
                    binding.moreButtonGroup.isVisible = true
                }
            }
            if (adapter.hasFilter()) collapse()
            else expand()
        }
        binding.mangaSummaryLabel.text = itemView.context.getString(
            R.string.about_this_,
            manga.seriesType(itemView.context)
        )
        with(binding.favoriteButton) {
            icon = ContextCompat.getDrawable(
                itemView.context,
                when {
                    item.isLocked -> R.drawable.ic_lock_24dp
                    manga.favorite -> R.drawable.ic_heart_24dp
                    else -> R.drawable.ic_heart_outline_24dp
                }
            )
            text = itemView.resources.getString(
                when {
                    item.isLocked -> R.string.unlock
                    manga.favorite -> R.string.in_library
                    else -> R.string.add_to_library
                }
            )
            checked(!item.isLocked && manga.favorite)
            adapter.delegate.setFavButtonPopup(this)
        }
        binding.trueBackdrop.setBackgroundColor(
            adapter.delegate.coverColor()
                ?: itemView.context.getResourceColor(android.R.attr.colorBackground)
        )

        val tracked = presenter.isTracked() && !item.isLocked

        with(binding.trackButton) {
            isVisible = presenter.hasTrackers()
            text = itemView.context.getString(
                if (tracked) R.string.tracked
                else R.string.tracking
            )

            icon = ContextCompat.getDrawable(
                itemView.context,
                if (tracked) R.drawable.ic_check_24dp else R.drawable.ic_sync_24dp
            )
            checked(tracked)
        }

        with(binding.startReadingButton) {
            val nextChapter = presenter.getNextUnreadChapter()
            isVisible = presenter.chapters.isNotEmpty() && !item.isLocked && !adapter.hasFilter()
            showReadingButton = isVisible
            isEnabled = (nextChapter != null)
            text = if (nextChapter != null) {
                val number = adapter.decimalFormat.format(nextChapter.chapter_number.toDouble())
                if (nextChapter.chapter_number > 0) resources.getString(
                    if (nextChapter.last_page_read > 0) R.string.continue_reading_chapter_
                    else R.string.start_reading_chapter_,
                    number
                )
                else {
                    resources.getString(
                        if (nextChapter.last_page_read > 0) R.string.continue_reading
                        else R.string.start_reading
                    )
                }
            } else {
                resources.getString(R.string.all_chapters_read)
            }
        }

        val count = presenter.chapters.size
        binding.chaptersTitle.text = itemView.resources.getQuantityString(R.plurals.chapters_plural, count, count)

        binding.topView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = adapter.delegate.topCoverHeight()
        }

        binding.mangaStatus.isVisible = manga.status != 0
        binding.mangaStatus.text = (
            itemView.context.getString(
                when (manga.status) {
                    SManga.ONGOING -> R.string.ongoing
                    SManga.COMPLETED -> R.string.completed
                    SManga.LICENSED -> R.string.licensed
                    else -> R.string.unknown_status
                }
            )
            )
        binding.mangaSource.text = presenter.source.toString()

        binding.filtersText.text = presenter.currentFilters()

        if (manga.source == LocalSource.ID) {
            binding.webviewButton.isVisible = false
            binding.shareButton.isVisible = false
        }

        if (!manga.initialized) return
        updateCover(manga)
    }

    private fun MaterialButton.checked(checked: Boolean) {
        if (checked) {
            backgroundTintList = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(
                    context.getResourceColor(R.attr.colorAccent),
                    75
                )
            )
            strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
        } else {
            resetStrokeColor()
            backgroundTintList =
                ContextCompat.getColorStateList(context, android.R.color.transparent)
        }
    }

    fun setTopHeight(newHeight: Int) {
        binding.topView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = newHeight
        }
    }

    fun setBackDrop(color: Int) {
        binding.trueBackdrop.setBackgroundColor(color)
    }

    fun updateTracking() {
        val presenter = adapter.delegate.mangaPresenter()
        val tracked = presenter.isTracked()
        with(binding.trackButton) {
            text = itemView.context.getString(
                if (tracked) R.string.tracked
                else R.string.tracking
            )

            icon = ContextCompat.getDrawable(
                itemView.context,
                if (tracked) R.drawable
                    .ic_check_24dp else R.drawable.ic_sync_24dp
            )
            checked(tracked)
        }
    }

    fun collapse() {
        binding.subItemGroup.isVisible = false
        binding.startReadingButton.isVisible = false
        if (binding.moreButton.isVisible || binding.moreButton.isInvisible) {
            binding.moreButtonGroup.isInvisible = true
        } else {
            binding.lessButton.isVisible = false
            binding.mangaGenresTags.isVisible = false
        }
    }

    fun updateCover(manga: Manga) {
        if (!manga.initialized) return
        val drawable = adapter.controller.binding.mangaCoverFull.drawable
        binding.mangaCover.loadAny(
            manga,
            builder = {
                placeholder(drawable)
                error(drawable)
                if (manga.favorite) networkCachePolicy(CachePolicy.READ_ONLY)
                diskCachePolicy(CachePolicy.READ_ONLY)
            }
        )
        binding.backdrop.loadAny(
            manga,
            builder = {
                placeholder(drawable)
                error(drawable)
                if (manga.favorite) networkCachePolicy(CachePolicy.READ_ONLY)
                diskCachePolicy(CachePolicy.READ_ONLY)
            }
        )
    }

    fun expand() {
        binding.subItemGroup.isVisible = true
        if (!showMoreButton) binding.moreButtonGroup.isVisible = false
        else {
            if (binding.mangaSummary.maxLines != Integer.MAX_VALUE) binding.moreButtonGroup.isVisible = true
            else {
                binding.lessButton.isVisible = true
                binding.mangaGenresTags.isVisible = true
            }
        }
        binding.startReadingButton.isVisible = showReadingButton
    }

    override fun onLongClick(view: View?): Boolean {
        super.onLongClick(view)
        return false
    }
}
