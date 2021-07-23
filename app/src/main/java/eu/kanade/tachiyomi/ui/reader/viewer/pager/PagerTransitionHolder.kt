package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.bold
import androidx.core.text.inSpans
import com.crazylegend.kotlinextensions.views.setFont
import com.mikepenz.iconics.typeface.library.materialdesigndx.MaterialDesignDx
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.iconicsDrawableMedium
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers

/**
 * View of the ViewPager that contains a chapter transition.
 */
@SuppressLint("ViewConstructor")
class PagerTransitionHolder(
    val viewer: PagerViewer,
    val transition: ChapterTransition,
) : LinearLayout(viewer.activity), ViewPagerAdapter.PositionableView {

    /**
     * Item that identifies this view. Needed by the adapter to not recreate views.
     */
    override val item: Any
        get() = transition

    /**
     * Subscription for status changes of the transition page.
     */
    private var statusSubscription: Subscription? = null

    /**
     * Text view used to display the text of the current and next/prev chapters.
     */
    private var textView = TextView(context).apply {
        // if (Build.VERSION.SDK_INT >= 23)
        // setTextColor(context.getResourceColor(R.attr.))
        textSize = 17.5F
        setFont(R.font.montserrat_regular)
        wrapContent()
    }

    /**
     * View container of the current status of the transition page. Child views will be added
     * dynamically.
     */
    private var pagesContainer = LinearLayout(context).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        orientation = VERTICAL
        gravity = Gravity.CENTER
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        val sidePadding = 64.dpToPx
        setPadding(sidePadding, 0, sidePadding, 0)
        addView(textView)
        addView(pagesContainer)

        when (transition) {
            is ChapterTransition.Prev -> bindPrevChapterTransition()
            is ChapterTransition.Next -> bindNextChapterTransition()
        }
    }

    /**
     * Called when this view is detached from the window. Unsubscribes any active subscription.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        statusSubscription?.unsubscribe()
        statusSubscription = null
    }

    /**
     * Binds a next chapter transition on this view and subscribes to the load status.
     */
    private fun bindNextChapterTransition() {
        val nextChapter = transition.to

        textView.text = if (nextChapter != null) {
            SpannableStringBuilder().bold { append(context.getString(R.string.finished_chapter)) }
                .append("\n${transition.from.chapter.name}\n\n")
                .bold { append(context.getString(R.string.next_)) }
                .append("\n${nextChapter.chapter.name}\n\n")
        } else {
            val d = context.iconicsDrawableMedium(MaterialDesignDx.Icon.gmf_account_tree)
            SpannableStringBuilder().append(context.getString(R.string.theres_no_next_chapter))
                .append("\n\n")
                .append(context.getString(R.string.try_similar))
                .append("   ")
                .inSpans(span = ImageSpan(d, DynamicDrawableSpan.ALIGN_BOTTOM)) {
                    append("  ")
                }
                .append("\n")
                .append(context.getString(R.string.try_similar_after_click))
        }

        if (nextChapter != null) {
            observeStatus(nextChapter)
        }
    }

    /**
     * Binds a previous chapter transition on this view and subscribes to the page load status.
     */
    private fun bindPrevChapterTransition() {
        val prevChapter = transition.to

        textView.text = if (prevChapter != null) {
            SpannableStringBuilder().apply {
                bold { append(context.getString(R.string.current_chapter)) }
                append("\n${transition.from.chapter.name}\n\n")
                val currSize = length
                bold { append(context.getString(R.string.previous_title)) }
                append("\n${prevChapter.chapter.name}\n\n")
            }
        } else {
            context.getString(R.string.theres_no_previous_chapter)
        }

        if (prevChapter != null) {
            observeStatus(prevChapter)
        }
    }

    /**
     * Observes the status of the page list of the next/previous chapter. Whenever there's a new
     * state, the pages container is cleaned up before setting the new state.
     */
    private fun observeStatus(chapter: ReaderChapter) {
        statusSubscription?.unsubscribe()
        statusSubscription = chapter.stateObserver
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { state ->
                pagesContainer.removeAllViews()
                when (state) {
                    is ReaderChapter.State.Wait -> {
                    }
                    is ReaderChapter.State.Loading -> setLoading()
                    is ReaderChapter.State.Error -> setError(state.error)
                    is ReaderChapter.State.Loaded -> setLoaded()
                }
            }
    }

    /**
     * Sets the loading state on the pages container.
     */
    private fun setLoading() {
        val progress = ProgressBar(context, null, android.R.attr.progressBarStyle)

        val textView = AppCompatTextView(context).apply {
            wrapContent()
            setText(R.string.loading_pages)
        }

        pagesContainer.addView(progress)
        pagesContainer.addView(textView)
    }

    /**
     * Sets the loaded state on the pages container.
     */
    private fun setLoaded() {
        // No additional view is added
    }

    /**
     * Sets the error state on the pages container.
     */
    private fun setError(error: Throwable) {
        val textView = AppCompatTextView(context).apply {
            wrapContent()
            text = context.getString(R.string.failed_to_load_pages_, error.message)
        }

        val retryBtn = PagerButton(context, viewer).apply {
            wrapContent()
            setText(R.string.retry)
            setOnClickListener {
                val toChapter = transition.to
                if (toChapter != null) {
                    viewer.activity.requestPreloadChapter(toChapter)
                }
            }
        }

        pagesContainer.addView(textView)
        pagesContainer.addView(retryBtn)
    }

    /**
     * Extension method to set layout params to wrap content on this view.
     */
    private fun View.wrapContent() {
        layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }
}
