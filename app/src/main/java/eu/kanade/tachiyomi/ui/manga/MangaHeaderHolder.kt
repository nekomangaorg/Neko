package eu.kanade.tachiyomi.ui.manga

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.button.MaterialButton
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.glide.GlideApp
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.holder.BaseFlexibleViewHolder
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.DrawableHelper
import eu.kanade.tachiyomi.util.view.gone
import eu.kanade.tachiyomi.util.view.invisible
import eu.kanade.tachiyomi.util.view.resetStrokeColor
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import eu.kanade.tachiyomi.util.view.visible
import eu.kanade.tachiyomi.util.view.visibleIf
import kotlinx.android.synthetic.main.manga_details_controller.*
import kotlinx.android.synthetic.main.manga_header_item.*

class MangaHeaderHolder(
    private val view: View,
    private val adapter: MangaDetailsAdapter,
    startExpanded: Boolean,
    isTablet: Boolean = false
) : BaseFlexibleViewHolder(view, adapter) {

    init {
        chapter_layout.setOnClickListener { adapter.delegate.showChapterFilter() }
        if (start_reading_button != null) {
            start_reading_button.setOnClickListener { adapter.delegate.readNextChapter() }
            top_view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = adapter.delegate.topCoverHeight()
            }
            more_button.setOnClickListener { expandDesc() }
            manga_summary.setOnClickListener { expandDesc() }
            manga_summary.setOnLongClickListener {
                if (manga_summary.isTextSelectable && !adapter.recyclerView.canScrollVertically(-1)) {
                    (adapter.delegate as MangaDetailsController).swipe_refresh.isEnabled = false
                }
                false
            }
            manga_summary.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_UP) (adapter.delegate as MangaDetailsController).swipe_refresh.isEnabled =
                    true
                false
            }
            less_button.setOnClickListener { collapseDesc() }
            manga_genres_tags.setOnTagClickListener {
                adapter.delegate.tagClicked(it)
            }
            chapter_layout.setOnClickListener { adapter.delegate.showChapterFilter() }
            webview_button.setOnClickListener { adapter.delegate.showExternalSheet() }
            share_button.setOnClickListener { adapter.delegate.prepareToShareManga() }
            favorite_button.setOnClickListener {
                adapter.delegate.favoriteManga(false)
            }
            favorite_button.setOnLongClickListener {
                adapter.delegate.favoriteManga(true)
                true
            }
            manga_full_title.setOnLongClickListener {
                adapter.delegate.copyToClipboard(manga_full_title.text.toString(), R.string.title)
                true
            }
            manga_author.setOnLongClickListener {
                adapter.delegate.copyToClipboard(manga_author.text.toString(), R.string.author)
                true
            }
            manga_cover.setOnClickListener { adapter.delegate.zoomImageFromThumb(cover_card) }
            track_button.setOnClickListener { adapter.delegate.showTrackingSheet() }
            webview_button.setOnClickListener { adapter.delegate.showExternalSheet() }

            if (startExpanded) expandDesc()
            else collapseDesc()
            if (isTablet) chapter_layout.gone()
        }
    }

    private fun expandDesc() {
        if (more_button.visibility == View.VISIBLE) {
            manga_summary.maxLines = Integer.MAX_VALUE
            manga_summary.setTextIsSelectable(true)
            manga_genres_tags.visible()
            less_button.visible()
            more_button_group.gone()
        }
    }

    private fun collapseDesc() {
        manga_summary.setTextIsSelectable(false)
        manga_summary.maxLines = 3
        manga_summary.setOnClickListener { expandDesc() }
        manga_genres_tags.gone()
        less_button.gone()
        more_button_group.visible()
    }

    fun bindChapters() {
        val presenter = adapter.delegate.mangaPresenter()
        val count = presenter.chapters.size
        chapters_title.text = itemView.resources.getQuantityString(R.plurals.chapters, count, count)
        filters_text.text = presenter.currentFilters()
    }

    @SuppressLint("SetTextI18n")
    fun bind(item: MangaHeaderItem, manga: Manga) {
        val presenter = adapter.delegate.mangaPresenter()
        manga_full_title.text = manga.title

        if (manga.genre.isNullOrBlank().not()) manga_genres_tags.setTags(
            manga.genre?.split(", ")?.map(String::trim)
        )
        else manga_genres_tags.setTags(emptyList())

        if (manga.author == manga.artist || manga.artist.isNullOrBlank()) manga_author.text =
            manga.author?.trim()
        else {
            manga_author.text = "${manga.author?.trim()}, ${manga.artist}"
        }
        manga_summary.text =
            if (manga.description.isNullOrBlank()) itemView.context.getString(R.string.no_description)
            else manga.description?.trim()

        if (item.isLocked) sub_item_group.referencedIds =
            intArrayOf(R.id.manga_summary, R.id.manga_summary_label, R.id.button_layout)
        else sub_item_group.referencedIds = intArrayOf(
            R.id.start_reading_button,
            R.id.manga_summary,
            R.id.manga_summary_label,
            R.id.button_layout
        )

        manga_summary.post {
            if (sub_item_group.visibility != View.GONE) {
                if ((manga_summary.lineCount < 3 && manga.genre.isNullOrBlank()) || less_button.visibility == View.VISIBLE) {
                    more_button_group.gone()
                } else more_button_group.visible()
            }
            if (adapter.hasFilter()) collapse()
            else expand()
        }
        manga_summary_label.text = itemView.context.getString(
            R.string.about_this_, manga.mangaType(itemView.context)
        )
        with(favorite_button) {
            setImageDrawable(
                DrawableHelper.standardIcon24(
                    context, when {
                        item.isLocked -> MaterialDesignDx.Icon.gmf_lock
                        item.manga.favorite -> CommunityMaterial.Icon2.cmd_heart as IIcon
                        else -> CommunityMaterial.Icon2.cmd_heart_outline as IIcon
                    }
                )
            )
        }
        true_backdrop.setBackgroundColor(
            adapter.delegate.coverColor()
                ?: itemView.context.getResourceColor(android.R.attr.colorBackground)
        )

        val tracked = presenter.isTracked() && !item.isLocked

        with(track_button) {
            setImageDrawable(
                DrawableHelper.standardIcon32(
                    itemView.context,
                    MaterialDesignDx.Icon.gmf_art_track
                )
            )
        }

        with(similar_button) {
            visibleIf(presenter.similarEnabled())
            setImageDrawable(
                DrawableHelper.standardIcon24(
                    itemView.context,
                    MaterialDesignDx.Icon.gmf_account_tree
                )
            )
        }

        with(webview_button) {
            setImageDrawable(
                DrawableHelper.standardIcon24(
                    itemView.context,
                    CommunityMaterial.Icon2.cmd_web
                )
            )
        }
        with(share_button) {
            setImageDrawable(
                DrawableHelper.standardIcon24(
                    itemView.context,
                    MaterialDesignDx.Icon.gmf_share
                )
            )
        }

        with(start_reading_button) {
            val nextChapter = presenter.getNextUnreadChapter()
            visibleIf(presenter.chapters.isNotEmpty() && !item.isLocked)
            isEnabled = (nextChapter != null)
            text = if (nextChapter != null) {
                val number = adapter.decimalFormat.format(nextChapter.chapter_number.toDouble())
                if (nextChapter.chapter_number > 0) resources.getString(
                    if (nextChapter.last_page_read > 0) R.string.continue_reading_chapter_
                    else R.string.start_reading_chapter_, number
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
        chapters_title.text = itemView.resources.getQuantityString(R.plurals.chapters, count, count)

        top_view.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = adapter.delegate.topCoverHeight()
        }

        manga_status.text = (itemView.context.getString(
            when (manga.status) {
                SManga.ONGOING -> R.string.ongoing
                SManga.COMPLETED -> R.string.completed
                SManga.LICENSED -> R.string.licensed
                SManga.PUBLICATION_COMPLETE -> R.string.publication_complete
                SManga.HIATUS -> R.string.hiatus
                SManga.CANCELLED -> R.string.cancelled
                else -> R.string.unknown
            }
        ))
        manga_source.text = presenter.source.toString()

        filters_text.text = presenter.currentFilters()

        if (!manga.initialized) return
        GlideApp.with(view.context).load(manga).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(manga.id!!).toString()))
            .into(manga_cover)
        GlideApp.with(view.context).load(manga).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(MangaImpl.getLastCoverFetch(manga.id!!).toString())).centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade()).into(backdrop)
    }

    private fun MaterialButton.checked(checked: Boolean) {
        if (checked) {
            backgroundTintList = ColorStateList.valueOf(
                ColorUtils.setAlphaComponent(
                    context.getResourceColor(R.attr.colorAccent), 75
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
        top_view.updateLayoutParams<ConstraintLayout.LayoutParams> {
            height = newHeight
        }
    }

    fun setBackDrop(color: Int) {
        true_backdrop.setBackgroundColor(color)
    }

    fun collapse() {
        sub_item_group.gone()
        if (more_button.visibility == View.VISIBLE || more_button.visibility == View.INVISIBLE)
            more_button_group.invisible()
        else {
            less_button.gone()
            manga_genres_tags.gone()
        }
    }

    fun expand() {
        sub_item_group.visible()
        if (more_button.visibility == View.VISIBLE || more_button.visibility == View.INVISIBLE) more_button_group.visible()
        else {
            less_button.visible()
            manga_genres_tags.visible()
        }
    }

    override fun onLongClick(view: View?): Boolean {
        super.onLongClick(view)
        return false
    }
}
