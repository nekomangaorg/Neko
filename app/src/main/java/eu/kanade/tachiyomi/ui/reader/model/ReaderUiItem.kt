package eu.kanade.tachiyomi.ui.reader.model

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
    data class Transition(val transition: ChapterTransition) : ReaderUiItem {
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
