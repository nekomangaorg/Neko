package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.scanlatorList
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.online.utils.MdUtil

/**
 * This class filters chapters for the reader based on the user enabled preferences and filters
 */
class ReaderChapterFilter(
    private val downloadManager: DownloadManager,
    private val preferences: PreferencesHelper
) {

    fun filterChapter(
        dbChapters: List<Chapter>,
        manga: Manga,
        selectedChapter: Chapter? = null
    ): List<Chapter> {

        // if neither preference is enabled don't even filter
        if (!preferences.skipRead() && !preferences.skipFiltered()) {
            return dbChapters
        }

        var filteredChapters = dbChapters
        if (preferences.skipRead()) {
            filteredChapters = filteredChapters.filter { !it.read }
        }
        if (preferences.skipFiltered()) {
            val readEnabled = manga.readFilter == Manga.SHOW_READ
            val unreadEnabled = manga.readFilter == Manga.SHOW_UNREAD
            val downloadEnabled = manga.downloadedFilter == Manga.SHOW_DOWNLOADED
            val bookmarkEnabled = manga.bookmarkedFilter == Manga.SHOW_BOOKMARKED
            val listValidScanlators = MdUtil.getScanlators(manga.scanlator_filter.orEmpty())
            val scanlatorEnabled = listValidScanlators.isNotEmpty()

            // if none of the filters are enabled skip the filtering of them
            if (readEnabled || unreadEnabled || downloadEnabled || bookmarkEnabled) {

                filteredChapters = filteredChapters.filter {
                    if (readEnabled && it.read.not() ||
                        (unreadEnabled && it.read) ||
                        (bookmarkEnabled && it.bookmark.not()) ||
                        (downloadEnabled && downloadManager.isChapterDownloaded(it, manga).not()) ||
                        (scanlatorEnabled && it.scanlatorList().none { group -> listValidScanlators.contains(group) })
                    ) {
                        return@filter false
                    }
                    return@filter true
                }
            }
        }

        // add the selected chapter to the list in case it was filtered out
        if (selectedChapter?.id != null) {
            val find = filteredChapters.find { it.id == selectedChapter.id }
            if (find == null) {
                val mutableList = filteredChapters.toMutableList()
                mutableList.add(selectedChapter)
                filteredChapters = mutableList.toList()
            }
        }
        return filteredChapters
    }
}
