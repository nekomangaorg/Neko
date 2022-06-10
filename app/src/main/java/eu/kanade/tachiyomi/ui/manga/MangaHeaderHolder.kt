package eu.kanade.tachiyomi.ui.manga

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.MangaHeaderItemBinding
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import org.nekomanga.presentation.screens.mangadetails.MangaDetailsHeader
import org.nekomanga.presentation.theme.NekoTheme

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
                    toggleFavorite = {},
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
