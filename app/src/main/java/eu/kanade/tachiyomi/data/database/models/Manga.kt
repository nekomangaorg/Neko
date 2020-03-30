package eu.kanade.tachiyomi.data.database.models

import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

interface Manga : SManga {

    var id: Long?

    var source: Long

    var favorite: Boolean

    var last_update: Long

    var date_added: Long

    var viewer: Int

    var chapter_flags: Int

    var hide_title: Boolean

    fun setChapterOrder(order: Int) {
        setFlags(order, SORT_MASK)
    }

    private fun setFlags(flag: Int, mask: Int) {
        chapter_flags = chapter_flags and mask.inv() or (flag and mask)
    }

    fun sortDescending(): Boolean {
        return chapter_flags and SORT_MASK == SORT_DESC
    }

    fun mangaType(): Int {
        val sourceName = Injekt.get<SourceManager>().getOrStub(source).name
        val currentTags = genre?.split(",")?.map { it.trim().toLowerCase(Locale.US) }
        return if (currentTags?.any
            { tag ->
                tag.startsWith("japanese") || tag == "manga"
            } == true)
            TYPE_MANGA
        else if (currentTags?.any
            { tag ->
                tag.startsWith("english") || tag == "comic"
            } == true || isComicSource(sourceName))
            TYPE_COMIC
        else if (currentTags?.any
            { tag ->
                tag.startsWith("chinese") || tag == "manhua"
            } == true ||
            sourceName.contains("manhua", true))
            TYPE_MANHUA
        else if (currentTags?.any
            { tag ->
                tag == "long strip" || tag == "manhwa"
            } == true || isWebtoonSource(sourceName))
            TYPE_MANHWA
        else if (currentTags?.any
            { tag ->
                tag.startsWith("webtoon")
            } == true)
            TYPE_WEBTOON
        else TYPE_MANGA
    }

    fun defaultReaderType(): Int {
        val sourceName = Injekt.get<SourceManager>().getOrStub(source).name
        val currentTags = genre?.split(",")?.map { it.trim().toLowerCase(Locale.US) }
        return if (currentTags?.any
            { tag ->
                tag == "long strip" || tag == "manhwa" ||
                    tag.contains("webtoon")
            } == true || isWebtoonSource(sourceName) ||
            sourceName.contains("tapastic", true))
            ReaderActivity.WEBTOON
        else if (currentTags?.any
            { tag ->
                tag.startsWith("chinese") || tag == "manhua" ||
                    tag.startsWith("english") || tag == "comic"
            } == true || isComicSource(sourceName) ||
            sourceName.contains("manhua", true))
            ReaderActivity.LEFT_TO_RIGHT
        else 0
    }

    fun isWebtoonSource(sourceName: String): Boolean {
        return sourceName.contains("webtoon", true) ||
            sourceName.contains("manwha", true) ||
            sourceName.contains("toonily", true)
    }

    fun isComicSource(sourceName: String): Boolean {
        return sourceName.contains("gunnerkrigg", true) ||
            sourceName.contains("gunnerkrigg", true) ||
            sourceName.contains("dilbert", true) ||
            sourceName.contains("cyanide", true) ||
            sourceName.contains("xkcd", true) ||
            sourceName.contains("tapastic", true)
    }

    // Used to display the chapter's title one way or another
    var displayMode: Int
        get() = chapter_flags and DISPLAY_MASK
        set(mode) = setFlags(mode, DISPLAY_MASK)

    var readFilter: Int
        get() = chapter_flags and READ_MASK
        set(filter) = setFlags(filter, READ_MASK)

    var downloadedFilter: Int
        get() = chapter_flags and DOWNLOADED_MASK
        set(filter) = setFlags(filter, DOWNLOADED_MASK)

    var bookmarkedFilter: Int
        get() = chapter_flags and BOOKMARKED_MASK
        set(filter) = setFlags(filter, BOOKMARKED_MASK)

    var sorting: Int
        get() = chapter_flags and SORTING_MASK
        set(sort) = setFlags(sort, SORTING_MASK)

    companion object {

        const val SORT_DESC = 0x00000000
        const val SORT_ASC = 0x00000001
        const val SORT_MASK = 0x00000001

        // Generic filter that does not filter anything
        const val SHOW_ALL = 0x00000000

        const val SHOW_UNREAD = 0x00000002
        const val SHOW_READ = 0x00000004
        const val READ_MASK = 0x00000006

        const val SHOW_DOWNLOADED = 0x00000008
        const val SHOW_NOT_DOWNLOADED = 0x00000010
        const val DOWNLOADED_MASK = 0x00000018

        const val SHOW_BOOKMARKED = 0x00000020
        const val SHOW_NOT_BOOKMARKED = 0x00000040
        const val BOOKMARKED_MASK = 0x00000060

        const val SORTING_SOURCE = 0x00000000
        const val SORTING_NUMBER = 0x00000100
        const val SORTING_MASK = 0x00000100

        const val DISPLAY_NAME = 0x00000000
        const val DISPLAY_NUMBER = 0x00100000
        const val DISPLAY_MASK = 0x00100000

        const val TYPE_MANGA = 0
        const val TYPE_MANHWA = 1
        const val TYPE_MANHUA = 2
        const val TYPE_COMIC = 3
        const val TYPE_WEBTOON = 4

        fun create(source: Long): Manga = MangaImpl().apply {
            this.source = source
        }

        fun create(pathUrl: String, title: String, source: Long = 0): Manga = MangaImpl().apply {
            url = pathUrl
            this.title = title
            this.source = source
        }
    }
}
