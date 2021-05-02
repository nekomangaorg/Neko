package eu.kanade.tachiyomi.data.database.models

import android.content.Context
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.source.model.MangaInfo
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

    fun isBlank() = id == Long.MIN_VALUE
    fun isHidden() = status == -1

    fun setChapterOrder(order: Int) {
        setFlags(order, SORT_MASK)
        setFlags(SORT_LOCAL, SORT_SELF_MASK)
    }

    fun setSortToGlobal() = setFlags(SORT_GLOBAL, SORT_SELF_MASK)

    private fun setFlags(flag: Int, mask: Int) {
        chapter_flags = chapter_flags and mask.inv() or (flag and mask)
    }

    fun sortDescending(): Boolean = chapter_flags and SORT_MASK == SORT_DESC

    fun usesLocalSort(): Boolean = chapter_flags and SORT_SELF_MASK == SORT_LOCAL

    fun sortDescending(defaultDesc: Boolean): Boolean {
        return if (chapter_flags and SORT_SELF_MASK == SORT_GLOBAL) defaultDesc
        else sortDescending()
    }

    fun showChapterTitle(defaultShow: Boolean): Boolean = chapter_flags and DISPLAY_MASK == DISPLAY_NUMBER

    fun seriesType(context: Context, sourceManager: SourceManager? = null): String {
        return context.getString(
            when (seriesType(sourceManager = sourceManager)) {
                TYPE_WEBTOON -> R.string.webtoon
                TYPE_MANHWA -> R.string.manhwa
                TYPE_MANHUA -> R.string.manhua
                TYPE_COMIC -> R.string.comic
                else -> R.string.manga
            }
        ).toLowerCase(Locale.getDefault())
    }

    fun getGenres(): List<String>? {
        return genre?.split(",")
            ?.mapNotNull { tag -> tag.trim().takeUnless { it.isBlank() } }
    }

    fun getOriginalGenres(): List<String>? {
        return (originalGenre ?: genre)?.split(",")
            ?.mapNotNull { tag -> tag.trim().takeUnless { it.isBlank() } }
    }

    /**
     * The type of comic the manga is (ie. manga, manhwa, manhua)
     */
    fun seriesType(useOriginalTags: Boolean = false, customTags: String? = null, sourceManager: SourceManager? = null): Int {
        val sourceName by lazy { (sourceManager ?: Injekt.get()).getOrStub(source).name }
        val tags = customTags ?: if (useOriginalTags) originalGenre else genre
        val currentTags = tags?.split(",")?.map { it.trim().toLowerCase(Locale.US) } ?: emptyList()
        return if (currentTags.any { tag -> isMangaTag(tag) }) {
            TYPE_MANGA
        } else if (currentTags.any { tag -> isComicTag(tag) } ||
            isComicSource(sourceName)
        ) {
            TYPE_COMIC
        } else if (currentTags.any { tag -> isWebtoonTag(tag) } ||
            (
                sourceName.contains("webtoon", true) &&
                    currentTags.none { tag -> isManhuaTag(tag) } &&
                    currentTags.none { tag -> isManhwaTag(tag) }
                )
        ) {
            TYPE_WEBTOON
        } else if (currentTags.any { tag -> isManhuaTag(tag) } || sourceName.contains(
                "manhua",
                true
            )
        ) {
            TYPE_MANHUA
        } else if (currentTags.any { tag -> isManhwaTag(tag) } || isWebtoonSource(sourceName)) {
            TYPE_MANHWA
        } else {
            TYPE_MANGA
        }
    }

    /**
     * The type the reader should use. Different from manga type as certain manga has different
     * read types
     */
    fun defaultReaderType(): Int {
        val sourceName = Injekt.get<SourceManager>().getOrStub(source).name
        val currentTags = genre?.split(",")?.map { it.trim().toLowerCase(Locale.US) } ?: emptyList()
        return if (currentTags.any
            { tag ->
                isManhwaTag(tag) || tag.contains("webtoon")
            } || (
                isWebtoonSource(sourceName) &&
                    currentTags.none { tag -> isManhuaTag(tag) } &&
                    currentTags.none { tag -> isComicTag(tag) }
                )
        ) {
            ReaderActivity.WEBTOON
        } else if (currentTags.any
            { tag ->
                tag == "chinese" || tag == "manhua" ||
                    tag.startsWith("english") || tag == "comic"
            } || (
                isComicSource(sourceName) && !sourceName.contains("tapas", true) &&
                    currentTags.none { tag -> isMangaTag(tag) }
                ) ||
            (sourceName.contains("manhua", true) && currentTags.none { tag -> isMangaTag(tag) })
        ) {
            ReaderActivity.LEFT_TO_RIGHT
        } else 0
    }

    fun isSeriesTag(tag: String): Boolean {
        val tagLower = tag.toLowerCase(Locale.ROOT)
        return isMangaTag(tagLower) || isManhuaTag(tagLower) ||
            isManhwaTag(tagLower) || isComicTag(tagLower) || isWebtoonTag(tagLower)
    }

    fun isMangaTag(tag: String): Boolean {
        return tag in listOf("manga", "манга", "jp") || tag.startsWith("japanese")
    }

    fun isManhuaTag(tag: String): Boolean {
        return tag in listOf("manhua", "маньхуа", "cn", "hk", "zh-Hans", "zh-Hant") || tag.startsWith("chinese")
    }

    fun isLongStrip(): Boolean {
        val currentTags =
            genre?.split(",")?.map { it.trim().toLowerCase(Locale.US) } ?: emptyList()
        val sourceName by lazy { Injekt.get<SourceManager>().getOrStub(source).name }
        return currentTags.any { it == "long strip" } || sourceName.contains("webtoon", true)
    }

    fun isManhwaTag(tag: String): Boolean {
        return tag in listOf("long strip", "manhwa", "манхва", "kr") || tag.startsWith("korean")
    }

    fun isComicTag(tag: String): Boolean {
        return tag in listOf("comic", "комикс", "en", "gb") || tag.startsWith("english")
    }

    fun isWebtoonTag(tag: String): Boolean {
        return tag.startsWith("webtoon")
    }

    fun isWebtoonSource(sourceName: String): Boolean {
        return sourceName.contains("webtoon", true) ||
            sourceName.contains("manhwa", true) ||
            sourceName.contains("toonily", true)
    }

    fun isComicSource(sourceName: String): Boolean {
        return sourceName.contains("gunnerkrigg", true) ||
            sourceName.contains("dilbert", true) ||
            sourceName.contains("cyanide", true) ||
            sourceName.contains("xkcd", true) ||
            sourceName.contains("tapas", true) ||
            sourceName.contains("ComicExtra", true)
    }

    fun key(): String {
        return DiskUtil.hashKeyForDisk(thumbnail_url.orEmpty())
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

        const val SORT_GLOBAL = 0x00000000
        const val SORT_LOCAL = 0x00001000
        const val SORT_SELF_MASK = 0x00001000

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

        const val TYPE_MANGA = 1
        const val TYPE_MANHWA = 2
        const val TYPE_MANHUA = 3
        const val TYPE_COMIC = 4
        const val TYPE_WEBTOON = 5

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

fun Manga.toMangaInfo(): MangaInfo {
    return MangaInfo(
        artist = this.artist ?: "",
        author = this.author ?: "",
        cover = this.thumbnail_url ?: "",
        description = this.description ?: "",
        genres = this.getGenres() ?: emptyList(),
        key = this.url,
        status = this.status,
        title = this.title
    )
}
