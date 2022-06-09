package eu.kanade.tachiyomi.ui.manga

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
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
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.MangaHeaderItemBinding
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.moveCategories
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

    fun bind(item: MangaHeaderItem, manga: Manga) {
        val presenter = adapter.delegate.mangaPresenter()
        // composeStuff
        binding?.compose?.setContent {
            NekoTheme {
                var favoriteExpanded by rememberSaveable { mutableStateOf(false) }

                val trackServiceCount: Int by presenter.trackServiceCountState.collectAsState()

                val quickReadText = getQuickReadText(presenter, LocalContext.current)

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
                    quickReadText = quickReadText,
                    quickReadClick = { adapter.delegate.readNextChapter() },
                    numberOfChapters = presenter.chapters.size,
                    chapterFilterText = presenter.currentFilters(),
                    chapterHeaderClick = { adapter.delegate.showChapterFilter() },
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

        if (!manga.initialized) return
        updateCover(manga)
        if (adapter.preferences.themeMangaDetails()) {
            updateColors(false)
        }
    }

    fun updateColors(updateAll: Boolean = true) {
        val accentColor = adapter.delegate.accentColor() ?: return
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

private fun getQuickReadText(
    presenter: MangaDetailsPresenter,
    context: Context,
): String {
    val nextChapter = presenter.getNextUnreadChapter()

    return if (nextChapter != null) {
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
        context.getString(
            if (nextChapter.last_page_read > 0) R.string.continue_reading_
            else R.string.start_reading_,
            readTxt,
        )
    } else {
        ""
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
