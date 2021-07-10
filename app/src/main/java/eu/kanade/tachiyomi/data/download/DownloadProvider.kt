package eu.kanade.tachiyomi.data.download

import android.content.Context
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import com.elvishew.xlog.XLog
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.getOrDefault
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.isMergedChapter
import eu.kanade.tachiyomi.util.lang.isUUID
import eu.kanade.tachiyomi.util.storage.DiskUtil
import uy.kohesive.injekt.injectLazy

/**
 * This class is used to provide the directories where the downloads should be saved.
 * It uses the following path scheme: /<root downloads dir>/<source name>/<manga>/<chapter>
 *
 * @param context the application context.
 */
class DownloadProvider(private val context: Context) {

    /**
     * Preferences helper.
     */
    private val preferences: PreferencesHelper by injectLazy()

    /**
     * The root directory for downloads.
     */
    private var downloadsDir = preferences.downloadsDirectory().getOrDefault().let {
        val dir = UniFile.fromUri(context, it.toUri())
        DiskUtil.createNoMediaFile(dir, context)
        dir
    }

    init {
        preferences.downloadsDirectory().asObservable().skip(1)
            .subscribe { downloadsDir = UniFile.fromUri(context, it.toUri()) }
    }

    /**
     * Returns the download directory for a manga. For internal use only.
     *
     * @param manga the manga to query.
     * @param source the source of the manga.
     */
    internal fun getMangaDir(manga: Manga, source: Source): UniFile {
        try {
            return downloadsDir.createDirectory(getSourceDirName(source))
                .createDirectory(getMangaDirName(manga))
        } catch (e: Exception) {
            XLog.e(e)
            throw Exception(context.getString(R.string.invalid_download_location))
        }
    }

    /**
     * Returns the download directory for a source if it exists.
     *
     * @param source the source to query.
     */
    fun findSourceDir(source: Source): UniFile? {
        return downloadsDir.findFile(getSourceDirName(source), true)
    }

    /**
     * Returns the download directory for a manga if it exists.
     *
     * @param manga the manga to query.
     * @param source the source of the manga.
     */
    fun findMangaDir(manga: Manga, source: Source): UniFile? {
        val sourceDir = findSourceDir(source)
        return sourceDir?.findFile(getMangaDirName(manga), true)
    }

    /**
     * Returns the download directory for a chapter if it exists.
     *
     * @param chapter the chapter to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findChapterDir(chapter: Chapter, manga: Manga, source: Source): UniFile? {
        val mangaDir = findMangaDir(manga, source)
        return if (chapter.isMergedChapter()) {
            mangaDir?.findFile(getJ2kChapterName(chapter))
        } else {
            var dir =
                getValidChapterDirNames(chapter).mapNotNull { mangaDir?.findFile(it) }.firstOrNull()
            if (dir == null) {
                dir = mangaDir?.listFiles()
                    ?.find { it.name != null && it.name!!.endsWith(chapter.mangadex_chapter_id) }
            }
            dir
        }
    }

    /**
     * Returns a list of downloaded directories for the chapters that exist.
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findChapterDirs(chapters: List<Chapter>, manga: Manga, source: Source): List<UniFile> {
        val mangaDir = findMangaDir(manga, source) ?: return emptyList()

        val idHashSet = chapters.map { it.mangadex_chapter_id }.toHashSet()
        val oldIdHashSet = chapters.mapNotNull { it.old_mangadex_id }.toHashSet()
        val chapterNameHashSet = chapters.map { it.name }.toHashSet()
        val scanalatorNameHashSet = chapters.map { getJ2kChapterName(it) }.toHashSet()

        return mangaDir.listFiles()!!.asList().filter { file ->
            file.name?.let { fileName ->
                val mangadexId = fileName.substringAfterLast(" - ", "")
                // legacy dex id
                if (mangadexId.isNotEmpty() && mangadexId.isUUID()) {
                    return@filter idHashSet.contains(mangadexId)
                } else if (mangadexId.isNotEmpty() && mangadexId.isDigitsOnly()) {
                    return@filter oldIdHashSet.contains(mangadexId)
                } else {
                    if (scanalatorNameHashSet.contains(fileName)) {
                        return@filter true
                    }
                    val afterScanlatorCheck = fileName.substringAfter("_")
                    return@filter chapterNameHashSet.contains(fileName) || chapterNameHashSet.contains(
                        afterScanlatorCheck
                    )
                }
            }
            return@filter false
        }
    }

    /**
     * Returns a list of all files in manga directory
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findUnmatchedChapterDirs(
        chapters: List<Chapter>,
        manga: Manga,
        source: Source,
    ): List<UniFile> {
        val mangaDir = findMangaDir(manga, source) ?: return emptyList()
        val idHashSet = chapters.map { it.mangadex_chapter_id }.toHashSet()
        val chapterNameHashSet = chapters.map { it.name }.toHashSet()
        val scanalatorNameHashSet = chapters.map { getJ2kChapterName(it) }.toHashSet()

        return mangaDir.listFiles()!!.asList().filter { file ->
            file.name?.let { fileName ->
                if (fileName.endsWith(Downloader.TMP_DIR_SUFFIX)) {
                    return@filter true
                }
                val mangadexId = fileName.substringAfterLast("- ", "")
                if (mangadexId.isNotEmpty() && (mangadexId.isDigitsOnly() || mangadexId.isUUID())) {
                    return@filter !idHashSet.contains(mangadexId)
                } else {
                    if (scanalatorNameHashSet.contains(fileName)) {
                        return@filter false
                    }
                    val afterScanlatorCheck = fileName.substringAfter("_")
                    return@filter !chapterNameHashSet.contains(fileName) && !chapterNameHashSet.contains(
                        afterScanlatorCheck
                    )
                }
            }
            // everything else is considered true
            return@filter true
        }
    }

    fun renameMangaFolder(from: String, to: String, sourceId: Long) {
        val sourceManager by injectLazy<SourceManager>()
        val source = sourceManager.get(sourceId) ?: return
        val sourceDir = findSourceDir(source)
        val mangaDir = sourceDir?.findFile(DiskUtil.buildValidFilename(from))
        mangaDir?.renameTo(to)
    }

    /**
     * Returns a list of downloaded directories for the chapters that exist.
     *
     * @param chapters the chapters to query.
     * @param manga the manga of the chapter.
     * @param source the source of the chapter.
     */
    fun findTempChapterDirs(chapters: List<Chapter>, manga: Manga, source: Source): List<UniFile> {
        val mangaDir = findMangaDir(manga, source) ?: return emptyList()
        return chapters.mapNotNull { mangaDir.findFile("${getChapterDirName(it)}${Downloader.TMP_DIR_SUFFIX}") }
    }

    /**
     * Returns the download directory name for a source always english to not break with other forks or current neko
     *
     * @param source the source to query.
     */
    fun getSourceDirName(source: Source): String {
        return "$source (EN)"
    }

    /**
     * Returns the download directory name for a manga.
     *
     * @param manga the manga to query.
     */
    fun getMangaDirName(manga: Manga): String {
        return DiskUtil.buildValidFilename(manga.title)
    }

    /**
     * Returns the chapter directory name for a chapter.
     *
     * @param chapter the chapter to query.
     */
    fun getChapterDirName(chapter: Chapter, useNewId: Boolean = true): String {
        if (chapter.isMergedChapter()) {
            return getJ2kChapterName(chapter)
        } else {
            if (useNewId.not() && chapter.old_mangadex_id == null) {
                return ""
            }
            val chapterId = if (useNewId) chapter.mangadex_chapter_id else chapter.old_mangadex_id
            return DiskUtil.buildValidFilename(chapter.name, " - $chapterId")
        }
    }

    fun getJ2kChapterName(chapter: Chapter): String {
        return DiskUtil.buildValidFilename(
            if (chapter.scanlator != null) "${chapter.scanlator}_${chapter.name}"
            else chapter.name
        )
    }

    /**
     * Returns valid downloaded chapter directory names.
     *
     * @param chapter the chapter to query.
     */
    fun getValidChapterDirNames(chapter: Chapter): List<String> {
        return listOf(
            getChapterDirName(chapter, true),
            // chater names from j2k
            getJ2kChapterName(chapter),
            // legacy manga id
            getChapterDirName(chapter, false),
            // Legacy chapter directory name used in v0.8.4 and before
            DiskUtil.buildValidFilename(chapter.name)
        ).filter { it.isNotEmpty() }
    }
}
