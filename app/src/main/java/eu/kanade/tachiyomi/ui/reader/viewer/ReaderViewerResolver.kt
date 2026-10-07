package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.pager.L2RPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.R2LPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.pager.VerticalPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer

/** Decides whether the reader can keep its current viewer for a reading mode. */
object ReaderViewerResolver {

    /**
     * Returns [current] when it already shows [readingMode] with the same [noWebtoonTag], otherwise
     * the viewer built by [create].
     *
     * The check runs before [create] because building a viewer has side effects. Its init points
     * the preload engine's onPageSplit at itself and registers preference listeners on its own
     * scope. A viewer built only to be compared and then dropped keeps those, so it would take page
     * splits away from the live viewer and reload the reader on every later preference change.
     */
    fun resolve(
        current: BaseViewer?,
        readingMode: Int,
        noWebtoonTag: Boolean,
        create: (readingMode: Int, noWebtoonTag: Boolean) -> BaseViewer,
    ): BaseViewer {
        val isSameViewer =
            when (readingMode) {
                ReadingModeType.LEFT_TO_RIGHT.flagValue -> current is L2RPagerViewer
                ReadingModeType.VERTICAL.flagValue -> current is VerticalPagerViewer
                ReadingModeType.WEBTOON.flagValue ->
                    current is WebtoonViewer && current.noWebtoonTag == noWebtoonTag
                else -> current is R2LPagerViewer
            }
        return current?.takeIf { isSameViewer } ?: create(readingMode, noWebtoonTag)
    }
}
