package eu.kanade.tachiyomi.ui.reader.model

/**
 * Sealed hierarchy representing a renderable item in Compose reader viewers (pager or webtoon).
 * Replaces untyped List<Any> and Pair<*, *> runtime casting with compile-time type safety.
 */
sealed interface ReaderUiItem {

    fun key(prefix: String): String

    /** A single page or paired double-page spread. */
    data class Page(
        val page: ReaderPage,
        val extraPage: ReaderPage? = null,
    ) : ReaderUiItem {
        override fun key(prefix: String): String {
            val firstHalfSuffix = page.firstHalf?.let { "_$it" } ?: ""
            return if (extraPage != null) {
                val extraHalfSuffix = extraPage.firstHalf?.let { "_$it" } ?: ""
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

        override fun key(prefix: String): String {
            return "${prefix}_split_${page.chapter.chapter.id}_${page.index}_${split.topOffset}"
        }
    }

    /** A transition page between adjacent chapters. */
    data class Transition(val transition: ChapterTransition) : ReaderUiItem {
        override fun key(prefix: String): String {
            val type = if (transition is ChapterTransition.Prev) "prev" else "next"
            val fromId = transition.from.chapter.id
            val toId = transition.to?.chapter?.id
            return "${prefix}_transition_${type}_${fromId}_${toId}"
        }
    }

    companion object {
        /**
         * Converts a legacy PagerViewerAdapter joined item (Pair<Any, Any?> or Any) to
         * [ReaderUiItem].
         */
        fun fromPagerItem(item: Any): ReaderUiItem? {
            return when (item) {
                is Pair<*, *> -> {
                    val first = item.first
                    val second = item.second
                    when (first) {
                        is ReaderPage -> Page(first, second as? ReaderPage)
                        is ChapterTransition -> Transition(first)
                        else -> null
                    }
                }
                is ReaderPage -> Page(item)
                is ChapterTransition -> Transition(item)
                is ReaderUiItem -> item
                else -> null
            }
        }

        /** Converts a legacy WebtoonAdapter item to [ReaderUiItem]. */
        fun fromWebtoonItem(item: Any): ReaderUiItem? {
            return when (item) {
                is ReaderPage -> Page(item)
                is ReaderPageSplit -> SplitPage(item)
                is ChapterTransition -> Transition(item)
                is ReaderUiItem -> item
                else -> null
            }
        }
    }
}
