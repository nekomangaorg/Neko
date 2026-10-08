package eu.kanade.tachiyomi.ui.reader.model

import org.nekomanga.presentation.screens.reader.viewer.ChapterTransitionUiModel
import org.nekomanga.presentation.screens.reader.viewer.toPreloadState
import org.nekomanga.presentation.screens.reader.viewer.withPreloadState

/**
 * Sealed hierarchy representing a renderable item in Compose reader viewers (pager or webtoon).
 * Replaces untyped List<Any> and Pair<*, *> runtime casting with compile-time type safety.
 */
sealed interface ReaderUiItem {

    val chapterId: Long?
    val pageIndex: Int?

    fun key(prefix: String): String

    /** A single page or paired double-page spread. */
    data class Page(
        val page: ReaderPage,
        val extraPage: ReaderPage? = null,
    ) : ReaderUiItem {
        override val chapterId: Long?
            get() = page.chapter.chapter.id

        override val pageIndex: Int
            get() = page.index

        override fun key(prefix: String): String {
            val firstHalfSuffix = page.firstHalf?.let { "_half_$it" } ?: ""
            return if (extraPage != null) {
                val extraHalfSuffix = extraPage.firstHalf?.let { "_half_$it" } ?: ""
                "${prefix}_page_${page.chapter.chapter.id}_${page.index}${firstHalfSuffix}_${extraPage.chapter.chapter.id}_${extraPage.index}${extraHalfSuffix}"
            } else {
                "${prefix}_page_${page.chapter.chapter.id}_${page.index}${firstHalfSuffix}"
            }
        }
    }

    /** A split page slice in webtoon mode. */
    data class SplitPage(val split: ReaderPageSplit) : ReaderUiItem {
        val page: ReaderPage
            get() = split.page

        override val chapterId: Long?
            get() = page.chapter.chapter.id

        override val pageIndex: Int
            get() = page.index

        override fun key(prefix: String): String {
            return "${prefix}_split_${page.chapter.chapter.id}_${page.index}_${split.topOffset}"
        }
    }

    /** A transition page between adjacent chapters. */
    data class Transition(
        val transition: ChapterTransition,
        val transitionUiModel: ChapterTransitionUiModel? = null,
    ) : ReaderUiItem {
        override val chapterId: Long?
            get() = null

        override val pageIndex: Int?
            get() = null

        override fun key(prefix: String): String {
            val to = transition.to
            return if (to != null) {
                val fromKey =
                    transition.from.chapter.id?.takeIf { it > 0 }?.toString()
                        ?: transition.from.chapter.url
                val toKey = to.chapter.id?.takeIf { it > 0 }?.toString() ?: to.chapter.url
                val (first, second) = if (fromKey <= toKey) fromKey to toKey else toKey to fromKey
                "${prefix}_transition_${first}_${second}"
            } else {
                val type = if (transition is ChapterTransition.Prev) "prev" else "next"
                val fromKey =
                    transition.from.chapter.id?.takeIf { it > 0 }?.toString()
                        ?: transition.from.chapter.url
                "${prefix}_transition_${type}_${fromKey}"
            }
        }
    }
}

/**
 * Compares two reader UI items for semantic identity equivalence across list updates and preloads.
 * Enables deterministic list re-anchoring and pure JVM test assertions without Android View models.
 */
fun ReaderUiItem.isEquivalentTo(target: ReaderUiItem?): Boolean {
    if (target == null) return false
    return when {
        this is ReaderUiItem.Page && target is ReaderUiItem.Page -> {
            val sameChapter = isSameChapter(this.page.chapter, target.page.chapter)
            if (!sameChapter) return false

            val thisPages = listOfNotNull(this.page, this.extraPage)
            val targetPages = listOfNotNull(target.page, target.extraPage)

            thisPages.any { tp ->
                targetPages.any { op ->
                    tp.index == op.index &&
                        tp.firstHalf == op.firstHalf &&
                        isSameChapter(tp.chapter, op.chapter)
                }
            }
        }
        this is ReaderUiItem.SplitPage && target is ReaderUiItem.SplitPage -> {
            isSameChapter(this.page.chapter, target.page.chapter) &&
                this.page.index == target.page.index &&
                this.split.topOffset == target.split.topOffset
        }
        this is ReaderUiItem.Page && target is ReaderUiItem.SplitPage -> {
            isSameChapter(this.page.chapter, target.page.chapter) &&
                this.page.index == target.page.index &&
                target.split.topOffset == 0
        }
        this is ReaderUiItem.SplitPage && target is ReaderUiItem.Page -> {
            isSameChapter(this.page.chapter, target.page.chapter) &&
                this.page.index == target.page.index &&
                this.split.topOffset == 0
        }
        this is ReaderUiItem.Transition && target is ReaderUiItem.Transition -> {
            areTransitionsEquivalent(this.transition, target.transition)
        }
        else -> false
    }
}

/**
 * True when [next] is the slice that starts where this slice ends, in the same tall page. The
 * webtoon viewer puts no page gap between them, so the page reads as one image.
 */
fun ReaderUiItem.continuesInto(next: ReaderUiItem?): Boolean =
    this is ReaderUiItem.SplitPage &&
        next is ReaderUiItem.SplitPage &&
        isSameChapter(this.page.chapter, next.page.chapter) &&
        this.page.index == next.page.index &&
        this.split.topOffset + this.split.splitHeight == next.split.topOffset

/**
 * These items with each transition card showing its target chapter's current state. The card model
 * is resolved when the items are built, and a failed preload does not rebuild them, so the reader
 * runs this when a target's state changes. Returns this list when every card is already current,
 * and keeps each item's [ReaderUiItem.Transition.transition] so the viewers' keys hold.
 */
fun List<ReaderUiItem>.withLivePreloadStates(): List<ReaderUiItem> {
    var changed = false
    val updated = map { item ->
        val model = (item as? ReaderUiItem.Transition)?.transitionUiModel ?: return@map item
        val target = item.transition.to ?: return@map item
        val live = model.withPreloadState(target.state.toPreloadState())
        if (live === model) {
            item
        } else {
            changed = true
            item.copy(transitionUiModel = live)
        }
    }
    return if (changed) updated else this
}

/**
 * This item without its transition card model, for viewers that react to which item is shown but
 * not to a card state change.
 */
fun ReaderUiItem.withoutCardModel(): ReaderUiItem =
    if (this is ReaderUiItem.Transition) copy(transitionUiModel = null) else this

internal fun isSameChapter(a: ReaderChapter, b: ReaderChapter): Boolean {
    val aId = a.chapter.id
    val bId = b.chapter.id
    return if (aId != null && bId != null && aId > 0 && bId > 0) {
        aId == bId
    } else {
        a.chapter.url == b.chapter.url
    }
}

internal fun areTransitionsEquivalent(a: ChapterTransition, b: ChapterTransition): Boolean {
    if (a::class == b::class && isSameChapter(a.from, b.from)) {
        return true
    }
    val aTo = a.to
    val bTo = b.to
    if (aTo != null && bTo != null) {
        if (isSameChapter(a.from, bTo) && isSameChapter(aTo, b.from)) {
            return true
        }
    }
    return false
}
