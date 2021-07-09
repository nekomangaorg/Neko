package eu.kanade.tachiyomi.ui.manga

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import coil.request.CachePolicy
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.image.coil.loadManga
import eu.kanade.tachiyomi.databinding.ChapterHeaderItemBinding
import eu.kanade.tachiyomi.databinding.MangaHeaderItemBinding
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.isMerged
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.create
import eu.kanade.tachiyomi.util.system.iconicsDrawable
import eu.kanade.tachiyomi.util.system.iconicsDrawableLarge
import eu.kanade.tachiyomi.util.system.isLTR
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import java.util.Locale

@SuppressLint("ClickableViewAccessibility")
class MangaHeaderHolder(
    view: View,
    private val adapter: MangaDetailsAdapter,
    startExpanded: Boolean,
    private val isTablet: Boolean = false,
) : BaseFlexibleViewHolder(view, adapter) {

    val binding: MangaHeaderItemBinding? = try {
        MangaHeaderItemBinding.bind(view)
    } catch (e: Exception) {
        null
    }
    private val chapterBinding: ChapterHeaderItemBinding? = try {
        ChapterHeaderItemBinding.bind(view)
    } catch (e: Exception) {
        null
    }

    private var showReadingButton = true
    private var showMoreButton = true
    var hadSelection = false

    init {

        if (binding == null) {
            with(chapterBinding) {
                this ?: return@with
                chapterLayout.setOnClickListener { adapter.delegate.showChapterFilter() }
            }
        }
        with(binding) {
            this ?: return@with
            chapterLayout.setOnClickListener { adapter.delegate.showChapterFilter() }
            startReadingButton.setOnClickListener { adapter.delegate.readNextChapter() }
            topView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = adapter.delegate.topCoverHeight()
            }
            moreButton.setOnClickListener { expandDesc() }
            mangaSummary.setOnClickListener {
                if (moreButton.isVisible) {
                    expandDesc()
                } else if (!hadSelection) {
                    collapseDesc()
                } else {
                    hadSelection = false
                }
            }
            mangaSummary.setOnLongClickListener {
                if (mangaSummary.isTextSelectable && !adapter.recyclerView.canScrollVertically(
                        -1
                    )
                ) {
                    (adapter.delegate as MangaDetailsController).binding.swipeRefresh.isEnabled =
                        false
                }
                false
            }
            mangaSummary.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    view.requestFocus()
                }
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    hadSelection = mangaSummary.hasSelection()
                    (adapter.delegate as MangaDetailsController).binding.swipeRefresh.isEnabled =
                        true
                }
                false
            }
            if (!itemView.resources.isLTR) {
                moreBgGradient.rotation = 180f
            }
            lessButton.setOnClickListener { collapseDesc() }
            mangaGenresTags.setOnTagClickListener {
                adapter.delegate.tagClicked(it)
            }
            webviewButton.setOnClickListener { adapter.delegate.showExternalSheet() }
            similarButton.setOnClickListener { adapter.delegate.openSimilar() }
            mergeButton.setOnClickListener { adapter.delegate.openMerge() }

            shareButton.setOnClickListener { adapter.delegate.prepareToShareManga() }
            favoriteButton.setOnClickListener {
                adapter.delegate.favoriteManga(false)
            }

            title.setOnLongClickListener {
                adapter.delegate.copyToClipboard(title.text.toString(), R.string.title)
                true
            }
            /*mangaAuthor.setOnClickListener {
                mangaAuthor.text?.let { adapter.delegate.globalSearch(it.toString()) }
            }*/
            mangaAuthor.setOnLongClickListener {
                adapter.delegate.copyToClipboard(
                    mangaAuthor.text.toString(),
                    R.string.author
                )
                true
            }
            mangaCover.setOnClickListener { adapter.delegate.zoomImageFromThumb(coverCard) }
            trackButton.setOnClickListener { adapter.delegate.showTrackingSheet() }
            if (startExpanded) expandDesc()
            else collapseDesc()
            if (isTablet) {
                chapterLayout.isVisible = false
                expandDesc()
            }
        }
    }

    private fun expandDesc() {
        binding ?: return
        if (binding.moreButton.visibility == View.VISIBLE || isTablet) {
            binding.mangaSummary.maxLines = Integer.MAX_VALUE
            binding.mangaSummary.setTextIsSelectable(true)
            binding.mangaGenresTags.isVisible = true
            binding.lessButton.isVisible = !isTablet
            binding.moreButtonGroup.isVisible = false
            binding.title.maxLines = Integer.MAX_VALUE
            binding.mangaSummary.requestFocus()
        }
    }

    private fun collapseDesc() {
        binding ?: return
        if (isTablet) return
        binding.mangaSummary.setTextIsSelectable(false)
        binding.mangaSummary.isClickable = true
        binding.mangaSummary.maxLines = 3
        binding.mangaGenresTags.isVisible = isTablet
        binding.lessButton.isVisible = false
        binding.moreButtonGroup.isVisible = !isTablet
        binding.title.maxLines = 4
        adapter.recyclerView.post {
            adapter.delegate.updateScroll()
        }
    }

    fun bindChapters() {
        val presenter = adapter.delegate.mangaPresenter()
        val count = presenter.chapters.size
        if (binding != null) {
            binding.chaptersTitle.text =
                itemView.resources.getQuantityString(R.plurals.chapters_plural, count, count)
            binding.filtersText.text = presenter.currentFilters()
        } else if (chapterBinding != null) {
            chapterBinding.chaptersTitle.text =
                itemView.resources.getQuantityString(R.plurals.chapters_plural, count, count)
            chapterBinding.filtersText.text = presenter.currentFilters()
        }
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: MangaHeaderItem, manga: Manga) {
        val presenter = adapter.delegate.mangaPresenter()
        if (binding == null) {
            if (chapterBinding != null) {
                val count = presenter.chapters.size
                chapterBinding.chaptersTitle.text =
                    itemView.resources.getQuantityString(R.plurals.chapters_plural, count, count)
                chapterBinding.filtersText.text = presenter.currentFilters()
            }
            return
        }
        binding.title.text = manga.title

        if (manga.genre.isNullOrBlank().not()) binding.mangaGenresTags.setTags(
            manga.genre?.split(",")?.map(String::trim)
        )
        else binding.mangaGenresTags.setTags(emptyList())

        if (manga.author == manga.artist || manga.artist.isNullOrBlank()) {
            binding.mangaAuthor.text = manga.author?.trim()
        } else {
            binding.mangaAuthor.text =
                listOfNotNull(manga.author?.trim(), manga.artist?.trim()).joinToString(", ")
        }
        binding.mangaSummary.text =
            if (manga.description.isNullOrBlank()) itemView.context.getString(R.string.no_description)
            else manga.description?.trim()

        binding.mangaSummary.post {
//            if (binding.subItemGroup.isVisible) {
//                if ((binding.mangaSummary.lineCount < 3 && manga.genre.isNullOrBlank()) || binding.lessButton.isVisible) {
//                    binding.mangaSummary.setTextIsSelectable(true)
//                    binding.moreButtonGroup.isVisible = false
//                    showMoreButton = binding.lessButton.isVisible
//                } else {
//                    binding.moreButtonGroup.isVisible = true
//                }
//            }
            if (adapter.hasFilter()) collapse()
            else expand()
        }

        with(binding.favoriteButton) {
            val icon = when {
                item.isLocked -> MaterialDesignDx.Icon.gmf_lock
                item.manga.favorite -> CommunityMaterial.Icon2.cmd_heart as IIcon
                else -> CommunityMaterial.Icon2.cmd_heart_outline as IIcon
            }
            setImageDrawable(icon.create(context, 24f))
            adapter.delegate.setFavButtonPopup(this)
        }

        val tracked = presenter.isTracked() && !item.isLocked

        with(binding.trackButton) {
            setImageDrawable(
                context.iconicsDrawable(
                    MaterialDesignDx.Icon.gmf_art_track,
                    size = 32
                )
            )
        }

        with(binding.similarButton) {
            setImageDrawable(context.iconicsDrawableLarge(MaterialDesignDx.Icon.gmf_account_tree))
        }

        with(binding.mergeButton) {
            isVisible = (manga.status != SManga.COMPLETED)
            val iconics = when (manga.isMerged()) {
                true -> context.iconicsDrawableLarge(CommunityMaterial.Icon.cmd_check_decagram)
                false -> context.iconicsDrawableLarge(CommunityMaterial.Icon3.cmd_source_merge)
            }
            setImageDrawable(iconics)
        }

        with(binding.webviewButton) {
            setImageDrawable(context.iconicsDrawableLarge(CommunityMaterial.Icon3.cmd_web))
        }
        with(binding.shareButton) {
            setImageDrawable(context.iconicsDrawableLarge(MaterialDesignDx.Icon.gmf_share))
        }

        with(binding.startReadingButton) {
            val nextChapter = presenter.getNextUnreadChapter()
            isVisible = presenter.chapters.isNotEmpty() && !item.isLocked && !adapter.hasFilter()
            showReadingButton = isVisible
            isEnabled = (nextChapter != null)
            text = if (nextChapter != null) {
                val readTxt =
                    if (nextChapter.isMergedChapter() || (nextChapter.chapter.vol.isEmpty() && nextChapter.chapter.chapter_txt.isEmpty())) {
                        nextChapter.chapter.name
                    } else {
                        val vol = if (nextChapter.chapter.vol.isNotEmpty()) {
                            "Vol. " + nextChapter.chapter.vol
                        } else {
                            ""
                        }
                        vol + " " + nextChapter.chapter.chapter_txt
                    }
                resources.getString(
                    if (nextChapter.last_page_read > 0) R.string.continue_reading_
                    else R.string.start_reading_,
                    readTxt
                )
            } else {
                resources.getString(R.string.all_chapters_read)
            }
        }

        val count = presenter.chapters.size
        binding.chaptersTitle.text =
            itemView.resources.getQuantityString(R.plurals.chapters_plural, count, count)

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
                    SManga.PUBLICATION_COMPLETE -> R.string.publication_complete
                    SManga.HIATUS -> R.string.hiatus
                    SManga.CANCELLED -> R.string.cancelled
                    else -> R.string.unknown
                }
            )
            )

        binding.mangaRating.isVisible = manga.rating != null
        binding.mangaRating.text = "  " + manga.rating

        binding.mangaUsers.isVisible = manga.users != null
        binding.mangaUsers.text = "  " + manga.users

        binding.mangaMissingChapters.isVisible = manga.missing_chapters != null

        binding.mangaMissingChapters.text =
            itemView.context.getString(R.string.missing_chapters, manga.missing_chapters)

        manga.genre?.let {
            binding.r18Badge.isVisible = (it.contains("pornographic", true))
        }

        binding.mangaLangFlag.visibility = View.VISIBLE
        when (manga.lang_flag?.lowercase(Locale.US)) {
            "zh-hk" -> binding.mangaLangFlag.setImageResource(R.drawable.ic_flag_china)
            "zh" -> binding.mangaLangFlag.setImageResource(R.drawable.ic_flag_china)
            "ko" -> binding.mangaLangFlag.setImageResource(R.drawable.ic_flag_korea)
            "ja" -> binding.mangaLangFlag.setImageResource(R.drawable.ic_flag_japan)
            else -> binding.mangaLangFlag.visibility = View.GONE
        }

        binding.filtersText.text = presenter.currentFilters()

        if (!manga.initialized) return
        updateCover(manga)
    }

    fun clearDescFocus() {
        binding ?: return
        binding.mangaSummary.setTextIsSelectable(false)
        binding.mangaSummary.clearFocus()
    }

    fun setTopHeight(newHeight: Int) {
        binding ?: return
        if (newHeight == binding.topView.height) return
        binding.topView.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = newHeight
        }
    }

    fun setBackDrop(color: Int) {
        binding ?: return
        binding.trueBackdrop.setBackgroundColor(color)
    }

    fun updateTracking() {
    }

    fun collapse() {
        binding ?: return
        binding.subItemGroup.isVisible = false
        binding.startReadingButton.isVisible = false
        if (binding.moreButton.isVisible || binding.moreButton.isInvisible) {
            binding.moreButtonGroup.isInvisible = !isTablet
        } else {
            binding.lessButton.isVisible = false
            binding.mangaGenresTags.isVisible = isTablet
        }
    }

    fun updateCover(manga: Manga) {
        binding ?: return
        if (!manga.initialized) return
        val drawable = adapter.controller.binding.mangaCoverFull.drawable
        binding.mangaCover.loadManga(
            manga,
            builder = {
                placeholder(drawable)
                error(drawable)
                if (manga.favorite) networkCachePolicy(CachePolicy.READ_ONLY)
                diskCachePolicy(CachePolicy.READ_ONLY)
            }
        )
        binding.backdrop.loadManga(
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
        binding ?: return
        binding.subItemGroup.isVisible = true
        if (!showMoreButton) binding.moreButtonGroup.isVisible = false
        else {
            if (binding.mangaSummary.maxLines != Integer.MAX_VALUE) {
                binding.moreButtonGroup.isVisible = !isTablet
            } else {
                binding.lessButton.isVisible = !isTablet
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
