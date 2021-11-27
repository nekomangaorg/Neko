package eu.kanade.tachiyomi.ui.manga

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import androidx.core.text.isDigitsOnly
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.transition.TransitionSet
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import coil.request.CachePolicy
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
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
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.create
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.iconicsDrawable
import eu.kanade.tachiyomi.util.system.iconicsDrawableLarge
import eu.kanade.tachiyomi.util.system.isInNightMode
import eu.kanade.tachiyomi.util.system.isLTR
import eu.kanade.tachiyomi.util.view.resetStrokeColor
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
    private var canCollapse = true

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
            moreButton.setOnClickListener {
                expandDesc(true)
            }
            mangaSummary.setOnClickListener {
                if (moreButton.isVisible) {
                    expandDesc(true)
                } else if (!hadSelection) {
                    collapseDesc(true)
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
            lessButton.setOnClickListener {
                collapseDesc(true)
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
            applyBlur()
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

    private fun applyBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding?.backdrop?.alpha = 0.2f
            binding?.backdrop?.setRenderEffect(
                RenderEffect.createBlurEffect(
                    20f,
                    20f,
                    Shader.TileMode.MIRROR
                )
            )
        }
    }

    private fun expandDesc(animated: Boolean = false) {
        binding ?: return
        if (binding.moreButton.visibility == View.VISIBLE || isTablet) {
            androidx.transition.TransitionManager.endTransitions(adapter.controller.binding.recycler)
            binding.mangaSummary.maxLines = Integer.MAX_VALUE
            binding.mangaSummary.setTextIsSelectable(true)
            setDescription()
            binding.mangaGenresTags.isVisible = true
            binding.lessButton.isVisible = !isTablet
            binding.moreButtonGroup.isVisible = false
            if (animated) {
                val animVector = AnimatedVectorDrawableCompat.create(binding.root.context,
                    R.drawable.anim_expand_more_to_less)
                binding.lessButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                    null,
                    animVector,
                    null)
                animVector?.start()
            }
            binding.title.maxLines = Integer.MAX_VALUE
            binding.mangaAuthor.maxLines = Integer.MAX_VALUE
            binding.mangaSummary.requestFocus()
            if (animated) {
                val transition = TransitionSet()
                    .addTransition(androidx.transition.ChangeBounds())
                    .addTransition(androidx.transition.Fade())
                    .addTransition(androidx.transition.Slide())
                transition.duration = binding.root.resources.getInteger(
                    android.R.integer.config_shortAnimTime
                ).toLong()
                androidx.transition.TransitionManager.beginDelayedTransition(
                    adapter.controller.binding.recycler,
                    transition
                )
            }
        }
    }

    private fun collapseDesc(animated: Boolean = false) {
        binding ?: return
        if (isTablet || !canCollapse) return
        binding.moreButtonGroup.isVisible = !isTablet
        if (animated) {
            androidx.transition.TransitionManager.endTransitions(adapter.controller.binding.recycler)
            val animVector = AnimatedVectorDrawableCompat.create(
                binding.root.context,
                R.drawable.anim_expand_less_to_more
            )
            binding.moreButton.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                animVector,
                null
            )
            animVector?.start()
            val transition = TransitionSet()
                .addTransition(androidx.transition.ChangeBounds())
                .addTransition(androidx.transition.Fade())
            transition.duration = binding.root.resources.getInteger(
                android.R.integer.config_shortAnimTime
            ).toLong()
            androidx.transition.TransitionManager.beginDelayedTransition(
                adapter.controller.binding.recycler,
                transition
            )
        }
        binding.mangaSummary.setTextIsSelectable(false)
        binding.mangaSummary.isClickable = true
        binding.mangaSummary.maxLines = 3
        setDescription()
        binding.mangaGenresTags.isVisible = isTablet
        binding.lessButton.isVisible = false
        binding.title.maxLines = 4
        binding.mangaAuthor.maxLines = 2
        adapter.recyclerView.post {
            adapter.delegate.updateScroll()
        }
    }

    private fun setDescription() {
        if (binding != null) {
            val desc = adapter.controller.mangaPresenter().manga.description
            binding.mangaSummary.text = when {
                desc.isNullOrBlank() -> itemView.context.getString(R.string.no_description)
                binding.mangaSummary.maxLines != Int.MAX_VALUE -> desc.replace(
                    Regex(
                        "[\\r\\n\\s*]{2,}",
                        setOf(RegexOption.MULTILINE)
                    ),
                    "\n"
                )
                else -> desc.trim()
            }
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
        binding.title.text =
            "This is a very long title, This is a very long title, This is a very long title, This is a very long title ,This is a very long title" //manga.title

        setGenreTags(binding, manga)


        if (manga.author == manga.artist || manga.artist.isNullOrBlank()) {
            binding.mangaAuthor.text = manga.author?.trim()
        } else {
            binding.mangaAuthor.text =
                listOfNotNull(manga.author?.trim(), manga.artist?.trim()).joinToString(", ")
        }

        if (MdUtil.getMangaId(manga.url).isDigitsOnly()) {
            manga.description = "THIS MANGA IS NOT MIGRATED TO V5"
        }

        setDescription()



        binding.mangaSummary.post {
            if (binding.subItemGroup.isVisible) {
                if (binding.mangaSummary.lineCount < 3 && manga.genre.isNullOrBlank() &&
                    binding.moreButton.isVisible && manga.initialized
                ) {
                    expandDesc()
                    binding.lessButton.isVisible = false
                    showMoreButton = binding.lessButton.isVisible
                    canCollapse = false
                }
            }
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
        binding.trueBackdrop.setBackgroundColor(
            adapter.delegate.coverColor()
                ?: itemView.context.getResourceColor(R.attr.background)
        )

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
        if (adapter.preferences.themeMangaDetails()) {
            updateColors(false)
        }
    }

    private fun setGenreTags(binding: MangaHeaderItemBinding, manga: Manga) {
        with(binding.mangaGenresTags) {
            removeAllViews()
            val dark = context.isInNightMode()
            val amoled = adapter.delegate.mangaPresenter().preferences.themeDarkAmoled().get()
            val baseTagColor = context.getResourceColor(R.attr.background)
            val bgArray = FloatArray(3)
            val accentArray = FloatArray(3)

            ColorUtils.colorToHSL(baseTagColor, bgArray)
            ColorUtils.colorToHSL(adapter.delegate.accentColor()
                ?: context.getResourceColor(R.attr.colorSecondary), accentArray)
            val downloadedColor = ColorUtils.setAlphaComponent(
                ColorUtils.HSLToColor(
                    floatArrayOf(
                        if (adapter.delegate.accentColor() != null) accentArray[0] else bgArray[0],
                        bgArray[1],
                        (
                            when {
                                amoled && dark -> 0.1f
                                dark -> 0.225f
                                else -> 0.85f
                            }
                            )
                    )
                ),
                199
            )
            val textColor = ColorUtils.HSLToColor(
                floatArrayOf(
                    accentArray[0],
                    accentArray[1],
                    if (dark) 0.945f else 0.175f
                )
            )
            if (manga.genre.isNullOrBlank().not()) {
                (manga.getGenres() ?: emptyList()).map { genreText ->
                    val chip = LayoutInflater.from(binding.root.context).inflate(
                        R.layout.genre_chip,
                        this,
                        false
                    ) as Chip
                    val id = View.generateViewId()
                    chip.id = id
                    chip.chipBackgroundColor = ColorStateList.valueOf(downloadedColor)
                    chip.setTextColor(context.getColor(R.color.material_on_surface_emphasis_medium))
                    chip.text = genreText
                    chip.setOnClickListener {
                        adapter.delegate.tagClicked(genreText)
                    }
                    chip.setOnLongClickListener {
                        adapter.delegate.tagLongClicked(genreText)
                        true
                    }

                    this.addView(chip)
                }
            }
        }
    }

    fun clearDescFocus() {
        binding ?: return
        binding.mangaSummary.setTextIsSelectable(false)
        binding.mangaSummary.clearFocus()
    }

    private fun MaterialButton.checked(checked: Boolean) {
        if (checked) {
            stateListAnimator =
                AnimatorInflater.loadStateListAnimator(context, R.animator.icon_btn_state_list_anim)
            backgroundTintList = ColorStateList.valueOf(
                ColorUtils.blendARGB(
                    adapter.delegate.accentColor()
                        ?: context.getResourceColor(R.attr.colorSecondary),
                    context.getResourceColor(R.attr.background),
                    0.706f
                )
            )
            strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)
        } else {
            stateListAnimator = null
            resetStrokeColor()
            backgroundTintList =
                ColorStateList.valueOf(context.getResourceColor(R.attr.background))
        }
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

    fun updateColors(updateAll: Boolean = true) {
        binding ?: return
        val accentColor = adapter.delegate.accentColor() ?: return
        val manga = adapter.presenter.manga
        with(binding) {
            trueBackdrop.setBackgroundColor(
                adapter.delegate.coverColor()
                    ?: trueBackdrop.context.getResourceColor(R.attr.background)
            )
            TextViewCompat.setCompoundDrawableTintList(moreButton,
                ColorStateList.valueOf(accentColor))
            moreButton.setTextColor(accentColor)
            TextViewCompat.setCompoundDrawableTintList(lessButton,
                ColorStateList.valueOf(accentColor))
            lessButton.setTextColor(accentColor)
            shareButton.imageTintList = ColorStateList.valueOf(accentColor)
            webviewButton.imageTintList = ColorStateList.valueOf(accentColor)
            filterButton.imageTintList = ColorStateList.valueOf(accentColor)
            mergeButton.imageTintList = ColorStateList.valueOf(accentColor)
            trackButton.imageTintList = ColorStateList.valueOf(accentColor)
            favoriteButton.imageTintList = ColorStateList.valueOf(accentColor)
            similarButton.imageTintList = ColorStateList.valueOf(accentColor)

            val states = arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf()
            )

            val colors = intArrayOf(
                ColorUtils.setAlphaComponent(root.context.getResourceColor(R.attr.tabBarIconInactive),
                    43),
                accentColor
            )

            startReadingButton.backgroundTintList = ColorStateList(states, colors)

            val textColors = intArrayOf(
                ColorUtils.setAlphaComponent(root.context.getResourceColor(R.attr.colorOnSurface),
                    97),
                root.context.getResourceColor(android.R.attr.textColorPrimaryInverse)
            )
            startReadingButton.setTextColor(ColorStateList(states, textColors))
            if (updateAll) {
                // trackButton.checked(trackButton.stateListAnimator != null)
                // favoriteButton.checked(favoriteButton.stateListAnimator != null)
                setGenreTags(this, manga)
            }
        }
    }

    fun updateTracking() {
    }

    fun collapse() {
        binding ?: return
        if (!canCollapse) return
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
                target(
                    onSuccess = {
                        val bitmap = (it as? BitmapDrawable)?.bitmap
                        if (bitmap == null) {
                            binding.backdrop.setImageDrawable(it)
                            return@target
                        }
                        val yOffset = (bitmap.height / 2 * 0.33).toInt()

                        binding.backdrop.setImageDrawable(
                            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height - yOffset)
                                .toDrawable(itemView.resources)
                        )
                        applyBlur()
                    }
                )
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
