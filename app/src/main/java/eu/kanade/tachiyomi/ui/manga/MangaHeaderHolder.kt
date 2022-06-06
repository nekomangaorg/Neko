package eu.kanade.tachiyomi.ui.manga

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.ChapterHeaderItemBinding
import eu.kanade.tachiyomi.databinding.MangaHeaderItemBinding
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.moveCategories
import eu.kanade.tachiyomi.util.system.getResourceColor
import me.saket.cascade.CascadeDropdownMenu
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsHeader
import org.nekomanga.presentation.theme.NekoTheme
import androidx.compose.material3.DropdownMenuItem as MenuItem

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
            startReadingButton.setOnClickListener { adapter.delegate.readNextChapter(it) }
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
                if (adapter.preferences.themeMangaDetails()) {
                    val accentColor = adapter.delegate.accentColor() ?: return
                    chapterBinding.filterButton.imageTintList = ColorStateList.valueOf(accentColor)
                }
            }
            return
        }

        // composeStuff
        binding.compose.setContent {
            NekoTheme {
                var favoriteExpanded by rememberSaveable { mutableStateOf(false) }

                val trackServiceCount: Int by presenter.trackServiceCountState.collectAsState()

                MangaDetailsHeader(
                    manga = manga,
                    titleLongClick = { title ->
                        adapter.delegate.copyToClipboard(title, R.string.title)
                    },
                    creatorLongClick = { creator ->
                        adapter.delegate.copyToClipboard(creator, R.string.creator)
                    },
                    themeBasedOffCover = adapter.preferences.themeMangaDetails(),
                    trackServiceCount = trackServiceCount,
                    favoriteClick = {
                        if (manga.favorite.not()) {
                            presenter.toggleFavorite()
                        } else {
                            favoriteExpanded = true
                        }
                    },
                    trackingClick = { adapter.delegate.showTrackingSheet() },
                    artworkClick = { },
                    similarClick = { adapter.delegate.openSimilar() },
                    mergeClick = { adapter.delegate.openMerge() },
                    linksClick = { adapter.delegate.showExternalSheet() },
                    shareClick = { adapter.delegate.prepareToShareManga() },
                    genreClick = { adapter.delegate.tagClicked(it) },
                    genreLongClick = { adapter.delegate.tagLongClicked(it) },
                )

                CascadeDropdownMenu(
                    expanded = favoriteExpanded,
                    offset = DpOffset(8.dp, 0.dp),
                    onDismissRequest = { favoriteExpanded = false },
                ) {
                    val style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-.5).sp)
                    MenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.remove_from_library),
                                style = style,
                            )
                        },
                        onClick = {
                            presenter.toggleFavorite()
                            favoriteExpanded = false
                        },
                    )
                    if (presenter.getCategories().isNotEmpty()) {
                        val context = LocalContext.current
                        MenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.edit_categories),
                                    style = style,
                                )
                            },
                            onClick = {
                                presenter.manga.moveCategories(presenter.db, context.getActivity()!!)
                                favoriteExpanded = false
                            },
                        )
                    }
                }
            }
        }

        //  item.isLocked -> MaterialDesignDx.Icon.gmf_lock

        //val tracked = presenter.isTracked() && !item.isLocked

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
                    readTxt,
                )
            } else {
                resources.getString(R.string.all_chapters_read)
            }
        }

        val count = presenter.chapters.size
        binding.chaptersTitle.text =
            itemView.resources.getQuantityString(R.plurals.chapters_plural, count, count)


        manga.genre?.let {
            // binding.r18Badge.isVisible = (it.contains("pornographic", true))
        }

        binding.filtersText.text = presenter.currentFilters()

        if (!manga.initialized) return
        updateCover(manga)
        if (adapter.preferences.themeMangaDetails()) {
            updateColors(false)
        }
    }

    fun updateColors(updateAll: Boolean = true) {
        val accentColor = adapter.delegate.accentColor() ?: return
        if (binding == null) {
            if (chapterBinding != null) {
                chapterBinding.filterButton.imageTintList = ColorStateList.valueOf(accentColor)
            }
            return
        }
        val manga = adapter.presenter.manga
        with(binding) {

            val states = arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(),
            )

            val colors = intArrayOf(
                ColorUtils.setAlphaComponent(
                    root.context.getResourceColor(R.attr.tabBarIconInactive),
                    43,
                ),
                accentColor,
            )

            startReadingButton.backgroundTintList = ColorStateList(states, colors)

            val textColors = intArrayOf(
                ColorUtils.setAlphaComponent(
                    root.context.getResourceColor(R.attr.colorOnSurface),
                    97,
                ),
                root.context.getResourceColor(android.R.attr.textColorPrimaryInverse),
            )
            startReadingButton.setTextColor(ColorStateList(states, textColors))
            if (updateAll) {
                // trackButton.checked(trackButton.stateListAnimator != null)
                // favoriteButton.checked(favoriteButton.stateListAnimator != null)
                //setGenreTags(this, manga)
            }
        }
    }

    fun updateCover(manga: Manga) {
        binding ?: return
        if (!manga.initialized) return
        /* val drawable = adapter.controller.binding.mangaCoverFull.drawable
         binding.mangaCover.loadManga(
             manga,
             builder = {
                 placeholder(drawable)
                 error(drawable)
                 if (manga.favorite) networkCachePolicy(CachePolicy.READ_ONLY)
                 diskCachePolicy(CachePolicy.READ_ONLY)
             },
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
                                 .toDrawable(itemView.resources),
                         )
                         applyBlur()
                     },
                 )
             },
         )*/
    }

    override fun onLongClick(view: View?): Boolean {
        super.onLongClick(view)
        return false
    }
}

@Deprecated("This can be removed once entire view is compose and the e2e sheet is written in compose\n")
private fun Context.getActivity(): AppCompatActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is AppCompatActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
