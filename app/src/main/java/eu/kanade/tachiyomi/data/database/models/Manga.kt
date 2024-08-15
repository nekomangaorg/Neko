package eu.kanade.tachiyomi.data.database.models

import android.content.Context
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.external.Amazon
import eu.kanade.tachiyomi.data.external.AniList
import eu.kanade.tachiyomi.data.external.AnimePlanet
import eu.kanade.tachiyomi.data.external.BookWalker
import eu.kanade.tachiyomi.data.external.CdJapan
import eu.kanade.tachiyomi.data.external.Dex
import eu.kanade.tachiyomi.data.external.DexApi
import eu.kanade.tachiyomi.data.external.DexComments
import eu.kanade.tachiyomi.data.external.EBookJapan
import eu.kanade.tachiyomi.data.external.Engtl
import eu.kanade.tachiyomi.data.external.ExternalLink
import eu.kanade.tachiyomi.data.external.Kitsu
import eu.kanade.tachiyomi.data.external.Mal
import eu.kanade.tachiyomi.data.external.MangaUpdatesLink
import eu.kanade.tachiyomi.data.external.Raw
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.utils.MdUtil
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.util.system.toMangaCacheKey
import java.util.Locale
import org.nekomanga.R
import org.nekomanga.domain.details.MangaDetailsPreferences
import tachiyomi.source.model.MangaInfo

interface Manga : SManga {

    var id: Long?

    var source: Long

    var favorite: Boolean

    var last_update: Long

    var next_update: Long

    var date_added: Long

    var viewer_flags: Int

    var chapter_flags: Int

    var filtered_scanlators: String?

    var filtered_language: String?

    var user_cover: String?

    var user_title: String?

    fun isBlank() = id == Long.MIN_VALUE

    fun isHidden() = status == -1

    fun setChapterOrder(sorting: Int, order: Int) {
        setChapterFlags(sorting, CHAPTER_SORTING_MASK)
        setChapterFlags(order, CHAPTER_SORT_MASK)
        setChapterFlags(CHAPTER_SORT_LOCAL, CHAPTER_SORT_LOCAL_MASK)
    }

    fun setSortToGlobal() = setChapterFlags(CHAPTER_SORT_FILTER_GLOBAL, CHAPTER_SORT_LOCAL_MASK)

    fun setFilterToGlobal() = setChapterFlags(CHAPTER_SORT_FILTER_GLOBAL, CHAPTER_FILTER_LOCAL_MASK)

    fun setFilterToLocal() = setChapterFlags(CHAPTER_FILTER_LOCAL, CHAPTER_FILTER_LOCAL_MASK)

    private fun setChapterFlags(flag: Int, mask: Int) {
        chapter_flags = chapter_flags and mask.inv() or (flag and mask)
    }

    private fun setViewerFlags(flag: Int, mask: Int) {
        viewer_flags = viewer_flags and mask.inv() or (flag and mask)
    }

    val sortDescending: Boolean
        get() = chapter_flags and CHAPTER_SORT_MASK == CHAPTER_SORT_DESC

    val hideChapterTitles: Boolean
        get() = displayMode == CHAPTER_DISPLAY_NUMBER

    val usesLocalSort: Boolean
        get() = chapter_flags and CHAPTER_SORT_LOCAL_MASK == CHAPTER_SORT_LOCAL

    val usesLocalFilter: Boolean
        get() = chapter_flags and CHAPTER_FILTER_LOCAL_MASK == CHAPTER_FILTER_LOCAL

    fun sortDescending(mangaDetailsPreferences: MangaDetailsPreferences): Boolean =
        if (usesLocalSort) sortDescending else mangaDetailsPreferences.chaptersDescAsDefault().get()

    fun chapterOrder(mangaDetailsPreferences: MangaDetailsPreferences): Int =
        if (usesLocalSort) sorting else mangaDetailsPreferences.sortChapterOrder().get()

    fun readFilter(mangaDetailsPreferences: MangaDetailsPreferences): Int =
        if (usesLocalFilter) readFilter else mangaDetailsPreferences.filterChapterByRead().get()

    fun downloadedFilter(mangaDetailsPreferences: MangaDetailsPreferences): Int =
        if (usesLocalFilter) downloadedFilter
        else mangaDetailsPreferences.filterChapterByDownloaded().get()

    fun bookmarkedFilter(mangaDetailsPreferences: MangaDetailsPreferences): Int =
        if (usesLocalFilter) bookmarkedFilter
        else mangaDetailsPreferences.filterChapterByBookmarked().get()

    fun hideChapterTitle(mangaDetailsPreferences: MangaDetailsPreferences): Boolean =
        if (usesLocalFilter) hideChapterTitles
        else mangaDetailsPreferences.hideChapterTitlesByDefault().get()

    fun showChapterTitle(defaultShow: Boolean): Boolean =
        chapter_flags and CHAPTER_DISPLAY_MASK == CHAPTER_DISPLAY_NUMBER

    fun seriesType(context: Context): String {
        return context
            .getString(
                when (seriesType()) {
                    TYPE_WEBTOON -> R.string.webtoon
                    TYPE_MANHWA -> R.string.manhwa
                    TYPE_MANHUA -> R.string.manhua
                    TYPE_COMIC -> R.string.comic
                    else -> R.string.manga
                },
            )
            .lowercase(Locale.getDefault())
    }

    fun getGenres(filterOutSafe: Boolean = false): List<String>? {
        return genre
            ?.split(",")
            ?.mapNotNull { tag -> tag.trim().takeUnless { it.isBlank() } }
            ?.filter {
                if (filterOutSafe) {
                    !it.equals("Content rating: safe", true)
                } else {
                    true
                }
            }
    }

    fun getContentRating(): String? {
        return getGenres()
            ?.firstOrNull { it.startsWith("Content rating: ") }
            ?.substringAfter("Content rating: ")
    }

    /** The type of comic the manga is (ie. manga, manhwa, manhua) */
    fun seriesType(): Int {
        // lump everything as manga if not manhua or manhwa
        return when (lang_flag) {
            "ko" -> TYPE_MANHWA
            "zh" -> TYPE_MANHUA
            "zh-hk" -> TYPE_MANHUA
            else -> TYPE_MANGA
        }
    }

    /**
     * The type the reader should use. Different from manga type as certain manga has different read
     * types
     */
    fun defaultReaderType(): Int {
        val currentTags = genre?.split(",")?.map { it.trim().lowercase(Locale.US) }
        return when {
            isLongStrip() -> {
                ReadingModeType.WEBTOON.flagValue
            }
            currentTags?.any { tag ->
                tag == "long strip" || tag == "manhwa" || tag.contains("webtoon")
            } == true -> {
                ReadingModeType.WEBTOON.flagValue
            }
            currentTags?.any { tag ->
                tag == "chinese" || tag == "manhua" || tag.startsWith("english") || tag == "comic"
            } == true -> {
                ReadingModeType.LEFT_TO_RIGHT.flagValue
            }
            else -> 0
        }
    }

    fun isOneShotOrCompleted(db: DatabaseHelper): Boolean {
        val tags by lazy { genre?.split(",")?.map { it.trim().lowercase(Locale.US) } }
        val chapters by lazy { db.getChapters(this).executeAsBlocking() }
        val firstChapterName by lazy { chapters.firstOrNull()?.name?.lowercase() ?: "" }
        return status == SManga.COMPLETED ||
            tags?.contains("oneshot") == true ||
            (chapters.size == 1 &&
                (Regex("one.?shot").containsMatchIn(firstChapterName) ||
                    firstChapterName.contains("oneshot")))
    }

    fun key(): String {
        return id!!.toMangaCacheKey()
    }

    fun getAltTitles(): List<String> {
        return alt_titles?.split("|~|")?.filter { it.isNotBlank() }?.filter { it != originalTitle }
            ?: emptyList()
    }

    fun getExternalLinks(): List<ExternalLink> {
        val list = mutableListOf<ExternalLink>()
        list.add(Dex(MdUtil.getMangaUUID(url)))
        list.add(DexApi(MdUtil.getMangaUUID(url)))

        thread_id?.let { list.add(DexComments(it)) }

        kitsu_id?.let { list.add(Kitsu(it)) }

        anilist_id?.let { list.add(AniList(it)) }
        anime_planet_id?.let { list.add(AnimePlanet(it)) }
        manga_updates_id?.let { list.add(MangaUpdatesLink(it)) }

        my_anime_list_id?.let { list.add(Mal(it)) }

        other_urls?.let { combinedString ->
            combinedString.split("||").forEach { pairString ->
                val split = pairString.split("~~")
                when {
                    split[0] == "raw" -> list.add(Raw(split[1]))
                    split[0] == "engtl" -> list.add(Engtl(split[1]))
                    split[0] == "bw" -> list.add(BookWalker(split[1]))
                    split[0] == "cdj" -> list.add(CdJapan(split[1]))
                    split[0] == "ebj" -> list.add(EBookJapan(split[1]))
                    split[0] == "amz" -> list.add(Amazon(split[1]))
                }
            }
        }

        return list.toList().sortedBy { it.name }
    }

    // Used to display the chapter's title one way or another
    var displayMode: Int
        get() = chapter_flags and CHAPTER_DISPLAY_MASK
        set(mode) = setChapterFlags(mode, CHAPTER_DISPLAY_MASK)

    var readFilter: Int
        get() = chapter_flags and CHAPTER_READ_MASK
        set(filter) = setChapterFlags(filter, CHAPTER_READ_MASK)

    var downloadedFilter: Int
        get() = chapter_flags and CHAPTER_DOWNLOADED_MASK
        set(filter) = setChapterFlags(filter, CHAPTER_DOWNLOADED_MASK)

    var bookmarkedFilter: Int
        get() = chapter_flags and CHAPTER_BOOKMARKED_MASK
        set(filter) = setChapterFlags(filter, CHAPTER_BOOKMARKED_MASK)

    var sorting: Int
        get() = chapter_flags and CHAPTER_SORTING_MASK
        set(sort) = setChapterFlags(sort, CHAPTER_SORTING_MASK)

    var readingModeType: Int
        get() = viewer_flags and ReadingModeType.MASK
        set(readingMode) = setViewerFlags(readingMode, ReadingModeType.MASK)

    var orientationType: Int
        get() = viewer_flags and OrientationType.MASK
        set(rotationType) = setViewerFlags(rotationType, OrientationType.MASK)

    companion object {

        // Generic filter that does not filter anything
        const val SHOW_ALL = 0x00000000

        const val CHAPTER_SORT_DESC = 0x00000000
        const val CHAPTER_SORT_ASC = 0x00000001
        const val CHAPTER_SORT_MASK = 0x00000001

        const val CHAPTER_SORT_FILTER_GLOBAL = 0x00000000
        const val CHAPTER_SORT_LOCAL = 0x00001000
        const val CHAPTER_SORT_LOCAL_MASK = 0x00001000
        const val CHAPTER_FILTER_LOCAL = 0x00002000
        const val CHAPTER_FILTER_LOCAL_MASK = 0x00002000

        const val CHAPTER_SHOW_UNREAD = 0x00000002
        const val CHAPTER_SHOW_READ = 0x00000004
        const val CHAPTER_READ_MASK = 0x00000006

        const val CHAPTER_SHOW_DOWNLOADED = 0x00000008
        const val CHAPTER_SHOW_NOT_DOWNLOADED = 0x00000010
        const val CHAPTER_DOWNLOADED_MASK = 0x00000018

        const val CHAPTER_SHOW_BOOKMARKED = 0x00000020
        const val CHAPTER_SHOW_NOT_BOOKMARKED = 0x00000040
        const val CHAPTER_BOOKMARKED_MASK = 0x00000060

        const val CHAPTER_SORTING_SOURCE = 0x00000000
        const val CHAPTER_SORTING_NUMBER = 0x00000100
        const val CHAPTER_SORTING_UPLOAD_DATE = 0x00000200
        const val CHAPTER_SORTING_MASK = 0x00000300

        const val CHAPTER_DISPLAY_NAME = 0x00000000
        const val CHAPTER_DISPLAY_NUMBER = 0x00100000
        const val CHAPTER_DISPLAY_MASK = 0x00100000

        const val TYPE_MANGA = 1
        const val TYPE_MANHWA = 2
        const val TYPE_MANHUA = 3
        const val TYPE_COMIC = 4
        const val TYPE_WEBTOON = 5

        fun create(source: Long): Manga = MangaImpl().apply { this.source = source }

        fun create(pathUrl: String, title: String, source: Long = 0): Manga =
            MangaImpl().apply {
                url = pathUrl
                this.title = title
                this.source = source
            }
    }
}

fun Manga.isLongStrip() = this.genre?.contains("long strip", true) ?: false

fun Manga.toMangaInfo(): MangaInfo {
    return MangaInfo(
        artist = this.artist ?: "",
        author = this.author ?: "",
        cover = this.thumbnail_url ?: "",
        description = this.description ?: "",
        genres = this.getGenres() ?: emptyList(),
        key = this.url,
        status = this.status,
        title = this.title,
    )
}

fun Manga.uuid(): String {
    return MdUtil.getMangaUUID(this.url)
}
